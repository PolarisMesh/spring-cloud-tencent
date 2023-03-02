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

package com.tencent.cloud.plugin.pushgateway;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisStatPushGatewayProperties}.
 *
 * @author lingxiao.wlx
 */
public class PolarisStatPushGatewayPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(PolarisStatPushGatewayBootstrapConfiguration.class))
			.withPropertyValues("spring.cloud.polaris.enabled=true")
			.withPropertyValues("spring.cloud.polaris.stat.pushgateway.enabled=true")
			.withPropertyValues("spring.cloud.polaris.stat.pushgateway.address=127.0.0.1:9091")
			.withPropertyValues("spring.cloud.polaris.stat.pushgateway.push-interval=1000");

	@Test
	public void polarisStatPushGatewayPropertiesTest() {
		contextRunner.run(context -> {
			PolarisStatPushGatewayProperties polarisStatPushGatewayProperties = context.getBean(PolarisStatPushGatewayProperties.class);
			assertThat(polarisStatPushGatewayProperties.isEnabled()).isTrue();
			assertThat(polarisStatPushGatewayProperties.getAddress()).isEqualTo("127.0.0.1:9091");
			assertThat(polarisStatPushGatewayProperties.getPushInterval().toString()).isEqualTo("1000");
		});
	}
}
