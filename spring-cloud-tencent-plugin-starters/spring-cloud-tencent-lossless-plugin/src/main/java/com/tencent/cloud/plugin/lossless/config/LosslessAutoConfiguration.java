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

package com.tencent.cloud.plugin.lossless.config;

import com.tencent.cloud.plugin.lossless.LosslessRegistryAspect;
import com.tencent.cloud.polaris.context.ConditionalOnPolarisEnabled;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.cloud.rpc.enhancement.transformer.RegistrationTransformer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Autoconfiguration of lossless.
 *
 * @author Shedfree Wu
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnPolarisEnabled
@Import(LosslessPropertiesAutoConfiguration.class)
public class LosslessAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public LosslessRegistryAspect losslessRegistryAspect(
			ServiceRegistry serviceRegistry, Registration registration, PolarisContextProperties properties,
			LosslessProperties losslessProperties, PolarisSDKContextManager polarisSDKContextManager,
			RegistrationTransformer registrationTransformer) {
		return new LosslessRegistryAspect(serviceRegistry, registration, properties, losslessProperties,
				polarisSDKContextManager, registrationTransformer);
	}

}
