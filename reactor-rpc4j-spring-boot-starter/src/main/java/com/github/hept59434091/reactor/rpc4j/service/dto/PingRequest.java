package com.github.hept59434091.reactor.rpc4j.service.dto;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-06-02
 */
@Data
@Builder
@ProtobufClass
@NoArgsConstructor
@AllArgsConstructor
public class PingRequest {
    private String data;
}
