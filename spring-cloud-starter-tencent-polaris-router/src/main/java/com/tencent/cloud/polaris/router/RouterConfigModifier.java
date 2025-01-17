/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.polaris.router;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.cloud.polaris.router.config.properties.PolarisNearByRouterProperties;
import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.plugins.router.healthy.RecoverRouterConfig;
import com.tencent.polaris.plugins.router.nearby.NearbyRouterConfig;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import org.apache.commons.lang.StringUtils;

/**
 * RouterConfigModifier.
 *
 * @author sean yu
 */
public class RouterConfigModifier implements PolarisConfigModifier {

	private final PolarisNearByRouterProperties polarisNearByRouterProperties;

	public RouterConfigModifier(PolarisNearByRouterProperties polarisNearByRouterProperties) {
		this.polarisNearByRouterProperties = polarisNearByRouterProperties;
	}

	@Override
	public void modify(ConfigurationImpl configuration) {
		// Set excludeCircuitBreakInstances to false
		RecoverRouterConfig recoverRouterConfig = configuration.getConsumer().getServiceRouter()
				.getPluginConfig(ServiceRouterConfig.DEFAULT_ROUTER_RECOVER, RecoverRouterConfig.class);
		recoverRouterConfig.setExcludeCircuitBreakInstances(false);

		// Update modified config to source properties
		configuration.getConsumer().getServiceRouter()
				.setPluginConfig(ServiceRouterConfig.DEFAULT_ROUTER_RECOVER, recoverRouterConfig);

		if (StringUtils.isNotBlank(polarisNearByRouterProperties.getMatchLevel())) {
			RoutingProto.NearbyRoutingConfig.LocationLevel locationLevel =
					RoutingProto.NearbyRoutingConfig.LocationLevel.valueOf(StringUtils.upperCase(polarisNearByRouterProperties.getMatchLevel()));
			NearbyRouterConfig nearbyRouterConfig = configuration.getConsumer().getServiceRouter().getPluginConfig(
					ServiceRouterConfig.DEFAULT_ROUTER_NEARBY, NearbyRouterConfig.class);
			nearbyRouterConfig.setMatchLevel(locationLevel);
			configuration.getConsumer().getServiceRouter()
					.setPluginConfig(ServiceRouterConfig.DEFAULT_ROUTER_NEARBY, nearbyRouterConfig);
		}

	}

	@Override
	public int getOrder() {
		return OrderConstant.Modifier.ROUTER_ORDER;
	}
}
