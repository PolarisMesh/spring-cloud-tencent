/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 *  Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 *  Licensed under the BSD 3-Clause License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/BSD-3-Clause
 *
 *  Unless required by applicable law or agreed to in writing, software distributed
 *  under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations under the License.
 *
 */

package com.tencent.cloud.polaris.config.adapter;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.spring.property.PlaceholderHelper;
import com.tencent.cloud.polaris.config.spring.property.SpringValue;
import com.tencent.cloud.polaris.config.spring.property.SpringValueRegistry;
import com.tencent.polaris.configuration.api.core.ConfigKVFileChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.CollectionUtils;

/**
 * 1. Listen to the Polaris server configuration publishing event 2. Write the changed
 * configuration content to propertySource 3. Refresh the context through contextRefresher
 *
 * @author lepdou 2022-03-28
 */
public class PolarisPropertySourceAutoRefresher
		implements ApplicationListener<ApplicationReadyEvent>, ApplicationContextAware {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(PolarisPropertySourceAutoRefresher.class);

	private final PolarisConfigProperties polarisConfigProperties;

	private final PolarisPropertySourceManager polarisPropertySourceManager;

	private final boolean typeConverterHasConvertIfNecessaryWithFieldParameter;

	private TypeConverter typeConverter;

	private ApplicationContext applicationContext;

	private final SpringValueRegistry springValueRegistry;

	private ConfigurableBeanFactory beanFactory;

	private final PlaceholderHelper placeholderHelper;

	private final ContextRefresher contextRefresher;

	private final AtomicBoolean registered = new AtomicBoolean(false);

	public PolarisPropertySourceAutoRefresher(
			PolarisConfigProperties polarisConfigProperties,
			PolarisPropertySourceManager polarisPropertySourceManager,
			ContextRefresher contextRefresher,
			SpringValueRegistry springValueRegistry,
			PlaceholderHelper placeholderHelper) {
		this.polarisConfigProperties = polarisConfigProperties;
		this.polarisPropertySourceManager = polarisPropertySourceManager;
		this.contextRefresher = contextRefresher;
		this.springValueRegistry = springValueRegistry;
		this.placeholderHelper = placeholderHelper;
		this.typeConverterHasConvertIfNecessaryWithFieldParameter = testTypeConverterHasConvertIfNecessaryWithFieldParameter();

	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
		this.beanFactory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
		this.typeConverter = this.beanFactory.getTypeConverter();
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		registerPolarisConfigPublishEvent();
	}

	private void registerPolarisConfigPublishEvent() {
		if (!polarisConfigProperties.isAutoRefresh()) {
			return;
		}

		List<PolarisPropertySource> polarisPropertySources = polarisPropertySourceManager
				.getAllPropertySources();
		if (CollectionUtils.isEmpty(polarisPropertySources)) {
			return;
		}

		if (!registered.compareAndSet(false, true)) {
			return;
		}

		// register polaris config publish event
		for (PolarisPropertySource polarisPropertySource : polarisPropertySources) {
			polarisPropertySource.getConfigKVFile()
					.addChangeListener((ConfigKVFileChangeListener) configKVFileChangeEvent -> {
						LOGGER.info(
								"[SCT Config]  received polaris config change event and will refresh spring context."
										+ "namespace = {}, group = {}, fileName = {}",
								polarisPropertySource.getNamespace(),
								polarisPropertySource.getGroup(),
								polarisPropertySource.getFileName());

						Map<String, Object> source = polarisPropertySource
								.getSource();
						for (String changedKey : configKVFileChangeEvent.changedKeys()) {

							// 1. check whether the changed key is relevant
							Collection<SpringValue> targetValues = springValueRegistry.get(beanFactory, changedKey);
							if (targetValues == null || targetValues.isEmpty()) {
								continue;
							}

							// 2. update the value
							for (SpringValue val : targetValues) {
								updateSpringValue(val);
							}
						}

					});
		}
	}


	private void updateSpringValue(SpringValue springValue) {
		try {
			Object value = resolvePropertyValue(springValue);
			springValue.update(value);

			LOGGER.info("Auto update polaris changed value successfully, new value: {}, {}", value,
					springValue);
		}
		catch (Throwable ex) {
			LOGGER.error("Auto update polaris changed value failed, {}", springValue.toString(), ex);
		}
	}


	/**
	 * Logic transplanted from DefaultListableBeanFactory.
	 *
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#doResolveDependency(org.springframework.beans.factory.config.DependencyDescriptor,
	 * java.lang.String, java.util.Set, org.springframework.beans.TypeConverter)
	 */
	private Object resolvePropertyValue(SpringValue springValue) {
		// value will never be null, as @Value and @ApolloJsonValue will not allow that
		Object value = placeholderHelper
				.resolvePropertyValue(beanFactory, springValue.getBeanName(), springValue.getPlaceholder());

		if (springValue.isJson()) {
			value = parseJsonValue((String) value, springValue.getTargetType());
		}
		else {
			if (springValue.isField()) {
				// org.springframework.beans.TypeConverter#convertIfNecessary(java.lang.Object, java.lang.Class, java.lang.reflect.Field) is available from Spring 3.2.0+
				if (typeConverterHasConvertIfNecessaryWithFieldParameter) {
					value = this.typeConverter
							.convertIfNecessary(value, springValue.getTargetType(), springValue.getField());
				}
				else {
					value = this.typeConverter.convertIfNecessary(value, springValue.getTargetType());
				}
			}
			else {
				value = this.typeConverter.convertIfNecessary(value, springValue.getTargetType(),
						springValue.getMethodParameter());
			}
		}

		return value;
	}

	private Object parseJsonValue(String json, Class<?> targetType) {
		try {
			return JacksonUtils.json2JavaBean(json, targetType);
		}
		catch (Throwable ex) {
			LOGGER.error("Parsing json '{}' to type {} failed!", json, targetType, ex);
			throw ex;
		}
	}

	private boolean testTypeConverterHasConvertIfNecessaryWithFieldParameter() {
		try {
			TypeConverter.class.getMethod("convertIfNecessary", Object.class, Class.class, Field.class);
		}
		catch (Throwable ex) {
			return false;
		}
		return true;
	}

}
