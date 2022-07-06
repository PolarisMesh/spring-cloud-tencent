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

package com.tencent.cloud.polaris.loadbalancer.config;

import com.tencent.cloud.common.constant.ContextConstant;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties of Polaris loadbalancer.
 *
 * @author Haotian Zhang
 */
@ConfigurationProperties("spring.cloud.polaris.loadbalancer")
public class PolarisLoadBalancerProperties {

	/**
	 * If load-balance enabled.
	 */
	private Boolean enabled = true;

	/**
	 * Load balance strategy.
	 */
	private String strategy;

	/**
	 * Type of discovery server.
	 */
	private String discoveryType = ContextConstant.POLARIS;

	public String getStrategy() {
		return strategy;
	}

	public void setStrategy(String strategy) {
		this.strategy = strategy;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public String getDiscoveryType() {
		return discoveryType;
	}

	public void setDiscoveryType(String discoveryType) {
		this.discoveryType = discoveryType;
	}

	@Override
	public String toString() {
		return "PolarisLoadBalancerProperties{" + "loadbalancerEnabled=" + enabled + ", strategy='" + strategy + '\''
				+ '}';
	}
}
