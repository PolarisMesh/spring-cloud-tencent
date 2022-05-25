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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.JacksonUtils;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.Ordered;
import org.springframework.util.CollectionUtils;

import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.CUSTOM_METADATA;

/**
 * Interceptor used for adding the metadata in http headers from context when web client
 * is Feign.
 *
 * @author Haotian Zhang
 */
public class EncodeTransferMedataFeignInterceptor implements RequestInterceptor, Ordered {

	private static final Logger LOG = LoggerFactory.getLogger(EncodeTransferMedataFeignInterceptor.class);

	@Override
	public int getOrder() {
		return MetadataConstant.OrderConstant.METADATA_2_HEADER_INTERCEPTOR_ORDER;
	}

	@Override
	public void apply(RequestTemplate requestTemplate) {
		// get metadata of current thread
		MetadataContext metadataContext = MetadataContextHolder.get();

		// add new metadata and cover old
		if (!CollectionUtils.isEmpty(requestTemplate.headers())
				&& !CollectionUtils.isEmpty(requestTemplate.headers().get(CUSTOM_METADATA))) {
			for (String headerMetadataStr : requestTemplate.headers().get(CUSTOM_METADATA)) {
				try {
					// Since feign-core 11.1, RequestTemplate Adapted to RFC6570, See: https://tools.ietf.org/html/rfc6570#section-2.2
					// Feign ISSUES : https://github.com/OpenFeign/feign/pull/1203
					// Feign ISSUES : https://github.com/OpenFeign/feign/issues/1305
					// Fixed : If you used RequestTemplate#header(name,value) method , the value needs to be encoded using URLEncoder .
					headerMetadataStr = URLDecoder.decode(headerMetadataStr, "UTF-8");
					Map<String, String> headerMetadataMap = JacksonUtils.deserialize2Map(headerMetadataStr);
					for (String key : headerMetadataMap.keySet()) {
						metadataContext.putTransitiveCustomMetadata(key, headerMetadataMap.get(key));
					}
				}
				catch (UnsupportedEncodingException e) {
					LOG.error("Set header failed.", e);
					throw new RuntimeException(e);
				}
			}
		}

		Map<String, String> customMetadata = metadataContext.getAllTransitiveCustomMetadata();
		if (!CollectionUtils.isEmpty(customMetadata)) {
			String metadataStr = JacksonUtils.serialize2Json(customMetadata);
			requestTemplate.removeHeader(CUSTOM_METADATA);
			try {
				requestTemplate.header(CUSTOM_METADATA, URLEncoder.encode(metadataStr, "UTF-8"));
			}
			catch (UnsupportedEncodingException e) {
				LOG.error("Set header failed.", e);
				requestTemplate.header(CUSTOM_METADATA, metadataStr);
			}
		}
	}

}
