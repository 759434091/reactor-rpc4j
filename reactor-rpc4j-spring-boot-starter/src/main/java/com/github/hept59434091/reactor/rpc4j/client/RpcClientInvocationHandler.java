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

package com.github.hept59434091.reactor.rpc4j.client;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.jctools.maps.NonBlockingHashMap;
import org.springframework.core.annotation.AnnotationUtils;

import com.baidu.bjf.remoting.protobuf.Codec;
import com.github.hept59434091.reactor.rpc4j.annotation.ProtoBufMethod;
import com.github.hept59434091.reactor.rpc4j.annotation.ProtoBufService;
import com.github.hept59434091.reactor.rpc4j.exc.RpcClientException;
import com.github.hept59434091.reactor.rpc4j.exc.RpcRequestException;
import com.github.hept59434091.reactor.rpc4j.exc.RpcResponseException;
import com.github.hept59434091.reactor.rpc4j.exc.RpcServiceException;
import com.github.hept59434091.reactor.rpc4j.handler.DataPackageDecoder;
import com.github.hept59434091.reactor.rpc4j.handler.HandlerName;
import com.github.hept59434091.reactor.rpc4j.pojo.ErrorCodes;
import com.github.hept59434091.reactor.rpc4j.pojo.RpcDataPackage;
import com.github.hept59434091.reactor.rpc4j.pojo.RpcHeadMeta;
import com.github.hept59434091.reactor.rpc4j.pojo.RpcMeta;
import com.github.hept59434091.reactor.rpc4j.pojo.RpcRequestMeta;
import com.github.hept59434091.reactor.rpc4j.pojo.RpcResponseMeta;
import com.github.hept59434091.reactor.rpc4j.util.Rpc4jUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.netty.Connection;
import reactor.netty.NettyPipeline;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpClient;

/**
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-04-15
 */
@Slf4j
public class RpcClientInvocationHandler<SERVICE> implements InvocationHandler, Disposable {
    private static final int MESSAGE_LENGTH_FIELD_START = 4;
    private static final int MESSAGE_LENGTH_FIELD_WIDTH = 4;
    private static final int ADJUST_SIZE = 4;

    @Getter
    private final String serviceName;
    @Getter
    private final Class<SERVICE> clazz;
    private final NonBlockingHashMap<String, AtomicLong> correlationIdMap;
    private final NonBlockingHashMap<String, MonoSink<RpcDataPackage>> responseMap;

    @Getter
    private final String host;
    @Getter
    private final Integer port;
    @Getter
    private final String hostPort;
    private final Scheduler timer;
    /**
     * 0: alone
     * 1: pooled
     */
    private final int flag;
    private final AtomicBoolean lock;

    private volatile Connection connection;

    @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
    static <SERVICE> Mono<RpcClientInvocationHandler<SERVICE>> create(Class<SERVICE> clazz,
                                                                      String host,
                                                                      Integer port,
                                                                      int flag) {
        return Mono
                .create(monoSink -> new RpcClientInvocationHandler<>(monoSink, clazz, host, port, flag));
    }

