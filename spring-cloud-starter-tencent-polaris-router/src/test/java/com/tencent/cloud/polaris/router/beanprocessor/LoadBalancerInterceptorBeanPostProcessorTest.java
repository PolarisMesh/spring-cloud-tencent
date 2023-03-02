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

package com.tencent.cloud.polaris.router.beanprocessor;

import com.tencent.cloud.common.util.BeanFactoryUtils;
import com.tencent.cloud.polaris.router.resttemplate.PolarisLoadBalancerInterceptor;
import com.tencent.cloud.polaris.router.spi.SpringWebRouterLabelResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test for ${@link LoadBalancerInterceptorBeanPostProcessor}.
 *
 * @author lepdou 2022-05-26
 */
@ExtendWith(MockitoExtension.class)
public class LoadBalancerInterceptorBeanPostProcessorTest {

	@Mock
	private LoadBalancerClient loadBalancerClient;
	@Mock
	private LoadBalancerRequestFactory loadBalancerRequestFactory;
	@Mock
	private BeanFactory beanFactory;

	@Test
	public void testWrapperLoadBalancerInterceptor() {
		when(beanFactory.getBean(LoadBalancerRequestFactory.class)).thenReturn(loadBalancerRequestFactory);
		when(beanFactory.getBean(LoadBalancerClient.class)).thenReturn(loadBalancerClient);

		try (MockedStatic<BeanFactoryUtils> mockedBeanFactoryUtils = Mockito.mockStatic(BeanFactoryUtils.class)) {
			mockedBeanFactoryUtils.when(() -> BeanFactoryUtils.getBeans(beanFactory, SpringWebRouterLabelResolver.class))
					.thenReturn(null);
			LoadBalancerInterceptor loadBalancerInterceptor = new LoadBalancerInterceptor(loadBalancerClient, loadBalancerRequestFactory);

			LoadBalancerInterceptorBeanPostProcessor processor = new LoadBalancerInterceptorBeanPostProcessor();
			processor.setBeanFactory(beanFactory);

			Object bean = processor.postProcessBeforeInitialization(loadBalancerInterceptor, "");

			assertThat(bean).isInstanceOf(PolarisLoadBalancerInterceptor.class);
		}
	}

	@Test
	public void testNotWrapperLoadBalancerInterceptor() {
		LoadBalancerInterceptorBeanPostProcessor processor = new LoadBalancerInterceptorBeanPostProcessor();
		processor.setBeanFactory(beanFactory);

		OtherBean otherBean = new OtherBean();
		Object bean = processor.postProcessBeforeInitialization(otherBean, "");
		assertThat(bean).isNotInstanceOf(PolarisLoadBalancerInterceptor.class);
		assertThat(bean).isInstanceOf(OtherBean.class);
	}

	static class OtherBean {

	}
}
