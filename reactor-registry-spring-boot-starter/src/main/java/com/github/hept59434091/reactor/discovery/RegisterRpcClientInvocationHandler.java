package com.github.hept59434091.reactor.discovery;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.atomic.LongAdder;

import org.jctools.maps.NonBlockingHashMap;
import org.springframework.core.annotation.AnnotationUtils;

import com.github.hept59434091.reactor.rpc4j.RpcServiceClientRegister;
import com.github.hept59434091.reactor.rpc4j.annotation.ProtoBufService;
import com.github.hept59434091.reactor.rpc4j.client.DiscoveryProperties;
import com.github.hept59434091.reactor.rpc4j.client.RpcClientInvocationHandler;
import com.github.hept59434091.reactor.rpc4j.exc.RpcClientException;
import com.github.hept59434091.reactor.rpc4j.util.Rpc4jUtil;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.watch.WatchResponse;
import lombok.Getter;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

/**
 * RegisterRpcClientInvocationHandler
 *
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-05-05
 */
public class RegisterRpcClientInvocationHandler<SERVICE> implements InvocationHandler, Disposable {
    @Getter
    private final Class<SERVICE> clazz;
    @Getter
    private final String serviceName;
    @Getter
    private final String keyPrefix;
    private final NonBlockingHashMap<String, SERVICE> rpcProxyMap;
    private final LongAdder longAdder;
    private final int keyPrefixLen;

    private final DiscoveryClient discoveryClient;

    private volatile Object[] proxyView;

    private RegisterRpcClientInvocationHandler(Class<SERVICE> clazz,
                                               DiscoveryProperties discoveryProperties) {
        ProtoBufService protoBufService = Optional
                .ofNullable(AnnotationUtils.findAnnotation(clazz, ProtoBufService.class))
                .orElseThrow(() -> new IllegalArgumentException("Cannot find annotation @ProtoBufService"));
        String instanceName = Rpc4jUtil.getInstanceName(clazz);

        this.clazz = clazz;
        this.serviceName = Rpc4jUtil.getServiceName(protoBufService, instanceName);
        this.rpcProxyMap = new NonBlockingHashMap<>(16);
        this.longAdder = new LongAdder();

        this.keyPrefix = String.format("/%s/%s/", discoveryProperties.getProjectName(), serviceName);
        this.keyPrefixLen = keyPrefix.length();

        Client client = Client
                .builder()
                .endpoints(discoveryProperties.getEndpoints())
                .build();

        this.discoveryClient = DiscoveryClient.create(
                client,
                keyPrefix,
                this::watchResponseConsumer
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return method.invoke(getService(), args);
    }

    private Object getService() {
        Object[] objects = proxyView;
        Object object = null;
        while (object == null) {
            if (proxyView.length == 0) {
                throw new RpcClientException("No available instance");
            }

            object = objects[Math.abs(longAdder.intValue() % objects.length)];
        }

        return object;
    }

    private void watchResponseConsumer(WatchResponse watchResponse) {
        watchResponse
                .getEvents()
                .forEach(watchEvent -> {
                    String key = watchEvent.getKeyValue().getKey().toString(Charset.defaultCharset());

                    switch (watchEvent.getEventType()) {
                        case PUT:
                            String serviceAddr = key.substring(keyPrefixLen);
                            String[] hostPort = serviceAddr.split(":");

                            Mono
                                    .just(hostPort[0])
                                    .subscribe(ip -> RpcServiceClientRegister
                                            .register(clazz, ip, Integer.valueOf(hostPort[1]))
                                            .subscribe(service -> rpcProxyMap.put(key, service)));
                            break;
                        case DELETE:
                        case UNRECOGNIZED:
                            ((RpcClientInvocationHandler) Proxy
                                    .getInvocationHandler(rpcProxyMap.remove(key)))
                                    .dispose();
                            break;
                        default:
                    }
                });

        proxyView = rpcProxyMap.values().toArray();
    }

    @Override
    public void dispose() {
        discoveryClient.dispose();
        rpcProxyMap.forEach((key, value) -> ((RpcClientInvocationHandler) Proxy.getInvocationHandler(value)).dispose());
    }

    @Override
    public boolean isDisposed() {
        return discoveryClient.isDisposed();
    }
}

