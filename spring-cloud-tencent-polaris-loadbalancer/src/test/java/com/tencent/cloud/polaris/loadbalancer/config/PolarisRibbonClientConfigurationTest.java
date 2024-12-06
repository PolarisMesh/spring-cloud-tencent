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

package com.tencent.cloud.polaris.loadbalancer.config;

import com.tencent.cloud.polaris.loadbalancer.PolarisLoadBalancer;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static com.tencent.polaris.test.common.Consts.PORT;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisRibbonClientConfiguration}.
 *
 * @author wlx
 */
public class PolarisRibbonClientConfigurationTest {

	private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner();

	@Test
	public void testDefaultInitialization() {
		this.applicationContextRunner
				.withConfiguration(AutoConfigurations.of(
						TestApplication.class,
						PolarisRibbonClientConfiguration.class))
				.withPropertyValues("spring.application.name=" + SERVICE_PROVIDER)
				.withPropertyValues("server.port=" + PORT)
				.withPropertyValues("spring.cloud.polaris.address=grpc://127.0.0.1:10081")
				.run(context -> {
					assertThat(context).hasSingleBean(PolarisRibbonClientConfiguration.class);
					assertThat(context).hasSingleBean(PolarisLoadBalancer.class);
				});
	}

	@SpringBootApplication
	static class TestApplication {
	}
}
