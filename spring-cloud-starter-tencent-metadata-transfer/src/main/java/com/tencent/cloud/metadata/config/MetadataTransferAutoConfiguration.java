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

package com.tencent.cloud.metadata.config;

import java.util.List;
import java.util.Map;

import com.netflix.zuul.ZuulFilter;
import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.metadata.core.DecodeTransferMetadataReactiveFilter;
import com.tencent.cloud.metadata.core.DecodeTransferMetadataServletFilter;
import com.tencent.cloud.metadata.core.EncodeTransferMedataFeignInterceptor;
import com.tencent.cloud.metadata.core.EncodeTransferMedataRestTemplateInterceptor;
import com.tencent.cloud.metadata.core.EncodeTransferMedataScgFilter;
import com.tencent.cloud.metadata.core.EncodeTransferMetadataZuulFilter;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import static javax.servlet.DispatcherType.ASYNC;
import static javax.servlet.DispatcherType.ERROR;
import static javax.servlet.DispatcherType.FORWARD;
import static javax.servlet.DispatcherType.INCLUDE;
import static javax.servlet.DispatcherType.REQUEST;

/**
 * Metadata transfer auto configuration.
 *
 * @author Haotian Zhang
 */
@Configuration(proxyBeanMethods = false)
public class MetadataTransferAutoConfiguration {

	/**
	 * Create when web application type is SERVLET.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
	protected static class MetadataServletFilterConfig {

		@Bean
		public FilterRegistrationBean<DecodeTransferMetadataServletFilter> metadataServletFilterRegistrationBean(
				DecodeTransferMetadataServletFilter decodeTransferMetadataServletFilter) {
			FilterRegistrationBean<DecodeTransferMetadataServletFilter> filterRegistrationBean =
					new FilterRegistrationBean<>(decodeTransferMetadataServletFilter);
			filterRegistrationBean.setDispatcherTypes(ASYNC, ERROR, FORWARD, INCLUDE, REQUEST);
			filterRegistrationBean.setOrder(MetadataConstant.OrderConstant.WEB_FILTER_ORDER);
			return filterRegistrationBean;
		}

		@Bean
		public DecodeTransferMetadataServletFilter metadataServletFilter() {
			return new DecodeTransferMetadataServletFilter();
		}

	}

	/**
	 * Create when web application type is REACTIVE.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
	protected static class MetadataReactiveFilterConfig {

		@Bean
		public DecodeTransferMetadataReactiveFilter metadataReactiveFilter() {
			return new DecodeTransferMetadataReactiveFilter();
		}

	}

	/**
	 * Create when gateway application is Zuul.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "com.netflix.zuul.http.ZuulServlet")
	protected static class MetadataTransferZuulFilterConfig {

		@Bean
		public ZuulFilter encodeTransferMetadataZuulFilter() {
			return new EncodeTransferMetadataZuulFilter();
		}

	}

	/**
	 * Create when gateway application is SCG.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GlobalFilter")
	protected static class MetadataTransferScgFilterConfig {

		@Bean
		public GlobalFilter encodeTransferMedataScgFilter() {
			return new EncodeTransferMedataScgFilter();
		}

	}

	/**
	 * Create when Feign exists.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "feign.Feign")
	protected static class MetadataTransferFeignInterceptorConfig {

		@Bean
		public EncodeTransferMedataFeignInterceptor encodeTransferMedataFeignInterceptor() {
			return new EncodeTransferMedataFeignInterceptor();
		}

	}

	/**
	 * Create when RestTemplate exists.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "org.springframework.web.client.RestTemplate")
	protected static class MetadataTransferRestTemplateConfig implements ApplicationContextAware {

		private ApplicationContext context;

		@Bean
		public EncodeTransferMedataRestTemplateInterceptor encodeTransferMedataRestTemplateInterceptor() {
			return new EncodeTransferMedataRestTemplateInterceptor();
		}

		@Bean
		BeanPostProcessor encodeTransferMetadataRestTemplatePostProcessor(
				EncodeTransferMedataRestTemplateInterceptor encodeTransferMedataRestTemplateInterceptor) {
			// Coping with multiple bean injection scenarios
			Map<String, RestTemplate> beans = this.context.getBeansOfType(RestTemplate.class);
			// If the restTemplate has been created when the
			// MetadataRestTemplatePostProcessor Bean
			// is initialized, then manually set the interceptor.
			if (!CollectionUtils.isEmpty(beans)) {
				for (RestTemplate restTemplate : beans.values()) {
					List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
					// Avoid setting interceptor repeatedly.
					if (!interceptors.contains(encodeTransferMedataRestTemplateInterceptor)) {
						interceptors.add(encodeTransferMedataRestTemplateInterceptor);
						restTemplate.setInterceptors(interceptors);
					}
				}
			}
			return new EncodeTransferMetadataRestTemplatePostProcessor(encodeTransferMedataRestTemplateInterceptor);
		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			this.context = applicationContext;
		}

		public static class EncodeTransferMetadataRestTemplatePostProcessor
				implements BeanPostProcessor {

			private final EncodeTransferMedataRestTemplateInterceptor encodeTransferMedataRestTemplateInterceptor;

			EncodeTransferMetadataRestTemplatePostProcessor(
					EncodeTransferMedataRestTemplateInterceptor encodeTransferMedataRestTemplateInterceptor) {
				this.encodeTransferMedataRestTemplateInterceptor = encodeTransferMedataRestTemplateInterceptor;
			}

			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) {
				return bean;
			}

			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) {
				if (bean instanceof RestTemplate) {
					RestTemplate restTemplate = (RestTemplate) bean;
					List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
					// Avoid setting interceptor repeatedly.
					if (!interceptors.contains(encodeTransferMedataRestTemplateInterceptor)) {
						interceptors.add(this.encodeTransferMedataRestTemplateInterceptor);
						restTemplate.setInterceptors(interceptors);
					}
				}
				return bean;
			}
		}
	}
}
