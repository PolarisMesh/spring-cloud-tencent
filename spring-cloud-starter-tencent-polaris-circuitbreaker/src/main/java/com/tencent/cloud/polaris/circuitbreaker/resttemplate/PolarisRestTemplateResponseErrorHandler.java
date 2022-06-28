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

package com.tencent.cloud.polaris.circuitbreaker.resttemplate;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Objects;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.util.ReflectionUtils;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.api.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

/**
 * @author : wh
 * @date : 2022/6/21 17:25
 * @description: Extend ResponseErrorHandler to get request information
 */
public class PolarisRestTemplateResponseErrorHandler implements ResponseErrorHandler {

	private static final Logger LOG = LoggerFactory.getLogger(PolarisRestTemplateResponseErrorHandler.class);

	private static final String FileName = "connection";

	private final ConsumerAPI consumerAPI;

	private final PolarisResponseErrorHandler polarisResponseErrorHandler;


	public PolarisRestTemplateResponseErrorHandler(ConsumerAPI consumerAPI, PolarisResponseErrorHandler polarisResponseErrorHandler) {
		this.consumerAPI = consumerAPI;
		this.polarisResponseErrorHandler = polarisResponseErrorHandler;
	}

	@Override
	public boolean hasError(ClientHttpResponse response) {
		return true;
	}

	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		if (Objects.nonNull(polarisResponseErrorHandler)) {
			if (polarisResponseErrorHandler.hasError(response)) {
				polarisResponseErrorHandler.handleError(response);
			}
		}
	}

	public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
		ServiceCallResult resultRequest = null;
		try {
			resultRequest = builderServiceCallResult(url, response);
		}
		catch (IOException e) {
			LOG.error("Will report response of {} url {}", response, url, e);
			throw e;
		}
		finally {
			consumerAPI.updateServiceCallResult(resultRequest);
		}
	}

	private ServiceCallResult builderServiceCallResult(URI uri, ClientHttpResponse response) throws IOException {
		ServiceCallResult resultRequest = new ServiceCallResult();
		String serviceName = uri.getHost();
		resultRequest.setService(serviceName);
		resultRequest.setNamespace(MetadataContext.LOCAL_NAMESPACE);
		resultRequest.setMethod(uri.getPath());
		resultRequest.setRetStatus(RetStatus.RetSuccess);
		String sourceNamespace = MetadataContext.LOCAL_NAMESPACE;
		String sourceService = MetadataContext.LOCAL_SERVICE;
		if (StringUtils.isNotBlank(sourceNamespace) && StringUtils.isNotBlank(sourceService)) {
			resultRequest.setCallerService(new ServiceKey(sourceNamespace, sourceService));
		}
		HttpURLConnection connection = (HttpURLConnection) ReflectionUtils.getFieldValue(response, FileName);
		URL url = connection.getURL();
		resultRequest.setHost(url.getHost());
		resultRequest.setPort(url.getPort());
		if (response.getStatusCode().value() > 500) {
			resultRequest.setRetStatus(RetStatus.RetFail);
		}
		return resultRequest;
	}

}
