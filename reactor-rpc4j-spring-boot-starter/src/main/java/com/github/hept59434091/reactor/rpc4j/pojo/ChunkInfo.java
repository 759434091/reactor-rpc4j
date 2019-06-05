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
 * @since 2019-04-17
 */
@Data
public class ChunkInfo {
    /**
     * The Constant CODEC.
     */
    private static final Codec<ChunkInfo> CODEC = ProtobufProxy.create(ChunkInfo.class);

    /**
     * 用于唯一标识一个数据流，由发送方保证其唯一性，协议不对此进行任何检查.
     */
    @Protobuf(required = true)
    private Long streamId;

    /**
     * 从0开始严格递增。发送方需保证按序发送Chunk包。数据流的最后一个包chunk_id为-1。<br> 由于Protobuf RPC基于TCP协议，因此包之间的顺序可以保证.
     */
    @Protobuf(required = true)
    private long chunkId = -1;
}
