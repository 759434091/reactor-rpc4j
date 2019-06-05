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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Optional;

import com.github.hept59434091.reactor.rpc4j.client.DiscoveryProperties;
import com.github.hept59434091.reactor.rpc4j.client.RpcClientInvocationHandler;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-04-15
 */
@Slf4j
public class RpcServiceClientRegister {
    public static <SERVICE> Mono<SERVICE> register(Class<SERVICE> clazz, String host, Integer port) {
        return RpcClientInvocationHandler
                .create(clazz, host, port)
                .map(handler -> Proxy.newProxyInstance(
                        clazz.getClassLoader(),
                        new Class[] {clazz},
                        handler))
                .cast(clazz);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static <SERVICE> SERVICE register(Class<SERVICE> clazz, DiscoveryProperties discoveryProperties) {
        Class handlerClass = Class.forName("com.github.hept59434091.reactor.discovery.RegisterRpcClientInvocationHandler");
        Constructor constructor = Optional
                .of(handlerClass)
                .map(Class::getConstructors)
                .filter(constructors -> constructors.length == 1)
                .map(constructors -> constructors[0])
                .orElseThrow(() -> new IllegalArgumentException("Cannot find the correct class RegisterRpcClientInvocationHandler"));
        Object handler = constructor.newInstance(clazz, discoveryProperties);

        return (SERVICE) Proxy.newProxyInstance(
                clazz.getClassLoader(),
                new Class[]{clazz},
                (InvocationHandler) handler
        );
    }
}
