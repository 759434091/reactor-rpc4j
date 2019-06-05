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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import lombok.Builder;
import lombok.Data;

/**
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-04-09
 */
@Builder
@Data
public class RpcDataPackage {
    /**
     * The head.
     */
    private RpcHeadMeta head;

    /**
     * The rpc meta.
     */
    private RpcMeta rpcMeta;

    /**
     * The data.
     */
    private byte[] data;

    /**
     * The attachment.
     */
    private byte[] attachment;

    /**
     * The time stamp.
     */
    private long timeStamp;

    public static ByteBuf write(RpcDataPackage dataPackage) throws IOException {
        if (dataPackage.head == null) {
            throw new RuntimeException("property 'head' is null.");
        }
        if (dataPackage.rpcMeta == null) {
            throw new RuntimeException("property 'rpcMeta' is null.");
        }

        int totalSize = 0;

        // set dataSize
        int dataSize;
        if (dataPackage.data != null) {
            dataSize = dataPackage.data.length;
            totalSize += dataSize;
        }

        // set attachment size
        int attachmentSize = 0;
        if (dataPackage.attachment != null) {
            attachmentSize = dataPackage.attachment.length;
            totalSize += attachmentSize;
        }
        dataPackage.rpcMeta.setAttachmentSize(attachmentSize);

        // get RPC meta data
        byte[] rpcMetaBytes = RpcMeta.CODEC.encode(dataPackage.rpcMeta);
        int rpcMetaSize = rpcMetaBytes.length;
        totalSize += rpcMetaSize;

        dataPackage.head.setMetaSize(rpcMetaSize);
        dataPackage.head.setMessageSize(totalSize);

        // total size should add head size
        totalSize = totalSize + RpcHeadMeta.SIZE;

        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.heapBuffer(totalSize);
        ByteBuf headMetaBuf = RpcHeadMeta.write(dataPackage.head);
        byteBuf.writeBytes(headMetaBuf);
        ReferenceCountUtil.release(headMetaBuf);
        byteBuf.writeBytes(rpcMetaBytes);
        if (dataPackage.data != null) {
            byteBuf.writeBytes(dataPackage.data);
        }
        if (dataPackage.attachment != null) {
            byteBuf.writeBytes(dataPackage.attachment);
        }

        return byteBuf;
    }

    public static RpcDataPackage read(RpcHeadMeta headMeta, ByteBuf bodyBuf) {
        if (bodyBuf == null) {
            throw new IllegalArgumentException("param 'bytes' is null.");
        }

        int metaSize = headMeta.getMetaSize();
        ByteBuf metaBytes = bodyBuf.readSlice(metaSize);

        RpcMeta rpcMeta = RpcMeta.read(metaBytes);

        int attachmentSize = rpcMeta.getAttachmentSize();

        // read message data
        // message data size = totalSize - metaSize - attachmentSize
        int msgSize = headMeta.getMessageSize();
        int dataSize = msgSize - metaSize - attachmentSize;
        byte[] data = null;
        if (dataSize > 0) {
            data = new byte[dataSize];
            bodyBuf.readBytes(data, 0, dataSize);
        }

        // if need read attachment
        byte[] attachment = null;
        if (attachmentSize > 0) {
            attachment = new byte[attachmentSize];
            bodyBuf.readBytes(attachment, 0, attachmentSize);
        }

        return builder()
                .head(headMeta)
                .rpcMeta(rpcMeta)
                .data(data)
                .attachment(attachment)
                .timeStamp(System.currentTimeMillis())
                .build();
    }

    private RpcResponseMeta getMetaResponse() {
        RpcResponseMeta response = rpcMeta.getResponse();
        if (response == null) {
            response = new RpcResponseMeta();
            rpcMeta.setResponse(response);
        }
        return response;
    }

    public void setErrorCode(Integer errorCode) {
        getMetaResponse().setErrorCode(errorCode);
    }

    public void setErrorText(String errorText) {
        getMetaResponse().setErrorText(errorText);
    }
}
