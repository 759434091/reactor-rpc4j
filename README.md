# reactor-rpc4j
基于 jProtobuf 以及 Protobuf RPC协议 实现的 简单响应式RPC框架

参考自
[baidu/Jprotobuf-rpc-socket](https://github.com/baidu/Jprotobuf-rpc-socket)

使用详见

- reactor-registry-spring-boot-starter  
RPC框架注册中心支持
- reactor-rpc4j-spring-boot-starter   
RPC框架

## 快速开始

### 引入

RPC
```xml
<dependency>
  <groupId>com.github.759434091</groupId>
  <artifactId>reactor-rpc4j-spring-boot-starter</artifactId>
  <version>1.0.1.RELEASE</version>
</dependency>
```

### 定义

#### request

```java
@Data
@Builder
@ProtobufClass
@NoArgsConstructor
@AllArgsConstructor
public class TestRequest {
    private int id;
}
```

#### response

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ProtobufClass
public class TestResponse {
    private int id;
    private String value;
}
```

#### service

```java
import reactor.core.publisher.Mono;

/**
 * 接口参数支持
 * {@link Mono} / {@link Object}
 * 两种格式
 */
@ProtobufService("testService")
public interface TestService {
    @ProtobufMethod
    Mono<TestResponse> testMethod(Mono<TestRequest> testRequestMono);
    
    @ProtobufMethod
    Mono<TestResponse> testMethod(TestRequest testRequest);
}
```

### Server

这里并没有选择暴露和发现分开注解操作, 因为注册接口的时候就已经注定这是暴露还是发现. 

#### 实现

```java
public class TestServiceImpl implements TestService {
    @Override
    public Mono<TestResponse> testMethod(Mono<TestRequest> testRequestMono) {
        return testRequestMono
                .map(testRequest -> TestResponse.builder()
                        .id(testRequest.getId())
                        .value("hello")
                        .build())
                .log();
    }
}
```

#### 注册

或者`@Component\@Service`

```java
@Configuration
public class ServerConfiguration {
    @Bean
    TestServiceImpl testService() {
        return new TestServiceImpl();
    }
}
```

#### 配置

application.properties

````properties
reactor-rpc4j.server.enabled=true
reactor-rpc4j.server.port=8080
````

### Single Client 

怎么简单怎么来..

#### 注册

```java
        TestService testService = RpcServiceClientRegister.register(
                TestService.class,
                "127.0.0.1",
                8000
        );
```

## 测试

服务端启动服务. 



### Client Test

使用`CountDownLatch`避免提前退出. 

```java
        CountDownLatch countDownLatch = new CountDownLatch(1);

        TestRequest testRequest = TestRequest.builder()
                .id(1)
                .build();

        testService.testMethod(Mono.just(testRequest))
                .subscribe(testResponse -> {
                    log.info(JSON.toJSONString(testResponse));
                    countDownLatch.countDown();
                });

        countDownLatch.await();
```

## 注册中心
使用 etcd 作为注册中心

### 引入
```xml
<dependency>
  <groupId>com.github.759434091</groupId>
  <artifactId>reactor-registry-spring-boot-starter</artifactId>
  <version>1.0.1.RELEASE</version>
</dependency>
```

### 注册
开启配置
````properties
reactor-rpc4j.server.register.enabled=true
reactor-rpc4j.server.register.projectName=projectName
reactor-rpc4j.server.register.host=host
  #如 http://127.0.0.1:8081
reactor-rpc4j.server.register.port=port
  # 对外port
reactor-rpc4j.server.register.endpoints=
  # 如 http://127.0.0.1:8081
````

### 发现

`@see RpcServiceClientRegister#etcdRegister`

## 如何贡献
贡献patch流程、质量要求