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

package com.tencent.cloud.metadata.provider;

import com.tencent.cloud.common.util.UrlUtils;
import com.tencent.cloud.common.util.expresstion.SpringWebExpressionLabelUtils;
import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.MetadataProvider;

import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * MetadataProvider used for Reactive.
 *
 * @author Shedfree Wu
 */
public class ReactiveMetadataProvider implements MetadataProvider {

	private ServerHttpRequest serverHttpRequest;

	private String callerIp;

	public ReactiveMetadataProvider(ServerHttpRequest serverHttpRequest, String callerIp) {
		this.serverHttpRequest = serverHttpRequest;
		this.callerIp = callerIp;
	}

	@Override
	public String getRawMetadataStringValue(String key) {
		switch (key) {
		case MessageMetadataContainer.LABEL_KEY_METHOD:
			return serverHttpRequest.getMethod().name();
		case MessageMetadataContainer.LABEL_KEY_PATH:
			return UrlUtils.decode(serverHttpRequest.getPath().toString());
		case MessageMetadataContainer.LABEL_KEY_CALLER_IP:
			return callerIp;
		default:
			return null;
		}
	}

	@Override
	public String getRawMetadataMapValue(String key, String mapKey) {
		switch (key) {
			case MessageMetadataContainer.LABEL_MAP_KEY_HEADER:
				return UrlUtils.decode(SpringWebExpressionLabelUtils.getHeaderValue(serverHttpRequest, mapKey, null));
			case MessageMetadataContainer.LABEL_MAP_KEY_COOKIE:
				return UrlUtils.decode(SpringWebExpressionLabelUtils.getCookieValue(serverHttpRequest, mapKey, null));
			case MessageMetadataContainer.LABEL_MAP_KEY_QUERY:
				return UrlUtils.decode(SpringWebExpressionLabelUtils.getQueryValue(serverHttpRequest, mapKey, null));
			default:
				return null;
		}
	}
}
