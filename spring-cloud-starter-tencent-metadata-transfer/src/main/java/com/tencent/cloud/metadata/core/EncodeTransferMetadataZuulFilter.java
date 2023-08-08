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

package com.tencent.cloud.metadata.core;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.JacksonUtils;

import org.springframework.util.CollectionUtils;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;
import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.CUSTOM_DISPOSABLE_METADATA;
import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.CUSTOM_METADATA;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.ROUTE_TYPE;

/**
 * Zuul filter used for writing metadata in HTTP request header.
 *
 * @author Haotian Zhang
 */
public class EncodeTransferMetadataZuulFilter extends ZuulFilter {

	@Override
	public String filterType() {
		return ROUTE_TYPE;
	}

	@Override
	public int filterOrder() {
		return OrderConstant.Client.Zuul.ENCODE_TRANSFER_METADATA_FILTER_ORDER;
	}

	@Override
	public boolean shouldFilter() {
		return true;
	}

	@Override
	public Object run() {
		// get request context
		RequestContext requestContext = RequestContext.getCurrentContext();

		// get metadata of current thread
		MetadataContext metadataContext = MetadataContextHolder.get();

		Map<String, String> customMetadata = metadataContext.getCustomMetadata();
		Map<String, String> disposableMetadata = metadataContext.getDisposableMetadata();

		// Rebuild Metadata Header
		this.buildMetadataHeader(requestContext, customMetadata, CUSTOM_METADATA);
		this.buildMetadataHeader(requestContext, disposableMetadata, CUSTOM_DISPOSABLE_METADATA);

		TransHeadersTransfer.transfer(requestContext.getRequest());
		return null;
	}

	/**
	 * Set metadata into the request header for {@link RequestContext} .
	 *
	 * @param context    instance of {@link RequestContext}
	 * @param metadata   metadata map .
	 * @param headerName target metadata http header name .
	 */
	private void buildMetadataHeader(RequestContext context, Map<String, String> metadata, String headerName) {
		if (!CollectionUtils.isEmpty(metadata)) {
			String encodedMetadata = JacksonUtils.serialize2Json(metadata);
			try {
				context.addZuulRequestHeader(headerName, URLEncoder.encode(encodedMetadata, UTF_8));
			}
			catch (UnsupportedEncodingException e) {
				context.addZuulRequestHeader(headerName, encodedMetadata);
			}
		}
	}
}
