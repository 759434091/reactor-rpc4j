package com.github.hept59434091.reactor.rpc4j.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-04-17
 */
@SpringBootApplication(scanBasePackages = "com.github.hept59434091.reactor.rpc4j.server")
public class ServerTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServerTestApplication.class, args);
    }
}
