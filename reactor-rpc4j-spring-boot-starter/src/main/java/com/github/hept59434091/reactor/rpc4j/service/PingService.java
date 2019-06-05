package com.github.hept59434091.reactor.rpc4j.service;

import com.github.hept59434091.reactor.rpc4j.annotation.ProtoBufMethod;
import com.github.hept59434091.reactor.rpc4j.annotation.ProtoBufService;
import com.github.hept59434091.reactor.rpc4j.service.dto.PingRequest;
import com.github.hept59434091.reactor.rpc4j.service.dto.PingResponse;

import reactor.core.publisher.Mono;

/**
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-06-02
 */
@ProtoBufService
public interface PingService {
    /**
     * ping
     *
     * @param pingRequest req
     *
     * @return res mono
     */
    @ProtoBufMethod
    default Mono<PingResponse> ping(PingRequest pingRequest) {
        return Mono.just(PingResponse.builder().data(pingRequest.getData()).build());
    }
}
