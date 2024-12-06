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

package com.tencent.cloud.polaris.router.beanprocessor;

import com.tencent.cloud.polaris.router.resttemplate.PolarisLoadBalancerInterceptor;
import com.tencent.cloud.polaris.router.resttemplate.RouterContextFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestFactory;
import org.springframework.lang.NonNull;

/**
 * Replace {@link LoadBalancerInterceptor} with {@link PolarisLoadBalancerInterceptor}.
 * PolarisLoadBalancerInterceptor can pass routing context information.
 *
 * @author lepdou 2022-05-18
 */
public class LoadBalancerInterceptorBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware {

	private BeanFactory factory;

	@Override
	public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
		this.factory = beanFactory;
	}

	@Override
	public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
		if (bean instanceof LoadBalancerInterceptor) {
			// Support rest template router.
			// Replaces the default LoadBalancerInterceptor implementation and returns a custom PolarisLoadBalancerInterceptor
			LoadBalancerRequestFactory requestFactory = this.factory.getBean(LoadBalancerRequestFactory.class);
			LoadBalancerClient loadBalancerClient = this.factory.getBean(LoadBalancerClient.class);
			RouterContextFactory routerContextFactory = this.factory.getBean(RouterContextFactory.class);

			return new PolarisLoadBalancerInterceptor(loadBalancerClient, requestFactory, routerContextFactory);
		}
		return bean;
	}
}
