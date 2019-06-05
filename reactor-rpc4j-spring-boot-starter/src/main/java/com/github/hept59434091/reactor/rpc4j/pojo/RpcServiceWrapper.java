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

package com.github.hept59434091.reactor.rpc4j.pojo;

import com.baidu.bjf.remoting.protobuf.Codec;
import com.github.hept59434091.reactor.rpc4j.server.RpcHandlerFunction;

import lombok.Builder;
import lombok.Data;

/**
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-04-12
 */
@Data
@Builder
public class RpcServiceWrapper {
    private RpcHandlerFunction rpcHandlerFunction;
    private Codec requestCodec;
    private Codec responseCodec;
}
