package com.github.hept59434091.reactor.rpc4j.facade;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-04-17
 */
@Data
@Builder
@ProtobufClass
@NoArgsConstructor
@AllArgsConstructor
public class TestRequest {
    private int id;
}
