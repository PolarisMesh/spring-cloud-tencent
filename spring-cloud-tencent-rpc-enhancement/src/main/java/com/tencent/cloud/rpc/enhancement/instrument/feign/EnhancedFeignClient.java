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

package com.tencent.cloud.rpc.enhancement.instrument.feign;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedRequestContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedResponseContext;
import feign.Client;
import feign.Request;
import feign.Request.Options;
import feign.Response;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import static feign.Util.checkNotNull;

/**
 * Wrap for {@link Client}.
 *
 * @author Haotian Zhang
 */
public class EnhancedFeignClient implements Client {

	private final Client delegate;

	private final EnhancedPluginRunner pluginRunner;

	public EnhancedFeignClient(Client target, EnhancedPluginRunner pluginRunner) {
		this.delegate = checkNotNull(target, "target");
		this.pluginRunner = pluginRunner;
	}

	@Override
	public Response execute(Request request, Options options) throws IOException {
		EnhancedPluginContext enhancedPluginContext = new EnhancedPluginContext();

		HttpHeaders requestHeaders = new HttpHeaders();
		request.headers().forEach((s, strings) -> requestHeaders.addAll(s, new ArrayList<>(strings)));
		URI url = URI.create(request.url());

		EnhancedRequestContext enhancedRequestContext = EnhancedRequestContext.builder()
				.httpHeaders(requestHeaders)
				.httpMethod(HttpMethod.valueOf(request.httpMethod().name()))
				.url(url)
				.build();
		enhancedPluginContext.setRequest(enhancedRequestContext);
		enhancedPluginContext.setOriginRequest(request);

		enhancedPluginContext.setLocalServiceInstance(pluginRunner.getLocalServiceInstance());
		String svcName = request.requestTemplate().feignTarget().name();
		DefaultServiceInstance serviceInstance = new DefaultServiceInstance(
				String.format("%s-%s-%d", svcName, url.getHost(), url.getPort()),
				svcName, url.getHost(), url.getPort(), url.getScheme().equals("https"));
		// -1 means access directly by url
		if (serviceInstance.getPort() == -1) {
			enhancedPluginContext.setTargetServiceInstance(null, url);
		}
		else {
			enhancedPluginContext.setTargetServiceInstance(serviceInstance, url);
		}

		// Run pre enhanced plugins.
		pluginRunner.run(EnhancedPluginType.Client.PRE, enhancedPluginContext);

		long startMillis = System.currentTimeMillis();
		try {
			Response response = delegate.execute(request, options);
			enhancedPluginContext.setDelay(System.currentTimeMillis() - startMillis);

			HttpHeaders responseHeaders = new HttpHeaders();
			response.headers().forEach((s, strings) -> responseHeaders.addAll(s, new ArrayList<>(strings)));

			EnhancedResponseContext enhancedResponseContext = EnhancedResponseContext.builder()
					.httpStatus(response.status())
					.httpHeaders(responseHeaders)
					.build();
			enhancedPluginContext.setResponse(enhancedResponseContext);

			// Run post enhanced plugins.
			pluginRunner.run(EnhancedPluginType.Client.POST, enhancedPluginContext);
			return response;
		}
		catch (IOException origin) {
			enhancedPluginContext.setDelay(System.currentTimeMillis() - startMillis);
			enhancedPluginContext.setThrowable(origin);
			// Run exception enhanced feign plugins.
			pluginRunner.run(EnhancedPluginType.Client.EXCEPTION, enhancedPluginContext);
			throw origin;
		}
		finally {
			// Run finally enhanced plugins.
			pluginRunner.run(EnhancedPluginType.Client.FINALLY, enhancedPluginContext);
		}
	}
}
