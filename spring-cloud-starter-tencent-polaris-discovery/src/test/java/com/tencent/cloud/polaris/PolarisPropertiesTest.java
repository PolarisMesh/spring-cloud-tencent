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

package com.tencent.cloud.polaris;

import org.junit.Test;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Test for {@link PolarisDiscoveryProperties}.
 *
 * @author Haotian Zhang
 */
public class PolarisPropertiesTest {

	@Test
	public void testInitAndGetSet() {
		PolarisDiscoveryProperties temp = new PolarisDiscoveryProperties();
		try {
			temp.setNamespace(NAMESPACE_TEST);
			assertThat(temp.getNamespace()).isEqualTo(NAMESPACE_TEST);

			temp.setService(SERVICE_PROVIDER);
			assertThat(temp.getService()).isEqualTo(SERVICE_PROVIDER);

			temp.setToken("xxxxxx");
			assertThat(temp.getToken()).isEqualTo("xxxxxx");

			temp.init();
			assertThat(temp).isNotNull();
		}
		catch (Exception e) {
			fail();
			e.printStackTrace();
		}
	}

}
