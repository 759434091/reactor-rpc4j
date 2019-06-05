package com.github.hept59434091.reactor.discovery;

import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.springframework.lang.NonNull;

import com.google.protobuf.ByteString;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;

/**
 * DiscoveryClient
 *
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-05-05
 */
@Slf4j
@Getter
public class DiscoveryClient implements Disposable {
    private final Client client;
    private final String keyPrefixStr;
    private final ByteSequence keyPrefix;
    private final Watch.Watcher watcher;
    private final AtomicBoolean isDisposed;

    private DiscoveryClient(Client client,
                            String keyPrefixStr,
                            Consumer<WatchResponse> watchResponseConsumer) {
        this.isDisposed = new AtomicBoolean(false);
        this.client = client;
        this.keyPrefixStr = keyPrefixStr;
        this.keyPrefix = ByteSequence.from(ByteString.copyFromUtf8(keyPrefixStr));
        this.watcher = client
                .getWatchClient()
                .watch(
                        keyPrefix,
                        WatchOption.newBuilder().withPrefix(ByteSequence.from(ByteString.EMPTY)).build(),
                        watchResponseConsumer
                );
    }

    public static DiscoveryClient create(
            @NonNull Client client,
            @NonNull String keyPrefixStr,
            @NonNull Consumer<WatchResponse> watchResponseConsumer
    ) {
        return new DiscoveryClient(client, keyPrefixStr, watchResponseConsumer);
    }

    public static DiscoveryClient create(
            @NonNull Client client,
            @NonNull String keyPrefixStr
    ) {
        Consumer<WatchResponse> logWatchResponseConsumer = watchResponse -> watchResponse
                .getEvents()
                .forEach(watchEvent -> {
                    log.info("eventType={}, key={}, leaseId={}",
                            watchEvent.getEventType(),
                            watchEvent.getKeyValue().getKey().toString(Charset.defaultCharset()),
                            watchEvent.getKeyValue().getLease());
                });
        return create(client, keyPrefixStr, logWatchResponseConsumer);
    }

    @Override
    public void dispose() {
        if (isDisposed.getAndSet(true)) {
            return;
        }
        watcher.close();
    }

    @Override
    public boolean isDisposed() {
        return isDisposed.get();
    }
}
