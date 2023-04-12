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

package com.tencent.cloud.rpc.enhancement.plugin.reporter;

import java.util.Optional;

import com.tencent.cloud.rpc.enhancement.AbstractPolarisReporterAdapter;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPlugin;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedRequestContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedResponseContext;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.client.api.SDKContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.Ordered;

/**
 * Polaris reporter when feign call is successful.
 *
 * @author Haotian Zhang
 */
public class SuccessPolarisReporter extends AbstractPolarisReporterAdapter implements EnhancedPlugin {

	private static final Logger LOG = LoggerFactory.getLogger(SuccessPolarisReporter.class);

	private final ConsumerAPI consumerAPI;

	public SuccessPolarisReporter(RpcEnhancementReporterProperties properties,
			SDKContext context,
			ConsumerAPI consumerAPI) {
		super(properties, context);
		this.consumerAPI = consumerAPI;
	}

	@Override
	public String getName() {
		return SuccessPolarisReporter.class.getName();
	}

	@Override
	public EnhancedPluginType getType() {
		return EnhancedPluginType.POST;
	}

	@Override
	public void run(EnhancedPluginContext context) {
		if (!super.reportProperties.isEnabled()) {
			return;
		}

		EnhancedRequestContext request = context.getRequest();
		EnhancedResponseContext response = context.getResponse();
		ServiceInstance serviceInstance = Optional.ofNullable(context.getServiceInstance()).orElse(new DefaultServiceInstance());

		ServiceCallResult resultRequest = createServiceCallResult(
				serviceInstance.getServiceId(),
				serviceInstance.getHost(),
				serviceInstance.getPort(),
				request.getUrl(),
				request.getHttpHeaders(),
				response.getHttpHeaders(),
				response.getHttpStatus(),
				context.getDelay(),
				null
		);

		LOG.debug("Will report ServiceCallResult of {}. Request=[{} {}]. Response=[{}]. Delay=[{}]ms.",
				resultRequest.getRetStatus().name(), request.getHttpMethod().name(), request.getUrl().getPath(), response.getHttpStatus(), context.getDelay());

		consumerAPI.updateServiceCallResult(resultRequest);

	}

	@Override
	public void handlerThrowable(EnhancedPluginContext context, Throwable throwable) {
		LOG.error("SuccessPolarisReporter runs failed. context=[{}].",
				context, throwable);
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 1;
	}
}
