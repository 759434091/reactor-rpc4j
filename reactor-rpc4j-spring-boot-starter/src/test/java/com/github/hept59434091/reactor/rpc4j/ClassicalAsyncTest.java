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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

/**
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-04-18
 */
@Slf4j
public class ClassicalAsyncTest {
    private Executor executor = new ThreadPoolExecutor(
            1,
            1,
            10L,
            TimeUnit.SECONDS,
            new LinkedBlockingDeque<>());

    @Test
    public void callbackTest() throws InterruptedException, ExecutionException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Callback<String, Void> callback = param -> {
            log.info("task end. {}", param);
            countDownLatch.countDown();
            return null;
        };

        FutureTask<Void> futureTask = new FutureTask<>(() -> {
            try {
                Thread.sleep(2 * 1000);
                log.info("doing something");
                callback.call("done");
                return null;
            } finally {
                countDownLatch.countDown();
            }
        });

        executor.execute(futureTask);

        log.info("task is done? {}", futureTask.isDone());
        countDownLatch.await();
        log.info("task is done? {}", futureTask.isDone());
        log.info("task end. {}", futureTask.get());
    }

    @Test
    public void futureTest() throws InterruptedException, ExecutionException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        FutureTask<String> futureTask = new FutureTask<>(() -> {
            try {
                Thread.sleep(2 * 1000);
                log.info("doing something");
                return "done";
            } finally {
                countDownLatch.countDown();
            }
        });
        executor.execute(futureTask);

        log.info("task is done? {}", futureTask.isDone());
        countDownLatch.await();
        log.info("task is done? {}", futureTask.isDone());
        log.info("task end. {}", futureTask.get());
    }

    @Test
    public void completableFutureTest() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        CompletableFuture completableFuture = CompletableFuture
                .supplyAsync(() -> {
                    try {
                        Thread.sleep(2 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    log.info("doing something");
                    return "done";
                }, executor)
                .thenAccept(str -> {
                    log.info("task end. {}", str);
                    countDownLatch.countDown();
                });

        log.info("task is done? {}", completableFuture.isDone());
        countDownLatch.await();
        log.info("task is done? {}", completableFuture.isDone());
    }

    @Test
    public void reactorTest() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Disposable disposable = Mono
                .fromSupplier(() -> {
                    try {
                        Thread.sleep(2 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    log.info("doing something");
                    return "done";
                })
                .cast(String.class)
                .subscribe(str -> {
                    log.info("task end. {}", str);
                    countDownLatch.countDown();
                });

        log.info("task is done? {}", disposable.isDisposed());
        countDownLatch.await();
        log.info("task is done? {}", disposable.isDisposed());
    }

    @Test
    public void monoUsingTest() {
        Mono
                .using(
                        () -> Mono.just(1),
                        param -> Mono.just(String.valueOf(param)),
                        integer -> log.info("cleanup"),
                        true
                )
                .subscribe(log::info);
    }
}
