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
 *
 */

package com.tencent.cloud.polaris.router.resttemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.metadata.config.MetadataLocalProperties;
import com.tencent.cloud.common.util.ExpressionLabelUtils;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.polaris.router.RouterConstants;
import com.tencent.cloud.polaris.router.RouterRuleLabelResolver;
import com.tencent.cloud.polaris.router.spi.RouterLabelResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;

/**
 * PolarisLoadBalancerInterceptor extends LoadBalancerInterceptor capabilities.
 * Parses the label from the request and puts it into the RouterContext for routing.
 *
 * @author lepdou, cheese8
 */
public class PolarisLoadBalancerInterceptor extends LoadBalancerInterceptor {
	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisLoadBalancerInterceptor.class);

	private final LoadBalancerClient loadBalancer;
	private final LoadBalancerRequestFactory requestFactory;
	private final List<RouterLabelResolver> routerLabelResolvers;
	private final MetadataLocalProperties metadataLocalProperties;
	private final RouterRuleLabelResolver routerRuleLabelResolver;

	public PolarisLoadBalancerInterceptor(LoadBalancerClient loadBalancer,
			LoadBalancerRequestFactory requestFactory,
			List<RouterLabelResolver> routerLabelResolvers,
			MetadataLocalProperties metadataLocalProperties,
			RouterRuleLabelResolver routerRuleLabelResolver) {
		super(loadBalancer, requestFactory);
		this.loadBalancer = loadBalancer;
		this.requestFactory = requestFactory;
		this.metadataLocalProperties = metadataLocalProperties;
		this.routerRuleLabelResolver = routerRuleLabelResolver;

		if (!CollectionUtils.isEmpty(routerLabelResolvers)) {
			routerLabelResolvers.sort(Comparator.comparingInt(Ordered::getOrder));
			this.routerLabelResolvers = routerLabelResolvers;
		}
		else {
			this.routerLabelResolvers = null;
		}
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		final URI originalUri = request.getURI();
		String peerServiceName = originalUri.getHost();
		Assert.state(peerServiceName != null,
				"Request URI does not contain a valid hostname: " + originalUri);

		setLabelsToHeaders(request, body, peerServiceName);

		return this.loadBalancer.execute(peerServiceName,
				new PolarisLoadBalancerRequest<>(request, this.requestFactory.createRequest(request, body, execution)));
	}

	void setLabelsToHeaders(HttpRequest request, byte[] body, String peerServiceName) {
		// local service labels
		Map<String, String> labels = new HashMap<>(metadataLocalProperties.getContent());

		// labels from rule expression
		Map<String, String> ruleExpressionLabels = getExpressionLabels(request, peerServiceName);
		if (!CollectionUtils.isEmpty(ruleExpressionLabels)) {
			labels.putAll(ruleExpressionLabels);
		}

		// labels from request
		if (!CollectionUtils.isEmpty(routerLabelResolvers)) {
			routerLabelResolvers.forEach(resolver -> {
				try {
					Map<String, String> customResolvedLabels = resolver.resolve(request, body);
					if (!CollectionUtils.isEmpty(customResolvedLabels)) {
						labels.putAll(customResolvedLabels);
					}
				}
				catch (Throwable t) {
					LOGGER.error("[SCT][Router] revoke RouterLabelResolver occur some exception. ", t);
				}
			});
		}

		// labels from downstream
		Map<String, String> transitiveLabels = MetadataContextHolder.get()
				.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
		labels.putAll(transitiveLabels);

		// pass label by header
		if (labels.size() == 0) {
			request.getHeaders().set(RouterConstants.ROUTER_LABEL_HEADER, null);
			return;
		}
		String encodedLabelsContent;
		try {
			encodedLabelsContent = URLEncoder.encode(JacksonUtils.serialize2Json(labels), UTF_8);
		}
		catch (UnsupportedEncodingException e) {
			throw new RuntimeException("unsupported charset exception " + UTF_8);
		}
		request.getHeaders().set(RouterConstants.ROUTER_LABEL_HEADER, encodedLabelsContent);
	}

	private Map<String, String> getExpressionLabels(HttpRequest request, String peerServiceName) {
		Set<String> labelKeys = routerRuleLabelResolver.getExpressionLabelKeys(MetadataContext.LOCAL_NAMESPACE,
				MetadataContext.LOCAL_SERVICE, peerServiceName);

		if (CollectionUtils.isEmpty(labelKeys)) {
			return Collections.emptyMap();
		}

		return ExpressionLabelUtils.resolve(request, labelKeys);
	}
}
