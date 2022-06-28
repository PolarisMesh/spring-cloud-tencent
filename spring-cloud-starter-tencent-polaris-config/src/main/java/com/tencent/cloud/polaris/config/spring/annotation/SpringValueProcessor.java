package com.tencent.cloud.polaris.config.spring.annotation;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.spring.property.PlaceholderHelper;
import com.tencent.cloud.polaris.config.spring.property.SpringValue;
import com.tencent.cloud.polaris.config.spring.property.SpringValueDefinition;
import com.tencent.cloud.polaris.config.spring.property.SpringValueDefinitionProcessor;
import com.tencent.cloud.polaris.config.spring.property.SpringValueRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Bean;

/**
 *@author : wh
 *@date : 2022/6/28 09:19
 *@description:
 */
public class SpringValueProcessor extends AbstractPolarisProcessor implements BeanFactoryPostProcessor, BeanFactoryAware {

	private static final Logger logger = LoggerFactory.getLogger(SpringValueProcessor.class);

	private final PlaceholderHelper placeholderHelper;
	private final SpringValueRegistry springValueRegistry;

	private final PolarisConfigProperties polarisConfigProperties;

	private BeanFactory beanFactory;
	private Multimap<String, SpringValueDefinition> beanName2SpringValueDefinitions;

	public SpringValueProcessor(PlaceholderHelper placeholderHelper, SpringValueRegistry springValueRegistry, PolarisConfigProperties polarisConfigProperties) {
		this.placeholderHelper = placeholderHelper;
		this.springValueRegistry = springValueRegistry;
		beanName2SpringValueDefinitions = LinkedListMultimap.create();
		this.polarisConfigProperties = polarisConfigProperties;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
			throws BeansException {
		if (polarisConfigProperties.isAutoRefresh() && beanFactory instanceof BeanDefinitionRegistry) {
			beanName2SpringValueDefinitions = SpringValueDefinitionProcessor
					.getBeanName2SpringValueDefinitions((BeanDefinitionRegistry) beanFactory);
		}
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		if (polarisConfigProperties.isAutoRefresh()) {
			super.postProcessBeforeInitialization(bean, beanName);
			processBeanPropertyValues(bean, beanName);
		}
		return bean;
	}


	@Override
	protected void processField(Object bean, String beanName, Field field) {
		// register @Value on field
		Value value = field.getAnnotation(Value.class);
		if (value == null) {
			return;
		}

		doRegister(bean, beanName, field, value);
	}

	@Override
	protected void processMethod(Object bean, String beanName, Method method) {
		//register @Value on method
		Value value = method.getAnnotation(Value.class);
		if (value == null) {
			return;
		}
		//skip Configuration bean methods
		if (method.getAnnotation(Bean.class) != null) {
			return;
		}
		if (method.getParameterTypes().length != 1) {
			logger.error("Ignore @Value setter {}.{}, expecting 1 parameter, actual {} parameters",
					bean.getClass().getName(), method.getName(), method.getParameterTypes().length);
			return;
		}

		doRegister(bean, beanName, method, value);
	}

	private void doRegister(Object bean, String beanName, Member member, Value value) {
		Set<String> keys = placeholderHelper.extractPlaceholderKeys(value.value());
		if (keys.isEmpty()) {
			return;
		}

		for (String key : keys) {
			SpringValue springValue;
			if (member instanceof Field) {
				Field field = (Field) member;
				springValue = new SpringValue(key, value.value(), bean, beanName, field, false);
			}
			else if (member instanceof Method) {
				Method method = (Method) member;
				springValue = new SpringValue(key, value.value(), bean, beanName, method, false);
			}
			else {
				logger.error("polaris @Value annotation currently only support to be used on methods and fields, "
						+ "but is used on {}", member.getClass());
				return;
			}
			springValueRegistry.register(beanFactory, key, springValue);
			logger.info("Monitoring {}", springValue);
		}
	}

	private void processBeanPropertyValues(Object bean, String beanName) {
		Collection<SpringValueDefinition> propertySpringValues = beanName2SpringValueDefinitions
				.get(beanName);
		if (propertySpringValues == null || propertySpringValues.isEmpty()) {
			return;
		}

		for (SpringValueDefinition definition : propertySpringValues) {
			try {
				PropertyDescriptor pd = BeanUtils
						.getPropertyDescriptor(bean.getClass(), definition.getPropertyName());
				Method method = pd.getWriteMethod();
				if (method == null) {
					continue;
				}
				SpringValue springValue = new SpringValue(definition.getKey(), definition.getPlaceholder(),
						bean, beanName, method, false);
				springValueRegistry.register(beanFactory, definition.getKey(), springValue);
				logger.debug("Monitoring {}", springValue);
			}
			catch (Throwable ex) {
				logger.error("Failed to enable auto update feature for {}.{}", bean.getClass(),
						definition.getPropertyName());
			}
		}

		// clear
		beanName2SpringValueDefinitions.removeAll(beanName);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}
}
