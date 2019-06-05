package com.github.hept59434091.reactor.rpc4j.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.hept59434091.reactor.rpc4j.server.service.TestServiceImpl;

/**
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-04-17
 */
@Configuration
public class ServerConfiguration {
    @Bean
    TestServiceImpl testService() {
        return new TestServiceImpl();
    }
}
