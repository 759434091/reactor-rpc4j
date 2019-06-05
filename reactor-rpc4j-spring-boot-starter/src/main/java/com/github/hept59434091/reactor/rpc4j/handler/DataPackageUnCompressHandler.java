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

import java.util.List;

import com.github.hept59434091.reactor.rpc4j.compress.Compress;
import com.github.hept59434091.reactor.rpc4j.compress.GZipCompress;
import com.github.hept59434091.reactor.rpc4j.exc.RpcRequestException;
import com.github.hept59434091.reactor.rpc4j.pojo.ErrorCodes;
import com.github.hept59434091.reactor.rpc4j.pojo.RpcDataPackage;
import com.github.hept59434091.reactor.rpc4j.pojo.RpcMeta;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

/**
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-04-09
 */
@ChannelHandler.Sharable
public class DataPackageUnCompressHandler extends MessageToMessageDecoder<RpcDataPackage> {

    @Override
    protected void decode(ChannelHandlerContext ctx, RpcDataPackage msg, List<Object> out) {
        try {
            // check if do compress
            Integer compressType = msg.getRpcMeta().getCompressType();
            Compress compress = null;
            if (compressType == RpcMeta.COMPRESS_GZIP) {
                compress = new GZipCompress();
            } else if (compressType != 0) {
                throw new RpcRequestException("invalid compress Type");
            }

            if (compress != null) {
                byte[] data = msg.getData();
                data = compress.unCompress(data);
                msg.setData(data);
            }
        } catch (Exception e) {
            msg.setErrorCode(ErrorCodes.ST_ERROR_COMPRESS);
            msg.setErrorText("Data uncompress failed due to " + e.getMessage());
        }
        out.add(msg);
    }

}
