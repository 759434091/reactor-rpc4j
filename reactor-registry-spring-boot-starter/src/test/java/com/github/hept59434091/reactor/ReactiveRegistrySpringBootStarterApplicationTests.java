package com.github.hept59434091.reactor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.alibaba.fastjson.JSON;
import com.github.hept59434091.reactor.discovery.DiscoveryClient;
import com.github.hept59434091.reactor.registry.RegisterClient;

import io.etcd.jetcd.Client;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ReactiveRegistrySpringBootStarterApplicationTests {
    private Client client = Client
            .builder()
            .endpoints("http://127.0.0.1:2379")
            .build();

    @Test
    @SneakyThrows
    public void contextLoads() {
        client
                .getClusterClient()
                .listMember()
                .thenAccept(memberListResponse -> log.info(JSON.toJSONString(memberListResponse)))
                .get();
    }

    @Test
    @SneakyThrows
    public void register() {
        RegisterClient registerClient = RegisterClient.create(
                client,
                "com.github.hept59434091.reactor",
                "reactorFacade",
                "127.0.0.1",
                8081,
                3
        );

        DiscoveryClient discoveryClient = DiscoveryClient.create(client,
                "com.baidu.cpu");

        Thread.sleep(5 * 1000);
        registerClient.close();

        Thread.sleep(2 * 1000);
        discoveryClient.close();
    }

}
