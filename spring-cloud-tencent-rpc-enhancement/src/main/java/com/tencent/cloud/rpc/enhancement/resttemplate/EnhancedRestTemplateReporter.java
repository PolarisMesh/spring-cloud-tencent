/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.cloud.rpc.enhancement.resttemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

import com.tencent.cloud.common.constant.RouterConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.rpc.enhancement.AbstractPolarisReporterAdapter;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.api.utils.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;

/**
 * Extend ResponseErrorHandler to get request information.
 *
 * @author wh 2022/6/21
 */
public class EnhancedRestTemplateReporter extends AbstractPolarisReporterAdapter implements ResponseErrorHandler, ApplicationContextAware {

	/**
	 * Polaris-CircuitBreaker-Fallback header flag.
	 */
	public static final String POLARIS_CIRCUIT_BREAKER_FALLBACK_HEADER = "X-SCT-Polaris-CircuitBreaker-Fallback";
	/**
	 * response has error header flag, since EnhancedRestTemplateReporter#hasError always return true.
	 */
	public static final String HEADER_HAS_ERROR = "X-SCT-Has-Error";
	private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedRestTemplateReporter.class);
	private final ConsumerAPI consumerAPI;
	private ResponseErrorHandler delegateHandler;

	public EnhancedRestTemplateReporter(RpcEnhancementReporterProperties properties, ConsumerAPI consumerAPI) {
		super(properties);
		this.consumerAPI = consumerAPI;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		String[] handlerBeanNames = applicationContext.getBeanNamesForType(ResponseErrorHandler.class);
		if (handlerBeanNames.length == 1) {
			if (this.delegateHandler == null) {
				this.delegateHandler = new DefaultResponseErrorHandler();
			}
			return;
		}

		// inject user custom ResponseErrorHandler
		for (String beanName : handlerBeanNames) {
			// ignore self
			if (StringUtils.equalsIgnoreCase("enhancedRestTemplateReporter", beanName)) {
				continue;
			}
			this.delegateHandler = (ResponseErrorHandler) applicationContext.getBean(beanName);
		}
	}

	@Override
	public boolean hasError(@NonNull ClientHttpResponse response) throws IOException {
		if (delegateHandler != null) {
			// Preserve the delegated handler result
			boolean hasError = delegateHandler.hasError(response);
			response.getHeaders().add(HEADER_HAS_ERROR, String.valueOf(hasError));
		}
		return true;
	}

	@Override
	public void handleError(@NonNull ClientHttpResponse response) throws IOException {
		if (realHasError(response)) {
			delegateHandler.handleError(response);
		}

		clear(response);
	}

	@Override
	public void handleError(@NonNull URI url, @NonNull HttpMethod method, @NonNull ClientHttpResponse response) throws IOException {
		// report result to polaris
		if (reportProperties.isEnabled()) {
			reportResult(url, response);
		}

		// invoke delegate handler
		invokeDelegateHandler(url, method, response);
	}

	private void reportResult(URI url, ClientHttpResponse response) {
		if (Boolean.parseBoolean(response.getHeaders().getFirst(POLARIS_CIRCUIT_BREAKER_FALLBACK_HEADER))) {
			return;
		}
		try {
			ServiceCallResult resultRequest = createServiceCallResult(url, response);
			Map<String, String> loadBalancerContext = MetadataContextHolder.get().getLoadbalancerMetadata();

			String targetHost = loadBalancerContext.get("host");
			String targetPort = loadBalancerContext.get("port");
			String startMillis = loadBalancerContext.get("startMillis");
			long delay = System.currentTimeMillis() - Long.parseLong(startMillis);

			if (StringUtils.isBlank(targetHost) || StringUtils.isBlank(targetPort)) {
				LOGGER.warn("Can not get target host or port from metadata context. host = {}, port = {}", targetHost, targetPort);
				return;
			}

			resultRequest.setHost(targetHost);
			resultRequest.setPort(Integer.parseInt(targetPort));
			resultRequest.setDelay(delay);

			// checking response http status code
			if (apply(response.getStatusCode())) {
				resultRequest.setRetStatus(RetStatus.RetFail);
			}

			List<String> labels = response.getHeaders().get(RouterConstant.ROUTER_LABEL_HEADER);
			if (CollectionUtils.isNotEmpty(labels)) {
				String label = labels.get(0);
				try {
					label = URLDecoder.decode(label, UTF_8);
				}
				catch (UnsupportedEncodingException e) {
					LOGGER.error("unsupported charset exception " + UTF_8, e);
				}
				resultRequest.setLabels(convertLabel(label));
			}

			// processing report with consumerAPI .
			LOGGER.debug("Will report result of {}. Request=[{}]. Response=[{}]. Delay=[{}]ms.", resultRequest.getRetStatus()
					.name(), url, response.getStatusCode().value(), delay);
			consumerAPI.updateServiceCallResult(resultRequest);
		}
		catch (Exception e) {
			LOGGER.error("RestTemplate response reporter execute failed of {} url {}", response, url, e);
		}
	}

	private String convertLabel(String label) {
		label = label.replaceAll("\"|\\{|\\}", "")
				.replaceAll(",", "|");
		return label;
	}

	private void invokeDelegateHandler(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
		if (realHasError(response)) {
			delegateHandler.handleError(url, method, response);
		}

		clear(response);
	}

	private Boolean realHasError(ClientHttpResponse response) {
		if (delegateHandler == null) {
			return false;
		}

		String hasErrorHeader = response.getHeaders().getFirst(HEADER_HAS_ERROR);
		if (StringUtils.isBlank(hasErrorHeader)) {
			return false;
		}

		return Boolean.parseBoolean(hasErrorHeader);
	}

	private void clear(ClientHttpResponse response) {
		response.getHeaders().remove(HEADER_HAS_ERROR);
		response.getHeaders().remove(POLARIS_CIRCUIT_BREAKER_FALLBACK_HEADER);
	}

	private ServiceCallResult createServiceCallResult(URI uri, ClientHttpResponse response) throws IOException {
		ServiceCallResult resultRequest = new ServiceCallResult();
		String serviceName = uri.getHost();
		resultRequest.setService(serviceName);
		resultRequest.setNamespace(MetadataContext.LOCAL_NAMESPACE);
		resultRequest.setMethod(uri.getPath());
		resultRequest.setRetCode(response.getStatusCode().value());
		resultRequest.setRetStatus(RetStatus.RetSuccess);
		String sourceNamespace = MetadataContext.LOCAL_NAMESPACE;
		String sourceService = MetadataContext.LOCAL_SERVICE;
		if (StringUtils.isNotBlank(sourceNamespace) && StringUtils.isNotBlank(sourceService)) {
			resultRequest.setCallerService(new ServiceKey(sourceNamespace, sourceService));
		}
		return resultRequest;
	}

	protected ResponseErrorHandler getDelegateHandler() {
		return this.delegateHandler;
	}

	protected void setDelegateHandler(ResponseErrorHandler delegateHandler) {
		this.delegateHandler = delegateHandler;
	}
}
