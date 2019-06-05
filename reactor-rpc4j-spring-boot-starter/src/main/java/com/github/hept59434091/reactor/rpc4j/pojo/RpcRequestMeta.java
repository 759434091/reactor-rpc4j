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
import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.ProtobufProxy;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;

import lombok.Data;

/**
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-04-10
 */
@Data
public class RpcRequestMeta {
    /**
     * default encode and decode handler.
     */
    private static final Codec<RpcRequestMeta>
            CODEC = ProtobufProxy.create(RpcRequestMeta.class);

    /**
     * 服务名.
     */
    @Protobuf(required = true, order = 1)
    private String serviceName;

    /**
     * 方法名.
     */
    @Protobuf(required = true, order = 2)
    private String methodName;

    /**
     * 用于打印日志。可用于存放BFE_LOG_ID。该参数可选。.
     */
    @Protobuf(order = 3)
    private Long logId;

    /**
     * 非PbRpc规范，用于传输额外的参数.
     */
    @Protobuf(fieldType = FieldType.BYTES, order = 4)
    private byte[] extraParam;
}
