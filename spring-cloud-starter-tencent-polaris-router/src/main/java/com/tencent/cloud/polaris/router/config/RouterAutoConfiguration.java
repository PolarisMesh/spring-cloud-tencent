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

package com.tencent.cloud.polaris.router.config;

import com.tencent.cloud.polaris.context.ServiceRuleManager;
import com.tencent.cloud.polaris.router.RouterRuleLabelResolver;
import com.tencent.cloud.polaris.router.resttemplate.PolarisLoadBalancerBeanPostProcessor;
import com.tencent.cloud.polaris.router.scg.PolarisLoadBalancerClientBeanPostProcessor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

/**
 * router module auto configuration.
 *
 * @author lepdou 2022-05-11
 */
@Configuration
@LoadBalancerClients(defaultConfiguration = LoadBalancerConfiguration.class)
@Import({PolarisNearByRouterProperties.class, PolarisMetadataRouterProperties.class, PolarisRuleBasedRouterProperties.class})
public class RouterAutoConfiguration {

	@Bean
	@Order(HIGHEST_PRECEDENCE)
	@ConditionalOnClass(name = "org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor")
	public PolarisLoadBalancerBeanPostProcessor polarisLoadBalancerBeanPostProcessor() {
		return new PolarisLoadBalancerBeanPostProcessor();
	}

	@Bean
	@Order(HIGHEST_PRECEDENCE)
	@ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter")
	public PolarisLoadBalancerClientBeanPostProcessor polarisLoadBalancerClientBeanPostProcessor() {
		return new PolarisLoadBalancerClientBeanPostProcessor();
	}

	@Bean
	public RouterRuleLabelResolver routerRuleLabelResolver(ServiceRuleManager serviceRuleManager) {
		return new RouterRuleLabelResolver(serviceRuleManager);
	}
}