    public static <SERVICE> Mono<RpcClientInvocationHandler<SERVICE>> create(Class<SERVICE> clazz,
                                                                             String host,
                                                                             Integer port) {
        return create(clazz, host, port, 0);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (connection == null) {
            return Mono.error(new RpcClientException("Connection error"));
        }

        ProtoBufMethod protobufMethod = AnnotationUtils.findAnnotation(method, ProtoBufMethod.class);
        if (protobufMethod == null) {
            return method.invoke(this, args);
        }

        String methodName = Rpc4jUtil.getMethodName(protobufMethod, method);
        String signature = Rpc4jUtil.getMethodSignature(serviceName, methodName);

        if (!correlationIdMap.containsKey(signature)) {
            correlationIdMap.putIfAbsent(signature, new AtomicLong());
        }

        long correlationId = correlationIdMap.get(signature).incrementAndGet();
        String responseKey = signature + "$$" + correlationId;

        Codec reqCodec = Rpc4jUtil.getReqCodec(method, signature);
        Codec resCodec = Rpc4jUtil.getResCodec(method, signature);

        return Mono
                .<RpcDataPackage>create(monoSink -> Mono.just(args)
                        .filter(arr -> arr.length == 1)
                        .map(arr -> arr[0])
                        .flatMap(arg -> {
                            if (arg instanceof Mono) {
                                return (Mono<?>) arg;
                            }
                            return Mono.just(arg);
                        })
                        .<byte[]>handle((req, synchronousSink) -> {
                            try {
                                //noinspection unchecked
                                synchronousSink.next(reqCodec.encode(req));
                            } catch (IOException e) {
                                synchronousSink.error(new RpcRequestException(e));
                            }
                        })
                        .<ByteBuf>handle((data, synchronousSink) -> {
                            RpcRequestMeta rpcRequestMeta = new RpcRequestMeta();
                            rpcRequestMeta.setServiceName(serviceName);
                            rpcRequestMeta.setMethodName(methodName);

                            RpcMeta rpcMeta = RpcMeta.builder()
                                    .correlationId(correlationId)
                                    .compressType(0)
                                    .attachmentSize(0)
                                    .request(rpcRequestMeta)
                                    .build();

                            RpcHeadMeta rpcHeadMeta = RpcHeadMeta.builder()
                                    .magicCode(Rpc4jUtil.MAGIC_CODE)
                                    .build();

                            RpcDataPackage dataPackage = RpcDataPackage.builder()
                                    .rpcMeta(rpcMeta)
                                    .head(rpcHeadMeta)
                                    .data(data)
                                    .build();

                            try {
                                synchronousSink.next(RpcDataPackage.write(dataPackage));
                            } catch (IOException e) {
                                synchronousSink.error(new RpcRequestException(e));
                            }
                        })
                        .doOnSubscribe(subscription -> {
                            responseMap.put(responseKey, monoSink);
                            timer.schedule(() -> {
                                if (responseMap.containsKey(responseKey)) {
                                    var sink = responseMap.remove(responseKey);
                                    if (sink != null) {
                                        log.warn("sink timeout. key={}", responseKey);
                                        sink.error(new RpcClientException("Rpc timeout"));
                                    }
                                }
                            }, 1, TimeUnit.MINUTES);
                        })
                        .subscribe(data -> {
                            if (!connection.isDisposed()) {
                                connection.outbound().sendObject(data).then().subscribe();
                                return;
                            }

                            if (flag == 0) {
                                initConnection()
                                        .subscribe(conn -> {
                                            this.connection = conn;
                                            conn.outbound().sendObject(data).then().subscribe();
                                        });
                            } else {
                                responseMap.remove(responseKey);
                                monoSink.error(new RpcServiceException("connection disposed"));
                            }
                        }))
                .cast(RpcDataPackage.class)
                .handle((pkg, synchronousSink) -> {
                    try {
                        synchronousSink.next(resCodec.decode(pkg.getData()));
                    } catch (IOException e) {
                        synchronousSink.error(new RpcResponseException(e));
                    }
                });
    }

    @Override
    public void dispose() {
        connection.dispose();
        responseMap
                .forEach((s, monoSink) -> monoSink.error(new RpcClientException("Connection closed")));
    }

    @Override
    public boolean isDisposed() {
        return connection.isDisposed();
    }

