/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.hept59434091.reactor.rpc4j;

import com.github.hept59434091.reactor.rpc4j.pojo.RpcServiceRoute;
import com.github.hept59434091.reactor.rpc4j.server.RpcServer;
import com.github.hept59434091.reactor.rpc4j.server.RpcServerProperties;
import com.github.hept59434091.reactor.rpc4j.server.RpcServiceExporter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;

/**
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-04-12
 */
@Configuration
@EnableConfigurationProperties
@Import(RpcServiceExporter.class)
@ConditionalOnProperty(prefix = "reactor-rpc4j.server", name = "enabled", havingValue = "true")
public class ReactorRpcAutoConfiguration {
    @Resource
    private RpcServiceRoute rpcServiceRoute;

    @Bean
    @ConfigurationProperties(prefix = "reactor-rpc4j.server")
    RpcServerProperties rpcServerProperties() {
        return new RpcServerProperties();
    }

    @Bean
    RpcServer rpcServer(@Autowired RpcServerProperties rpcServerProperties) {
        return new RpcServer(rpcServerProperties, rpcServiceRoute);
    }
}
