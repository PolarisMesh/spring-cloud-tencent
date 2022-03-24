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

package com.tencent.cloud.polaris;

import javax.annotation.PostConstruct;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;

/**
 * Properties for Polaris.
 *
 * @author Haotian Zhang, Andrew Shan, Jie Cheng
 */
@ConfigurationProperties("spring.cloud.polaris.discovery")
public class PolarisProperties {

    /**
     * the polaris authentication token.
     */
    private String token;

    /**
     * namespace, separation registry of different environments.
     */
    @Value("${spring.cloud.polaris.discovery.namespace:#{'default'}}")
    private String namespace;

    /**
     * service name to registry.
     */
    @Value("${spring.cloud.polaris.discovery.service:${spring.application.name:}}")
    private String service;

    /**
     * 权重
     */
    @Value("${spring.cloud.polaris.discovery.weight:#{100}}")
    private float weight;

    /**
     * 版本号
     */
    private String version;

    /**
     * 协议名称
     */
    @Value("${spring.cloud.polaris.discovery.protocol:http}")
    private String protocol;

    /**
     * 使用spring cloud监听端口
     */
    @Value("${server.port:#{-1}}")
    private int port = -1;

    /**
     * 是否开启负载均衡
     */
    @Value("${spring.cloud.polaris.discovery.loadbalancer.enabled:#{true}}")
    private Boolean loadbalancerEnabled;

    /**
     * loadbalnce strategy
     */
    @Value("${spring.cloud.polaris.discovery.loadbalancer.policy:#{'weightedRandom'}}")
    private String policy;

    /**
     * loadbalnce strategy
     */
    @Value("${spring.cloud.polaris.discovery.register.enabled:#{true}}")
    private Boolean registerEnabled;

    /**
     * loadbalnce strategy
     */
    @Value("${spring.cloud.polaris.discovery.heartbeat.enabled:#{false}}")
    private Boolean heartbeatEnabled = true;

    /**
     * Custom health check url to override default
     */
    @Value("${spring.cloud.polaris.discovery.health-check-url:}")
    private String healthCheckUrl;

    @Autowired
    private Environment environment;

    /**
     * init properties
     *
     * @throws Exception
     */
    @PostConstruct
    public void init() throws Exception {
        if (StringUtils.isEmpty(this.getNamespace())) {
            this.setNamespace(environment.resolvePlaceholders("${spring.cloud.polaris.discovery.namespace:}"));
        }
        if (StringUtils.isEmpty(this.getService())) {
            this.setService(environment.resolvePlaceholders("${spring.cloud.polaris.discovery.service:}"));
        }
        if (StringUtils.isEmpty(this.getToken())) {
            this.setToken(environment.resolvePlaceholders("${spring.cloud.polaris.discovery.token:}"));
        }
    }

    public boolean isHeartbeatEnabled() {
        if (null == heartbeatEnabled) {
            return false;
        }
        return heartbeatEnabled;
    }

    public void setHeartbeatEnabled(Boolean heartbeatEnabled) {
        this.heartbeatEnabled = heartbeatEnabled;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }


    public boolean isRegisterEnabled() {
        return registerEnabled;
    }

    public void setRegisterEnabled(boolean registerEnabled) {
        this.registerEnabled = registerEnabled;
    }


    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public Boolean getLoadbalancerEnabled() {
        return loadbalancerEnabled;
    }

    public void setLoadbalancerEnabled(Boolean loadbalancerEnabled) {
        this.loadbalancerEnabled = loadbalancerEnabled;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHealthCheckUrl() {
        return healthCheckUrl;
    }

    public void setHealthCheckUrl(String healthCheckUrl) {
        this.healthCheckUrl = healthCheckUrl;
    }
    
    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "PolarisProperties{" +
                "token='" + token + '\'' +
                ", namespace='" + namespace + '\'' +
                ", service='" + service + '\'' +
                ", weight=" + weight +
                ", version='" + version + '\'' +
                ", protocol='" + protocol + '\'' +
                ", port=" + port +
                ", loadbalancerEnabled=" + loadbalancerEnabled +
                ", policy='" + policy + '\'' +
                ", registerEnabled=" + registerEnabled +
                ", heartbeatEnabled=" + heartbeatEnabled +
                ", healthCheckUrl=" + healthCheckUrl +
                ", environment=" + environment +
                '}';
    }
}
