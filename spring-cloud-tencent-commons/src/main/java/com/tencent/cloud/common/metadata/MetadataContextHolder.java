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

package com.tencent.cloud.common.metadata;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.common.metadata.config.MetadataLocalProperties;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;

import org.springframework.util.CollectionUtils;

/**
 * Metadata Context Holder.
 *
 * @author Haotian Zhang
 */
public final class MetadataContextHolder {

	private static final ThreadLocal<MetadataContext> METADATA_CONTEXT = new InheritableThreadLocal<>();

	private static MetadataLocalProperties metadataLocalProperties;
	private static StaticMetadataManager staticMetadataManager;

	private MetadataContextHolder() {
	}

	/**
	 * Get metadata context. Create if not existing.
	 * @return METADATA_CONTEXT
	 */
	public static MetadataContext get() {
		if (METADATA_CONTEXT.get() != null) {
			return METADATA_CONTEXT.get();
		}

		if (metadataLocalProperties == null) {
			metadataLocalProperties = (MetadataLocalProperties) ApplicationContextAwareUtils
					.getApplicationContext().getBean("metadataLocalProperties");
		}
		if (staticMetadataManager == null) {
			staticMetadataManager = (StaticMetadataManager) ApplicationContextAwareUtils
					.getApplicationContext().getBean("metadataManager");
		}

		// init static transitive metadata
		MetadataContext metadataContext = new MetadataContext();
		metadataContext.putFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE,
				staticMetadataManager.getMergedStaticTransitiveMetadata());

		METADATA_CONTEXT.set(metadataContext);

		return METADATA_CONTEXT.get();
	}

	/**
	 * Set metadata context.
	 * @param metadataContext metadata context
	 */
	public static void set(MetadataContext metadataContext) {
		METADATA_CONTEXT.set(metadataContext);
	}

	/**
	 * Save metadata map to thread local.
	 * @param dynamicTransitiveMetadata custom metadata collection
	 */
	public static void init(Map<String, String> dynamicTransitiveMetadata) {
		// Init ThreadLocal.
		MetadataContextHolder.remove();
		MetadataContext metadataContext = MetadataContextHolder.get();

		// Save transitive metadata to ThreadLocal.
		if (!CollectionUtils.isEmpty(dynamicTransitiveMetadata)) {
			Map<String, String> staticTransitiveMetadata = metadataContext.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
			Map<String, String> mergedTransitiveMetadata = new HashMap<>();
			mergedTransitiveMetadata.putAll(staticTransitiveMetadata);
			mergedTransitiveMetadata.putAll(dynamicTransitiveMetadata);

			metadataContext.putFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE,
					Collections.unmodifiableMap(mergedTransitiveMetadata));
		}
		MetadataContextHolder.set(metadataContext);
	}

	/**
	 * Remove metadata context.
	 */
	public static void remove() {
		METADATA_CONTEXT.remove();
	}
}
