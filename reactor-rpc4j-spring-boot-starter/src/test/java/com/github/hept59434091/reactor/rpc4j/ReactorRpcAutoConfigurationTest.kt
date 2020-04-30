package com.github.hept59434091.reactor.rpc4j

import com.github.hept59434091.reactor.rpc4j.facade.TestRequest
import com.github.hept59434091.reactor.rpc4j.facade.TestResponse
import com.github.hept59434091.reactor.rpc4j.facade.TestService
import com.github.hept59434091.reactor.rpc4j.server.RpcServerProperties
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.test.context.junit4.SpringRunner
import reactor.core.publisher.Mono
import javax.annotation.Resource
import kotlin.test.assertNotNull

@RunWith(SpringRunner::class)
@SpringBootTest(
        classes = [ReactorRpcAutoConfiguration::class],
        properties = ["reactor-rpc4j.server.enabled=true"]
)
class ReactorRpcAutoConfigurationTest {
    class TestServiceVoidImpl : TestService {
        override fun testMethod(
                testRequestMono: Mono<TestRequest>
        ): Mono<TestResponse> {
            return Mono.empty()
        }

        override fun testMethod2(
                testRequest: TestRequest
        ): Mono<TestResponse> {
            return Mono.empty()
        }

    }

    @Bean
    fun testService() = TestServiceVoidImpl()

    @Resource
    lateinit var reactorRpcAutoConfiguration: ReactorRpcAutoConfiguration

    @Test
    fun rpcServerProperties() {
        println("${
        reactorRpcAutoConfiguration.rpcServerProperties()
        }")
    }

    @Test
    fun rpcServer() {
        println("${
        reactorRpcAutoConfiguration.rpcServer(RpcServerProperties())
        }")
    }
}