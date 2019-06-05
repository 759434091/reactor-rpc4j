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

import java.io.IOException;

import com.baidu.bjf.remoting.protobuf.Codec;
import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.ProtobufProxy;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-04-09
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RpcMeta {
    /**
     * The Constant COMPRESS_NO.
     */
    public static final int COMPRESS_NO = 0;

    /**
     * The Constant COMPRESS_SNAPPY.
     */
    public static final int COMPRESS_SNAPPY = 1;

    /**
     * The Constant COMPRESS_GZIP.
     */
    public static final int COMPRESS_GZIP = 2;

    /**
     * Decode and encode handler.
     */
    public static final Codec<RpcMeta> CODEC = ProtobufProxy.create(RpcMeta.class);

    /**
     * 请求包元数据.
     */
    @Protobuf(fieldType = FieldType.OBJECT, order = 1)
    private RpcRequestMeta request;

    /**
     * 响应包元数据.
     */
    @Protobuf(fieldType = FieldType.OBJECT, order = 2)
    private RpcResponseMeta response;

    /**
     * 0 不压缩 1 使用Snappy 1.0.5 2 使用gzip
     */
    @Protobuf(order = 3)
    private Integer compressType;

    /**
     * 请求包中的该域由请求方设置，用于唯一标识一个RPC请求。<br>
     * 请求方有义务保证其唯一性，协议本身对此不做任何检查。 <br>
     * 响应方需要在对应的响应包里面将correlation_id设为同样的值。.
     */
    @Protobuf(order = 4)
    private Long correlationId;

    /**
     * 附件大小.
     */
    @Protobuf(order = 5)
    private Integer attachmentSize;

    /**
     * Chunk模式本质上是将一个大的数据流拆分成一个个小的Chunk包按序进行发送。如何拆分还原由通信双方确定.
     */
    @Protobuf(order = 6)
    private ChunkInfo chunkInfo;

    /**
     * 用于存放身份认证相关信息.
     */
    @Protobuf(fieldType = FieldType.BYTES, order = 7)
    private byte[] authenticationData;

    public static RpcMeta read(ByteBuf byteBuf) {
        try {
            int readableBytes = byteBuf.readableBytes();
            byte[] bytes = new byte[readableBytes];
            byteBuf.readBytes(bytes);
            return CODEC.decode(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
