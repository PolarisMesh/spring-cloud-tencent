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

package com.tencent.cloud.polaris.config.tsf;

import com.tencent.cloud.polaris.config.ConditionalOnPolarisConfigEnabled;
import com.tencent.cloud.polaris.config.adapter.PolarisConfigCustomExtensionLayer;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.tsf.adaptor.PolarisAdaptorTsfConfigExtensionLayer;
import com.tencent.cloud.polaris.config.tsf.controller.PolarisAdaptorTsfConfigController;
import com.tencent.cloud.polaris.context.tsf.ConditionalOnTsfEnabled;
import com.tencent.cloud.polaris.context.tsf.config.TsfCoreProperties;
import com.tencent.tsf.consul.config.watch.TsfConsulConfigRefreshEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * @author juanyinyang
 * @Date Jul 23, 2023 3:52:48 PM
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnTsfEnabled
@ConditionalOnPolarisConfigEnabled
public class PolarisAdaptorTsfConfigAutoConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisAdaptorTsfConfigAutoConfiguration.class);

	{
		System.setProperty("spring.cloud.polaris.config.refresh-type", "refresh_context");
		LOGGER.info(
				"[SCTT Config] PolarisAdaptorTsfConfigAutoConfiguration init set spring.cloud.polaris.config.refresh-type to refresh_context");
	}

	@Bean
	@ConditionalOnMissingBean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public PolarisConfigCustomExtensionLayer polarisAdaptorTsfConfigProcessCallback() {
		return new PolarisAdaptorTsfConfigExtensionLayer();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.cloud.consul.config.watch.enabled", matchIfMissing = true)
	public TsfConsulConfigRefreshEventListener polarisAdaptorTsfConsulRefreshEventListener() {
		return new TsfConsulConfigRefreshEventListener();
	}

	/**
	 * 初始化本类的条件：
	 * 1、关闭Spring Cloud Consul Config配置开关（如果开启Consul Config配置开关，那么初始化的是tsf自身的类ConfigController）
	 * 2、开启北极星配置（本类通过注解@ConditionalOnPolarisConfigEnabled开启）
	 * 3、tsf.config.instance.released-config.lookup.enabled的开关是打开的（默认不配置就是打开的）.
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnExpression("${spring.cloud.consul.config.enabled:true} == false and ${tsf.config.instance.released-config.lookup.enabled:true} == true")
	public PolarisAdaptorTsfConfigController polarisAdaptorTsfConfigController() {
		return new PolarisAdaptorTsfConfigController();
	}

	@Bean
	@ConditionalOnMissingBean
	public TsfConfigurationModifier tsfConfigModifier(TsfCoreProperties tsfCoreProperties, PolarisConfigProperties polarisConfigProperties) {
		return new TsfConfigurationModifier(tsfCoreProperties, polarisConfigProperties);
	}
}
