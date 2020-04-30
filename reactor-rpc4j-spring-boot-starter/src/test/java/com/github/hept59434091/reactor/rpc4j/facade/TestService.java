package com.github.hept59434091.reactor.rpc4j.facade;

import com.github.hept59434091.reactor.rpc4j.annotation.ProtoBufMethod;
import com.github.hept59434091.reactor.rpc4j.annotation.ProtoBufService;
import reactor.core.publisher.Mono;

/**
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-04-17
 */
@ProtoBufService("testService")
public interface TestService {
    @ProtoBufMethod
    Mono<TestResponse> testMethod(Mono<TestRequest> testRequestMono);

    @ProtoBufMethod
    Mono<TestResponse> testMethod2(TestRequest testRequest);
}
