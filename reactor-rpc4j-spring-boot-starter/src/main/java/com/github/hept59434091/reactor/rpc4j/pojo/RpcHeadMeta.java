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

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

/**
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-04-09
 */
@Data
@Builder
@Getter
public class RpcHeadMeta {
    /**
     * RPC meta head size.
     */
    public static final int SIZE = 12;

    public static final int MAGIC_CODE_LENGTH = 4;

    /**
     * 协议标识.
     */
    private byte[] magicCode;

    /**
     * message body size include.
     */
    private int messageSize;

    /**
     * RPC meta size.
     */
    private int metaSize;

    public static RpcHeadMeta read(byte[] bytes) {
        if (bytes == null || bytes.length != SIZE) {
            throw new IllegalArgumentException("invalid byte array. size must be " + SIZE);
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byte[] magicCode = new byte[4];
        byteBuffer.get(magicCode);

        RpcHeadMeta result = builder()
                .magicCode(magicCode)
                .messageSize(byteBuffer.getInt())
                .metaSize(byteBuffer.getInt())
                .build();

        byteBuffer.clear();
        return result;
    }

    public static ByteBuf write(RpcHeadMeta rpcHeadMeta) {
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.heapBuffer(SIZE);
        byteBuf.writeBytes(rpcHeadMeta.magicCode);
        byteBuf.writeInt(rpcHeadMeta.messageSize);
        byteBuf.writeInt(rpcHeadMeta.metaSize);
        return byteBuf;
    }
}
