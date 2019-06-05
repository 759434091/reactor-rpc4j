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
import com.baidu.bjf.remoting.protobuf.ProtobufProxy;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;

import lombok.Data;

/**
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-04-09
 */
@Data
public class RpcResponseMeta {
    /**
     * Decode and encode hand;er.
     */
    private static final Codec<RpcResponseMeta> CODEC = ProtobufProxy.create(RpcResponseMeta.class);

    /**
     * 发生错误时的错误号，0表示正常，非0表示错误。具体含义由应用方自行定义。.
     */
    @Protobuf(order = 1)
    private Integer errorCode;

    /**
     * 错误的文本描述.
     */
    @Protobuf(order = 2)
    private String errorText;
}
