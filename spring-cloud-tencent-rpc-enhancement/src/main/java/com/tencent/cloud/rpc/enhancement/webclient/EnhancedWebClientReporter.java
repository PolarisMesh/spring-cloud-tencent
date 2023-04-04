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

package com.tencent.cloud.rpc.enhancement.webclient;

import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Objects;

import com.tencent.cloud.common.constant.HeaderConstant;
import com.tencent.cloud.common.constant.RouterConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.util.RequestLabelUtils;
import com.tencent.cloud.rpc.enhancement.AbstractPolarisReporterAdapter;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.client.api.SDKContext;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;

public class EnhancedWebClientReporter extends AbstractPolarisReporterAdapter implements ExchangeFilterFunction {

	protected static final String METRICS_WEBCLIENT_START_TIME = EnhancedWebClientReporter.class.getName()
			+ ".START_TIME";
	private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedWebClientReporter.class);
	private final ConsumerAPI consumerAPI;

	private final SDKContext context;

	public EnhancedWebClientReporter(RpcEnhancementReporterProperties reportProperties, SDKContext context, ConsumerAPI consumerAPI) {
		super(reportProperties);
		this.context = context;
		this.consumerAPI = consumerAPI;
	}

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		return next.exchange(request).as((responseMono) -> instrumentResponse(request, responseMono))
				.contextWrite(this::putStartTime);
	}

	Mono<ClientResponse> instrumentResponse(ClientRequest request, Mono<ClientResponse> responseMono) {
		return Mono.deferContextual((ctx) -> responseMono.doOnEach((signal) -> {
			// report result to polaris
			if (!reportProperties.isEnabled()) {
				return;
			}
			ServiceCallResult callResult = new ServiceCallResult();
			Long startTime = getStartTime(ctx);
			callResult.setDelay(System.currentTimeMillis() - startTime);

			callResult.setNamespace(MetadataContext.LOCAL_NAMESPACE);
			callResult.setService(request.headers().getFirst(HeaderConstant.INTERNAL_CALLEE_SERVICE_ID));
			String sourceNamespace = MetadataContext.LOCAL_NAMESPACE;
			String sourceService = MetadataContext.LOCAL_SERVICE;
			if (StringUtils.isNotBlank(sourceNamespace) && StringUtils.isNotBlank(sourceService)) {
				callResult.setCallerService(new ServiceKey(sourceNamespace, sourceService));
			}

			Collection<String> labels = request.headers().get(RouterConstant.ROUTER_LABEL_HEADER);
			if (CollectionUtils.isNotEmpty(labels) && labels.iterator().hasNext()) {
				String label = labels.iterator().next();
				try {
					label = URLDecoder.decode(label, UTF_8);
				}
				catch (UnsupportedEncodingException e) {
					LOGGER.error("unsupported charset exception " + UTF_8, e);
				}
				callResult.setLabels(RequestLabelUtils.convertLabel(label));
			}

			URI uri = request.url();
			callResult.setMethod(uri.getPath());
			callResult.setHost(uri.getHost());
			// -1 means access directly by url, and use http default port number 80
			callResult.setPort(uri.getPort() == -1 ? 80 : uri.getPort());
			if (Objects.nonNull(context)) {
				callResult.setCallerIp(context.getConfig().getGlobal().getAPI().getBindIP());
			}

			RetStatus retStatus = RetStatus.RetSuccess;
			ClientResponse response = signal.get();
			if (Objects.nonNull(response)) {
				HttpHeaders headers = response.headers().asHttpHeaders();

				callResult.setRuleName(getActiveRuleNameFromRequest(headers));
				if (apply(response.statusCode())) {
					retStatus = RetStatus.RetFail;
				}
				retStatus = getRetStatusFromRequest(headers, retStatus);
			}
			if (signal.isOnError()) {
				Throwable throwable = signal.getThrowable();
				if (throwable instanceof SocketTimeoutException) {
					retStatus = RetStatus.RetTimeout;
				}
			}
			callResult.setRetStatus(retStatus);

			consumerAPI.updateServiceCallResult(callResult);
		}));
	}

	private Long getStartTime(ContextView context) {
		return context.get(METRICS_WEBCLIENT_START_TIME);
	}

	private Context putStartTime(Context context) {
		return context.put(METRICS_WEBCLIENT_START_TIME, System.currentTimeMillis());
	}
}
