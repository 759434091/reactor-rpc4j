package com.github.hept59434091.reactor.rpc4j.fn.server.service;

import com.github.hept59434091.reactor.rpc4j.facade.TestRequest;
import com.github.hept59434091.reactor.rpc4j.facade.TestResponse;
import com.github.hept59434091.reactor.rpc4j.facade.TestService;

import reactor.core.publisher.Mono;

/**
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-04-17
 */
public class TestServiceImpl implements TestService {
    @Override
    public Mono<TestResponse> testMethod(Mono<TestRequest> testRequestMono) {
        return testRequestMono
                .map(testRequest -> TestResponse.builder()
                        .id(testRequest.getId())
                        .value("hello")
                        .build())
                .log("testMethod");
    }

    @Override
    public Mono<TestResponse> testMethod2(TestRequest testRequest) {
        return Mono.just(TestResponse.builder()
                .id(testRequest.getId())
                .value("hello")
                .build())
                .log("testMethod2");
    }
}
