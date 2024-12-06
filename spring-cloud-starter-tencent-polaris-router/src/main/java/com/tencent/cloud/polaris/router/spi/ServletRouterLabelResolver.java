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

package com.tencent.cloud.polaris.router.spi;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.Ordered;

/**
 * Router label resolver for spring web http request.
 *
 * @author jarvisxiong 2022-08-04
 */
public interface ServletRouterLabelResolver extends Ordered {

	/**
	 * resolve labels from servlet http request. User can customize expression parser to extract labels.
	 *
	 * @param request the servlet http request.
	 * @param expressionLabelKeys the expression labels which are configured in router rule.
	 * @return resolved labels
	 */
	default Map<String, String> resolve(HttpServletRequest request, Set<String> expressionLabelKeys) {
		return Collections.emptyMap();
	}
}
