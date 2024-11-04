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

package com.tencent.cloud.polaris.ratelimit.utils;

import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.common.util.ResourceFileUtils;
import com.tencent.cloud.polaris.ratelimit.config.PolarisRateLimitProperties;
import com.tencent.cloud.polaris.ratelimit.constant.RateLimitConstant;
import com.tencent.polaris.api.plugin.stat.TraceConstants;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.assembly.api.AssemblyAPI;
import com.tencent.polaris.assembly.api.pojo.TraceAttributes;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.StringUtils;

/**
 * Rate limit utils.
 *
 * @author lepdou 2022-04-20
 */
public final class RateLimitUtils {

	private static final Logger LOG = LoggerFactory.getLogger(RateLimitUtils.class);

	private RateLimitUtils() {

	}

	public static String getRejectTips(PolarisRateLimitProperties polarisRateLimitProperties) {
		String tips = polarisRateLimitProperties.getRejectRequestTips();

		if (StringUtils.hasText(tips)) {
			return tips;
		}

		String rejectFilePath = polarisRateLimitProperties.getRejectRequestTipsFilePath();
		if (StringUtils.hasText(rejectFilePath)) {
			try {
				tips = ResourceFileUtils.readFile(rejectFilePath);
			}
			catch (Exception e) {
				LOG.error("[RateLimit] Read custom reject tips file error. path = {}",
						rejectFilePath, e);
			}
		}

		if (StringUtils.hasText(tips)) {
			return tips;
		}

		return RateLimitConstant.QUOTA_LIMITED_INFO;
	}

	public static void reportTrace(AssemblyAPI assemblyAPI, String ruleId) {
		try {
			if (assemblyAPI != null) {
				Map<String, String> attributes = new HashMap<>();
				attributes.put(TraceConstants.RateLimitRuleId, ruleId);
				TraceAttributes traceAttributes = new TraceAttributes();
				traceAttributes.setAttributes(attributes);
				traceAttributes.setAttributeLocation(TraceAttributes.AttributeLocation.SPAN);
				assemblyAPI.updateTraceAttributes(traceAttributes);
			}
		}
		catch (Throwable throwable) {
			LOG.warn("[RateLimit] Report rule id {} to trace error.", ruleId, throwable);
		}
	}

	public static void release(QuotaResponse quotaResponse) {
		if (quotaResponse != null && CollectionUtils.isNotEmpty(quotaResponse.getReleaseList())) {
			for (Runnable release : quotaResponse.getReleaseList()) {
				try {
					release.run();
				}
				catch (Throwable throwable) {
					LOG.warn("[RateLimit] Release error.", throwable);
				}
			}
		}
	}
}
