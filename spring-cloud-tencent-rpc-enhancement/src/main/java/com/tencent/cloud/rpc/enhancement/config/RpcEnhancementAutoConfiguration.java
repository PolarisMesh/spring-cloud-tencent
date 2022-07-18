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

package com.tencent.cloud.rpc.enhancement.config;

import java.util.List;

import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.cloud.rpc.enhancement.feign.EnhancedFeignBeanPostProcessor;
import com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignPlugin;
import com.tencent.cloud.rpc.enhancement.feign.plugin.reporter.ExceptionPolarisReporter;
import com.tencent.cloud.rpc.enhancement.feign.plugin.reporter.SuccessPolarisReporter;
import com.tencent.cloud.rpc.enhancement.resttemplate.EnhancedRestTemplateModifier;
import com.tencent.cloud.rpc.enhancement.resttemplate.EnhancedRestTemplateReporter;
import com.tencent.cloud.rpc.enhancement.resttemplate.PolarisResponseErrorHandler;
import com.tencent.polaris.api.core.ConsumerAPI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.client.RestTemplate;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

/**
 * Auto Configuration for Polaris {@link feign.Feign} OR {@link RestTemplate} which can automatically bring in the call
 * results for reporting.
 *
 * @author <a href="mailto:iskp.me@gmail.com">Palmer.Xu</a> 2022-06-29
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "spring.cloud.tencent.rpc-enhancement.enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(PolarisContextAutoConfiguration.class)
public class RpcEnhancementAutoConfiguration {

	/**
	 * Configuration for Polaris {@link feign.Feign} which can automatically bring in the call
	 * results for reporting.
	 *
	 * @author Haotian Zhang
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "org.springframework.cloud.openfeign.FeignAutoConfiguration")
	@AutoConfigureBefore(name = "org.springframework.cloud.openfeign.FeignAutoConfiguration")
	protected static class PolarisFeignClientAutoConfiguration {

		@Bean
		@Order(HIGHEST_PRECEDENCE)
		public EnhancedFeignBeanPostProcessor polarisFeignBeanPostProcessor(
				@Autowired(required = false) List<EnhancedFeignPlugin> enhancedFeignPlugins) {
			return new EnhancedFeignBeanPostProcessor(enhancedFeignPlugins);
		}

		@Configuration
		static class PolarisReporterConfig {

			@Bean
			public SuccessPolarisReporter successPolarisReporter() {
				return new SuccessPolarisReporter();
			}

			@Bean
			public ExceptionPolarisReporter exceptionPolarisReporter() {
				return new ExceptionPolarisReporter();
			}
		}
	}

	/**
	 * Configuration for Polaris {@link RestTemplate} which can automatically bring in the call
	 * results for reporting.
	 *
	 * @author wh 2022/6/21
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "org.springframework.web.client.RestTemplate")
	protected static class PolarisRestTemplateAutoConfiguration {

		@Bean
		public EnhancedRestTemplateReporter polarisRestTemplateResponseErrorHandler(
				ConsumerAPI consumerAPI, @Autowired(required = false) PolarisResponseErrorHandler polarisResponseErrorHandler) {
			return new EnhancedRestTemplateReporter(consumerAPI, polarisResponseErrorHandler);
		}

		@Bean
		public EnhancedRestTemplateModifier polarisRestTemplateBeanPostProcessor(
				EnhancedRestTemplateReporter enhancedRestTemplateReporter) {
			return new EnhancedRestTemplateModifier(enhancedRestTemplateReporter);
		}
	}
}