    private Mono<? extends Connection> initConnection() {
        if (!lock.compareAndSet(false, true)) {
            return Mono
                    .create(monoSink -> timer.schedule(waitConnection(monoSink)));
        }

        return TcpClient.create()
                .host(host)
                .port(port)
                .runOn(LoopResources.create(host, 1, true))
                .option(ChannelOption.SO_KEEPALIVE, true)
                .doOnConnected(conn -> conn.addHandlerLast(
                        HandlerName.FRAME_DECODER, new LengthFieldBasedFrameDecoder(
                                Integer.MAX_VALUE,
                                MESSAGE_LENGTH_FIELD_START,
                                MESSAGE_LENGTH_FIELD_WIDTH,
                                ADJUST_SIZE,
                                0))
                        .addHandlerLast(HandlerName.PACKAGE_DECODER, new DataPackageDecoder())
                        .onReadIdle(60_1000, () -> {
                            if (flag == 1) {
                                log.error("readIdle timeout, disposing connection");
                                conn.dispose();
                            } else {
                                log.error("readIdle timeout, trying re-init");
                                initConnection();
                            }
                        })
                        .onWriteIdle(60_1000, () -> {
                            if (flag == 1) {
                                log.error("writeIdle timeout, disposing connection");
                                conn.dispose();
                            } else {
                                log.error("writeIdle timeout, trying re-init");
                                initConnection();
                            }
                        })
                        .outbound().options(NettyPipeline.SendOptions::flushOnEach))
                .handle((nettyInbound, nettyOutbound) -> nettyInbound
                        .receiveObject()
                        .map(RpcDataPackage.class::cast)
                        .doOnNext(pkg -> {
                            RpcMeta rpcMeta = pkg.getRpcMeta();
                            RpcRequestMeta rpcRequestMeta = rpcMeta.getRequest();

                            String signature = Rpc4jUtil.getMethodSignature(
                                    rpcRequestMeta.getServiceName(),
                                    rpcRequestMeta.getMethodName());

                            String responseKey = signature + "$$" + rpcMeta.getCorrelationId();

                            MonoSink<RpcDataPackage> sink = responseMap.remove(responseKey);
                            if (sink == null) {
                                log.warn("receive error responseKey={}", responseKey);
                                return;
                            }

                            RpcResponseMeta responseMeta = Optional
                                    .of(pkg)
                                    .map(RpcDataPackage::getRpcMeta)
                                    .map(RpcMeta::getResponse)
                                    .orElse(null);
                            if (responseMeta == null) {
                                sink.error(new RpcResponseException("Invalid package"));
                                return;
                            }
                            if (ErrorCodes.ST_SUCCESS != responseMeta.getErrorCode()) {
                                sink.error(new RpcResponseException(MessageFormat.format(
                                        "code={0}, message={1}",
                                        responseMeta.getErrorCode(),
                                        responseMeta.getErrorText())));
                                return;
                            }

                            sink.success(pkg);
                        })
                        .then())
                .connect()
                .doFinally(signalType -> lock.set(false));
    }

    private Runnable waitConnection(MonoSink<Connection> monoSink) {
        return () -> {
            if (!lock.get()) {
                monoSink.success(connection);
                return;
            }

            timer.schedule(waitConnection(monoSink));
        };
    }

    private RpcClientInvocationHandler(MonoSink<RpcClientInvocationHandler<SERVICE>> monoSink,
                                       Class<SERVICE> clazz,
                                       String host,
                                       Integer port,
                                       int flag) {
        this.clazz = clazz;
        this.correlationIdMap = new NonBlockingHashMap<>();
        this.responseMap = new NonBlockingHashMap<>();
        this.host = host;
        this.port = port;
        this.hostPort = host + ":" + port;
        this.timer = Schedulers.newSingle("timer");
        this.flag = flag;
        this.lock = new AtomicBoolean(false);

        String instanceName = Rpc4jUtil.getInstanceName(clazz);
        ProtoBufService protoBufService = Optional
                .ofNullable(AnnotationUtils.findAnnotation(clazz, ProtoBufService.class))
                .orElseThrow(() -> new IllegalArgumentException("Illegal class " + clazz.getSimpleName()));

        this.serviceName = Rpc4jUtil.getServiceName(protoBufService, instanceName);

        initConnection()
                .subscribe(connection -> {
                    this.connection = connection;
                    monoSink.success(this);
                });
    }

}
