package com.github.hept59434091.reactor;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.hept59434091.reactor.registry.RegisterClient;
import com.github.hept59434091.reactor.registry.RegisterProperties;
import com.github.hept59434091.reactor.rpc4j.pojo.RpcServiceRoute;
import com.github.hept59434091.reactor.rpc4j.util.Rpc4jUtil;

import io.etcd.jetcd.Client;

/**
 * RegisterAutoConfiguration
 *
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-05-05
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnBean(RpcServiceRoute.class)
@ConditionalOnProperty(prefix = "reactor-rpc4j.server.register", name = "enable", havingValue = "true")
public class RegisterAutoConfiguration {
    @Resource
    RpcServiceRoute rpcServiceRoute;

    @Bean
    @ConditionalOnProperty("reactor-rpc4j.server.register")
    RegisterProperties registerProperties() {
        return new RegisterProperties();
    }

    @Bean
    @ConditionalOnMissingBean(Client.class)
    Client client() {
        RegisterProperties registerProperties = registerProperties();

        return Client
                .builder()
                .endpoints(registerProperties.getEndpoints())
                .build();
    }

    @Bean
    Map<String, RegisterClient> registerClientMap() {
        Client client = client();
        RegisterProperties registerProperties = registerProperties();

        return rpcServiceRoute
                .keySet()
                .stream()
                .map(Rpc4jUtil::getServiceName)
                .collect(Collectors.toMap(
                        Function.identity(),
                        serviceName -> RegisterClient.create(
                                client,
                                registerProperties.getProjectName(),
                                serviceName,
                                registerProperties.getHost(),
                                registerProperties.getPort(),
                                registerProperties.getTtl())
                ));
    }

    @PreDestroy
    private void destroy() {
        registerClientMap()
                .values()
                .forEach(RegisterClient::close);
    }
}
