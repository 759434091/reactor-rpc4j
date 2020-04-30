package com.github.hept59434091.reactor.rpc4j.fn.client;

import com.alibaba.fastjson.JSON;
import com.github.hept59434091.reactor.rpc4j.RpcServiceClientRegister;
import com.github.hept59434091.reactor.rpc4j.facade.TestService;
import com.github.hept59434091.reactor.rpc4j.facade.TestRequest;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

import reactor.core.publisher.Mono;

import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-04-17
 */
@Slf4j
public class ClientApplication {
    private TestService testService = RpcServiceClientRegister.register(
            TestService.class,
            "127.0.0.1",
            8000
    ).block();

    @Test
    public void testMethod() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        TestRequest testRequest = TestRequest.builder()
                .id(1)
                .build();

        testService.testMethod(Mono.just(testRequest))
                .subscribe(testResponse -> {
                    log.info(JSON.toJSONString(testResponse));
                    countDownLatch.countDown();
                });

        countDownLatch.await();
    }

    @Test
    public void testMethod2() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        TestRequest testRequest = TestRequest.builder()
                .id(1)
                .build();

        testService.testMethod2(testRequest)
                .log("testMethod2")
                .subscribe(testResponse -> {
                    log.info(JSON.toJSONString(testResponse));
                    countDownLatch.countDown();
                });

        countDownLatch.await();
    }
}
