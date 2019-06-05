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

package com.github.hept59434091.reactor.rpc4j.server;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

import com.github.hept59434091.reactor.rpc4j.annotation.ProtoBufMethod;
import com.github.hept59434091.reactor.rpc4j.annotation.ProtoBufService;
import com.github.hept59434091.reactor.rpc4j.pojo.RpcServiceRoute;
import com.github.hept59434091.reactor.rpc4j.pojo.RpcServiceWrapper;
import com.github.hept59434091.reactor.rpc4j.service.PingService;
import com.github.hept59434091.reactor.rpc4j.util.Rpc4jUtil;

import lombok.extern.slf4j.Slf4j;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

/**
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-04-12
 */
@Slf4j
public class RpcServiceExporter extends ApplicationObjectSupport {
    @Bean
    PingService pingService() {
        return new PingService() {
        };
    }

    @SuppressWarnings("unchecked")
    @Bean
    @ConditionalOnMissingBean(RpcServiceRoute.class)
    RpcServiceRoute rpcServiceRoute() throws BeansException {
        ListableBeanFactory applicationContext = getApplicationContext();
        Assert.notNull(applicationContext, "applicationContext must not be null");

        Map<String, Object> beanMap = applicationContext.getBeansWithAnnotation(ProtoBufService.class);

        RpcServiceRoute rpcServiceRoute = new RpcServiceRoute();

        beanMap.forEach((beanName, beanObject) -> {
            ProtoBufService protoBufService = applicationContext.findAnnotationOnBean(beanName, ProtoBufService.class);
            Assert.notNull(protoBufService, "annotation ProtoBufService must not be null");

            Optional.of(beanObject)
                    .map(Object::getClass)
                    .map(Class::getMethods)
                    .map(Arrays::stream)
                    .get()
                    .forEach(method -> {
                        ProtoBufMethod protobufMethod = AnnotationUtils.findAnnotation(method, ProtoBufMethod.class);
                        if (protobufMethod == null) {
                            return;
                        }

                        String serviceName = Rpc4jUtil.getServiceName(protoBufService, beanName);
                        String methodName = Rpc4jUtil.getMethodName(protobufMethod, method);
                        String signature = Rpc4jUtil.getMethodSignature(serviceName, methodName);

                        RpcHandlerFunction rpcHandlerFunction;
                        if (method.getGenericParameterTypes()[0] instanceof ParameterizedTypeImpl) {
                            rpcHandlerFunction = mono -> {
                                try {
                                    return (Mono<?>) method.invoke(beanObject, mono);
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    throw Exceptions.propagate(e);
                                }
                            };
                        } else {
                            rpcHandlerFunction = mono ->
                                    mono.flatMap(req -> {
                                        try {
                                            return (Mono<?>) method.invoke(beanObject, req);
                                        } catch (IllegalAccessException | InvocationTargetException e) {
                                            throw Exceptions.propagate(e);
                                        }
                                    });
                        }

                        RpcServiceWrapper rpcServiceWrapper = RpcServiceWrapper.builder()
                                .requestCodec(Rpc4jUtil.getReqCodec(method, signature))
                                .responseCodec(Rpc4jUtil.getResCodec(method, signature))
                                .rpcHandlerFunction(rpcHandlerFunction)
                                .build();
                        rpcServiceRoute.put(signature, rpcServiceWrapper);
                    });
        });

        return rpcServiceRoute;
    }

    @Override
    protected void initApplicationContext() throws BeansException {
        super.initApplicationContext();
        rpcServiceRoute();
    }
}
