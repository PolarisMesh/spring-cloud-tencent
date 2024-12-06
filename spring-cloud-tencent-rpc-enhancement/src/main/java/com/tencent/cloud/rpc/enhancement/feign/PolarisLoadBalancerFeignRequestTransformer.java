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

package com.tencent.cloud.rpc.enhancement.feign;

import com.tencent.cloud.common.metadata.MetadataContextHolder;
import feign.Request;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.openfeign.loadbalancer.LoadBalancerFeignRequestTransformer;

import static com.tencent.cloud.rpc.enhancement.resttemplate.PolarisLoadBalancerRequestTransformer.LOAD_BALANCER_SERVICE_INSTANCE;

/**
 * PolarisLoadBalancerFeignRequestTransformer.
 *
 * @author sean yu
 */
public class PolarisLoadBalancerFeignRequestTransformer implements LoadBalancerFeignRequestTransformer {

	/**
	 * Transform Request, add Loadbalancer ServiceInstance to MetadataContext.
	 * @param request request
	 * @param instance instance
	 * @return HttpRequest
	 */
	@Override
	public Request transformRequest(Request request, ServiceInstance instance) {
		if (instance != null) {
			MetadataContextHolder.get().setLoadbalancer(LOAD_BALANCER_SERVICE_INSTANCE, instance);
		}
		return request;
	}

}
