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

import lombok.Data;

/**
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-04-17
 */
@Data
public class RpcServerProperties {
    private boolean keepAlive = true;
    private boolean tcpNoDelay = true;
    private int keepAliveTime = 60;
    private int soLinger = 5;
    private int backlog = 100;
    private int connectTimeout;
    private int receiveBufferSize = 1024 * 64;
    private int sendBufferSize = 1024 * 64;
    private int readerIdleTime = 60 * 1000;
    private int writerIdleTime = 60 * 1000;
    private int maxSize = Integer.MAX_VALUE;
    private int port = 8000;
}
