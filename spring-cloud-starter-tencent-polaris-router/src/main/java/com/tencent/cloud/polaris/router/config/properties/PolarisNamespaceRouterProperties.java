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

package com.tencent.cloud.polaris.router.config.properties;

import com.tencent.polaris.api.rpc.NamespaceRouterFailoverType;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * the configuration for namespace router.
 *
 * @author lepdou 2022-05-23
 */
@ConfigurationProperties(prefix = "spring.cloud.polaris.router.namespace-router")
public class PolarisNamespaceRouterProperties {

	private boolean enabled = false;

	private NamespaceRouterFailoverType failOver = NamespaceRouterFailoverType.all;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public NamespaceRouterFailoverType getFailOver() {
		return failOver;
	}

	public void setFailOver(NamespaceRouterFailoverType failOver) {
		this.failOver = failOver;
	}

	@Override
	public String toString() {
		return "PolarisNamespaceRouterProperties{" +
				"enabled=" + enabled +
				", failOver=" + failOver +
				'}';
	}
}
