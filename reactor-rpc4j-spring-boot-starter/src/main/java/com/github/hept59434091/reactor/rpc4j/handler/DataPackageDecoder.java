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

package com.github.hept59434091.reactor.rpc4j.handler;

import java.util.Arrays;
import java.util.List;

import com.github.hept59434091.reactor.rpc4j.pojo.RpcHeadMeta;
import com.github.hept59434091.reactor.rpc4j.pojo.RpcDataPackage;
import com.github.hept59434091.reactor.rpc4j.util.Rpc4jUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-04-09
 */
@Slf4j
public class DataPackageDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < RpcHeadMeta.SIZE) {
            return;
        }
        long rpcMessageDecoderStart = System.currentTimeMillis();
        in.markReaderIndex();

        // read head meta
        byte[] bytes = new byte[RpcHeadMeta.SIZE];
        in.readBytes(bytes, in.readerIndex(), RpcHeadMeta.SIZE);

        RpcHeadMeta headMeta = RpcHeadMeta.read(bytes);
        int bodySize = headMeta.getMessageSize();

        if (in.readableBytes() < bodySize) {
            in.resetReaderIndex();
            return;
        }

        // check magic code
        byte[] magicCode = headMeta.getMagicCode();
        if (!Rpc4jUtil.validateMagicCode(magicCode)) {
            throw new RuntimeException("Error magic code:" + Arrays.toString(magicCode));
        }

        // read package
        ByteBuf bodyBytes = in.readSlice(bodySize);

        RpcDataPackage rpcDataPackage = RpcDataPackage.read(headMeta, bodyBytes);

        if (log.isDebugEnabled()) {
            long rpcMessageDecoderEnd = System.currentTimeMillis();
            log.debug("[profiling] decode cost={}ms", rpcMessageDecoderEnd - rpcMessageDecoderStart);
        }
        out.add(rpcDataPackage);
    }
}
