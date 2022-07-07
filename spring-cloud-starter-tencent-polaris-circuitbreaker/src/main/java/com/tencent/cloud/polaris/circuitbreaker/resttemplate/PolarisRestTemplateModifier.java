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

package com.tencent.cloud.polaris.circuitbreaker.resttemplate;

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Auto configuration RestTemplate, Find the RestTemplate bean annotated with {@link LoadBalanced},
 * then replace {@link org.springframework.web.client.ResponseErrorHandler}
 * with {@link PolarisRestTemplateResponseErrorHandler} .
 *
 * @author wh 2022/6/21
 */
public class PolarisRestTemplateModifier implements ApplicationContextAware, SmartInitializingSingleton {

	private final PolarisRestTemplateResponseErrorHandler polarisRestTemplateResponseErrorHandler;
	private ApplicationContext applicationContext;

	public PolarisRestTemplateModifier(PolarisRestTemplateResponseErrorHandler polarisRestTemplateResponseErrorHandler) {
		this.polarisRestTemplateResponseErrorHandler = polarisRestTemplateResponseErrorHandler;
	}

	@Override
	public void afterSingletonsInstantiated() {
		Map<String, Object> beans = this.applicationContext.getBeansWithAnnotation(LoadBalanced.class);
		if (!ObjectUtils.isEmpty(beans)) {
			beans.forEach(this::initRestTemplate);
		}
	}

	private void initRestTemplate(String beanName, Object bean) {
		if (bean instanceof RestTemplate) {
			RestTemplate restTemplate = (RestTemplate) bean;
			restTemplate.setErrorHandler(polarisRestTemplateResponseErrorHandler);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
