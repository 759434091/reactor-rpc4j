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

import java.io.IOException;

import org.reactivestreams.Publisher;
import org.springframework.beans.factory.InitializingBean;

import com.github.hept59434091.reactor.rpc4j.exc.RpcRequestException;
import com.github.hept59434091.reactor.rpc4j.exc.RpcResponseException;
import com.github.hept59434091.reactor.rpc4j.exc.RpcServiceException;
import com.github.hept59434091.reactor.rpc4j.handler.DataPackageDecoder;
import com.github.hept59434091.reactor.rpc4j.handler.DataPackageUnCompressHandler;
import com.github.hept59434091.reactor.rpc4j.handler.DefaultIdleHandler;
import com.github.hept59434091.reactor.rpc4j.handler.HandlerName;
import com.github.hept59434091.reactor.rpc4j.pojo.ErrorCodes;
import com.github.hept59434091.reactor.rpc4j.pojo.RpcDataPackage;
import com.github.hept59434091.reactor.rpc4j.pojo.RpcResponseMeta;
import com.github.hept59434091.reactor.rpc4j.pojo.RpcServiceRoute;
import com.github.hept59434091.reactor.rpc4j.pojo.RpcServiceWrapper;
import com.github.hept59434091.reactor.rpc4j.util.Rpc4jUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.Connection;
import reactor.netty.DisposableServer;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;
import reactor.netty.NettyPipeline;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpServer;

/**
 * RpcServer
 *
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-04-08
 */
@Slf4j
@Data
public class RpcServer implements InitializingBean {
    private final int messageLengthFieldStart = 4;
    private final int messageLengthFieldWidth = 4;
    private final int adjustSize = 4;
    private final DataPackageUnCompressHandler dataPackageUnCompressHandler = new DataPackageUnCompressHandler();

    private final RpcServerProperties serverProperties;
    private final RpcServiceRoute rpcServiceRoute;
    private final TcpServer server;

    public RpcServer(RpcServerProperties serverProperties, RpcServiceRoute rpcServiceRoute) {
        this.serverProperties = serverProperties;
        this.rpcServiceRoute = rpcServiceRoute;

        LoopResources loopResources = LoopResources.create("loop");

        server = TcpServer.create()
                .doOnBind(bootstrap -> bootstrap.option(ChannelOption.SO_BACKLOG, serverProperties.getBacklog()))
                .port(serverProperties.getPort())
                .runOn(loopResources)
                .option(ChannelOption.SO_KEEPALIVE, serverProperties.isKeepAlive())
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.TCP_NODELAY, serverProperties.isTcpNoDelay())
                .option(ChannelOption.SO_LINGER, serverProperties.getSoLinger())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, serverProperties.getConnectTimeout())
                .option(ChannelOption.SO_RCVBUF, serverProperties.getReceiveBufferSize())
                .option(ChannelOption.SO_SNDBUF, serverProperties.getSendBufferSize())
                .doOnConnection(this::initConnection)
                .handle(this::handler);
    }

    private void initConnection(Connection connection) {
        connection
                .onReadIdle(serverProperties.getReaderIdleTime(),
                        DefaultIdleHandler.getHandler(connection))
                .onWriteIdle(serverProperties.getWriterIdleTime(),
                        DefaultIdleHandler.getHandler(connection))
                .addHandlerLast(HandlerName.FRAME_DECODER, new LengthFieldBasedFrameDecoder(
                        serverProperties.getMaxSize(),
                        messageLengthFieldStart,
                        messageLengthFieldWidth,
                        adjustSize,
                        0)
                )
                .addHandlerLast(HandlerName.PACKAGE_DECODER, new DataPackageDecoder())
                .addHandlerLast(HandlerName.UNCOMPRESS_DECODER, dataPackageUnCompressHandler)
                .outbound()
                .options(NettyPipeline.SendOptions::flushOnEach);
    }

    @SuppressWarnings("unchecked")
    private Publisher<Void> handler(NettyInbound nettyInbound, NettyOutbound nettyOutbound) {
        return nettyInbound
                .receiveObject()
                .cast(RpcDataPackage.class)
                .flatMap(pkg -> {
                    String signature = Rpc4jUtil.getMethodSignature(pkg.getRpcMeta().getRequest());

                    RpcServiceWrapper rpcServiceWrapper = rpcServiceRoute.get(signature);
                    if (rpcServiceWrapper == null) {
                        return Mono.error(new RpcRequestException("service not found. " + signature));
                    }

                    RpcHandlerFunction<Object, Object> rpcHandlerFunction = rpcServiceWrapper.getRpcHandlerFunction();

                    return Mono.just(pkg)
                            .map(RpcDataPackage::getData)
                            .handle((data, synchronousSink) -> {
                                try {
                                    synchronousSink.next(rpcServiceWrapper.getRequestCodec().decode(data));
                                } catch (IOException e) {
                                    synchronousSink.error(new RpcRequestException(e));
                                }
                            })
                            .publish(rpcHandlerFunction::handle)
                            .onErrorMap(
                                    e -> !(e instanceof RpcRequestException),
                                    RpcServiceException::new
                            )
                            .<ByteBuf>handle((resp, synchronousSink) -> {
                                try {
                                    RpcResponseMeta rpcResponseMeta = new RpcResponseMeta();
                                    rpcResponseMeta.setErrorCode(ErrorCodes.ST_SUCCESS);

                                    byte[] data = rpcServiceWrapper.getResponseCodec().encode(resp);

                                    pkg.setData(data);
                                    pkg.getRpcMeta().setResponse(rpcResponseMeta);

                                    synchronousSink.next(RpcDataPackage.write(pkg));
                                } catch (IOException e) {
                                    synchronousSink.error(new RpcResponseException(e));
                                }
                            })
                            .onErrorResume(
                                    e -> e instanceof RpcRequestException
                                            || e instanceof RpcServiceException,
                                    e -> {
                                        RpcResponseMeta rpcResponseMeta = new RpcResponseMeta();
                                        rpcResponseMeta.setErrorCode(ErrorCodes.ST_ERROR);
                                        rpcResponseMeta.setErrorText(e.getMessage());

                                        pkg.getRpcMeta().setResponse(rpcResponseMeta);

                                        return Mono
                                                .just(pkg)
                                                .handle((dataPackage, synchronousSink) -> {
                                                    try {
                                                        synchronousSink.next(RpcDataPackage.write(dataPackage));
                                                    } catch (IOException e1) {
                                                        synchronousSink.error(new RpcResponseException(e));
                                                    }
                                                });
                                    }
                            );
                })
                .publish(nettyOutbound::send)
                .doOnError(e -> log.error("conn catch error.", e));
    }

    @Override
    public void afterPropertiesSet() {
        DisposableServer disposableServer = server.bindNow();

        log.info("reactive rpc4j start at port {}", serverProperties.getPort());
        Schedulers
                .newSingle(r -> {
                    Thread thread = new Thread(r);
                    thread.setDaemon(false);
                    thread.setName("server");
                    thread.setContextClassLoader(getClass().getClassLoader());
                    return thread;
                })
                .schedule(() -> disposableServer.onDispose().block());
    }
}
