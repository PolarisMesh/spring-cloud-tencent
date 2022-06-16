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

package com.tencent.cloud.common.metadata;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tencent.cloud.common.metadata.config.MetadataLocalProperties;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * manage metadata from env/config file.
 *
 *@author lepdou 2022-05-20
 */
public class StaticMetadataManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(StaticMetadataManager.class);

	private static final String ENV_METADATA_PREFIX = "SCT_METADATA_CONTENT_";
	private static final int ENV_METADATA_PREFIX_LENGTH = ENV_METADATA_PREFIX.length();
	private static final String ENV_METADATA_CONTENT_TRANSITIVE = "SCT_METADATA_CONTENT_TRANSITIVE";
	private static final String ENV_METADATA_ZONE = "SCT_METADATA_ZONE";
	private static final String ENV_METADATA_REGION = "SCT_METADATA_REGION";
	private static final String ENV_METADATA_CAMPUS = "SCT_METADATA_CAMPUS";

	/**
	 * the metadata key of region.
	 */
	public static final String LOCATION_KEY_REGION = "region";
	/**
	 * the metadata key of zone.
	 */
	public static final String LOCATION_KEY_ZONE = "zone";
	/**
	 * the metadata key of campus/datacenter.
	 */
	public static final String LOCATION_KEY_CAMPUS = "campus";

	private Map<String, String> envMetadata;
	private Map<String, String> envTransitiveMetadata;
	private Map<String, String> configMetadata;
	private Map<String, String> configTransitiveMetadata;
	private Map<String, String> mergedStaticMetadata;
	private Map<String, String> mergedStaticTransitiveMetadata;
	private String zone;
	private String region;
	private String campus;

	public StaticMetadataManager(MetadataLocalProperties metadataLocalProperties) {
		parseConfigMetadata(metadataLocalProperties);
		parseEnvMetadata();
		merge();
		parseLocationMetadata();

		LOGGER.info("[SCT] Loaded static metadata info. {}", this);
	}

	private void parseEnvMetadata() {
		Map<String, String> allEnvs = System.getenv();

		envMetadata = new HashMap<>();
		// parse all metadata
		for (Map.Entry<String, String> entry : allEnvs.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if (StringUtils.isNotBlank(key) && key.startsWith(ENV_METADATA_PREFIX)
					&& !key.equals(ENV_METADATA_CONTENT_TRANSITIVE)) {
				String sourceKey = StringUtils.substring(key, ENV_METADATA_PREFIX_LENGTH);
				envMetadata.put(sourceKey, value);

				LOGGER.info("[SCT] resolve metadata from env. key = {}, value = {}", sourceKey, value);
			}
		}
		envMetadata = Collections.unmodifiableMap(envMetadata);

		envTransitiveMetadata = new HashMap<>();
		// parse transitive metadata
		String transitiveKeys = allEnvs.get(ENV_METADATA_CONTENT_TRANSITIVE);
		if (StringUtils.isNotBlank(transitiveKeys)) {
			String[] keyArr = StringUtils.split(transitiveKeys, ",");
			if (keyArr != null && keyArr.length > 0) {
				for (String key : keyArr) {
					String value = envMetadata.get(key);
					if (StringUtils.isNotBlank(value)) {
						envTransitiveMetadata.put(key, value);
					}
				}
			}
		}
		envTransitiveMetadata = Collections.unmodifiableMap(envTransitiveMetadata);
	}

	private void parseConfigMetadata(MetadataLocalProperties metadataLocalProperties) {
		Map<String, String> allMetadata = metadataLocalProperties.getContent();
		List<String> transitiveKeys = metadataLocalProperties.getTransitive();

		Map<String, String> result = new HashMap<>();
		for (String key : transitiveKeys) {
			if (allMetadata.containsKey(key)) {
				result.put(key, allMetadata.get(key));
			}
		}

		configTransitiveMetadata = Collections.unmodifiableMap(result);
		configMetadata = Collections.unmodifiableMap(allMetadata);
	}

	private void merge() {
		// env priority is bigger than config
		Map<String, String> mergedMetadataResult = new HashMap<>();

		mergedMetadataResult.putAll(configMetadata);
		mergedMetadataResult.putAll(envMetadata);

		this.mergedStaticMetadata = Collections.unmodifiableMap(mergedMetadataResult);

		Map<String, String> mergedTransitiveMetadataResult = new HashMap<>();
		mergedTransitiveMetadataResult.putAll(configTransitiveMetadata);
		mergedTransitiveMetadataResult.putAll(envTransitiveMetadata);

		this.mergedStaticTransitiveMetadata = Collections.unmodifiableMap(mergedTransitiveMetadataResult);
	}

	private void parseLocationMetadata() {
		zone = System.getenv(ENV_METADATA_ZONE);
		region = System.getenv(ENV_METADATA_REGION);
		campus = System.getenv(ENV_METADATA_CAMPUS);
	}

	public Map<String, String> getAllEnvMetadata() {
		return envMetadata;
	}

	public Map<String, String> getEnvTransitiveMetadata() {
		return envTransitiveMetadata;
	}

	public Map<String, String> getAllConfigMetadata() {
		return configMetadata;
	}

	public Map<String, String> getConfigTransitiveMetadata() {
		return configTransitiveMetadata;
	}

	public Map<String, String> getMergedStaticMetadata() {
		return mergedStaticMetadata;
	}

	public Map<String, String> getMergedStaticTransitiveMetadata() {
		return mergedStaticTransitiveMetadata;
	}

	public String getZone() {
		return zone;
	}

	public String getRegion() {
		return region;
	}

	public String getCampus() {
		return campus;
	}

	public Map<String, String> getLocationMetadata() {
		Map<String, String> locationMetadata = new HashMap<>();
		if (StringUtils.isNotBlank(region)) {
			locationMetadata.put(LOCATION_KEY_REGION, region);
		}
		if (StringUtils.isNotBlank(zone)) {
			locationMetadata.put(LOCATION_KEY_ZONE, zone);
		}
		if (StringUtils.isNotBlank(campus)) {
			locationMetadata.put(LOCATION_KEY_CAMPUS, campus);
		}
		return locationMetadata;
	}

	@Override
	public String toString() {
		return "StaticMetadataManager{" +
				"envMetadata=" + envMetadata +
				", envTransitiveMetadata=" + envTransitiveMetadata +
				", configMetadata=" + configMetadata +
				", configTransitiveMetadata=" + configTransitiveMetadata +
				", mergedStaticMetadata=" + mergedStaticMetadata +
				", mergedStaticTransitiveMetadata=" + mergedStaticTransitiveMetadata +
				", zone='" + zone + '\'' +
				", region='" + region + '\'' +
				", campus='" + campus + '\'' +
				'}';
	}
}
