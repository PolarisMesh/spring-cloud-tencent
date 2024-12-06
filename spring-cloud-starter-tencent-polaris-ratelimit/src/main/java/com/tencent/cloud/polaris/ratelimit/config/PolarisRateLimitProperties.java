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

package com.tencent.cloud.polaris.ratelimit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;

/**
 * The properties for rate limit.
 *
 * @author lepdou 2022-04-20
 */
@ConfigurationProperties("spring.cloud.polaris.ratelimit")
public class PolarisRateLimitProperties {

	/**
	 * custom tips when reject request.
	 */
	private String rejectRequestTips;

	/**
	 * context file path.
	 */
	private String rejectRequestTipsFilePath;

	/**
	 * custom http code when reject request.
	 */
	private int rejectHttpCode = HttpStatus.TOO_MANY_REQUESTS.value();

	/**
	 * Max queuing time when using unirate.
	 */
	private long maxQueuingTime = 1000L;

	public String getRejectRequestTips() {
		return rejectRequestTips;
	}

	public void setRejectRequestTips(String rejectRequestTips) {
		this.rejectRequestTips = rejectRequestTips;
	}

	public String getRejectRequestTipsFilePath() {
		return rejectRequestTipsFilePath;
	}

	public void setRejectRequestTipsFilePath(String rejectRequestTipsFilePath) {
		this.rejectRequestTipsFilePath = rejectRequestTipsFilePath;
	}

	public int getRejectHttpCode() {
		return rejectHttpCode;
	}

	public void setRejectHttpCode(int rejectHttpCode) {
		this.rejectHttpCode = rejectHttpCode;
	}

	public long getMaxQueuingTime() {
		return maxQueuingTime;
	}

	public void setMaxQueuingTime(long maxQueuingTime) {
		this.maxQueuingTime = maxQueuingTime;
	}
}
