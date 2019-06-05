package com.github.hept59434091.reactor.registry;

import java.util.concurrent.CompletableFuture;

import org.springframework.lang.NonNull;

import com.github.hept59434091.reactor.exc.RegisterException;
import com.google.protobuf.ByteString;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.CloseableClient;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Observers;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.options.PutOption;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * RegisterClient
 *
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-05-05
 */
@Slf4j
@Getter
public class RegisterClient implements AutoCloseable {
    /**
     * raw string key
     */
    private final String keyStr;
    /**
     * value, default EMPTY
     */
    private final ByteSequence value = ByteSequence.from(ByteString.EMPTY);
    /**
     * etcd client
     */
    @SuppressWarnings("SpellCheckingInspection")
    private final Client client;
    /**
     * byteSeq key
     */
    private final ByteSequence key;
    /**
     * kv ttl value, unit seconds
     */
    private final Integer ttl;

    /**
     * observer, default log.debug kv & lease
     */
    private final StreamObserver<LeaseKeepAliveResponse> keepAliveResponseStreamObserver;

    /**
     * keepAlive client
     */
    private CloseableClient closeableClient;

    /**
     * lease Id
     */
    private Long leaseId;

    private RegisterClient(Client client, String keyStr, Integer ttl) {
        this.ttl = ttl;
        this.client = client;
        this.keyStr = keyStr;
        this.key = ByteSequence.from(ByteString.copyFromUtf8(keyStr));
        keepAliveResponseStreamObserver = Observers
                .observer(leaseKeepAliveResponse -> log
                        .info("refresh key={}, lease={}", keyStr, leaseKeepAliveResponse.getID()));
        register();
    }

    private synchronized void register() {
        Lease lease = client.getLeaseClient();

        lease
                .grant(ttl)
                .thenApply(LeaseGrantResponse::getID)
                .thenCompose(leaseId -> client.getKVClient()
                        .put(key, value, PutOption.newBuilder().withLeaseId(leaseId).withPrevKV().build())
                        .thenApply(putResponse -> leaseId))
                .thenApply(leaseId -> {
                    this.leaseId = leaseId;
                    return leaseId;
                })
                .thenApply(leaseId -> lease.keepAlive(leaseId, keepAliveResponseStreamObserver))
                .thenAccept(client -> closeableClient = client)
                .exceptionally(throwable -> {
                    throw new RegisterException("register error. ", throwable);
                });
    }

    @Override
    public void close() {
        closeableClient.close();
        CompletableFuture
                .allOf(
                        client.getKVClient().delete(key),
                        client.getLeaseClient().revoke(leaseId))
                .exceptionally(throwable -> {
                    throw new RegisterException("close client error. ", throwable);
                });
    }

    public static RegisterClient create(
            @NonNull Client client,
            @NonNull String projectName,
            @NonNull String serviceName,
            @NonNull String host,
            @NonNull Integer port,
            @NonNull Integer ttl
    ) {
        String keyStr = String.format("/%s/%s/%s:%d", projectName, serviceName, host, port);
        return new RegisterClient(client, keyStr, ttl);
    }

    public static RegisterClient create(
            @NonNull Client client,
            @NonNull String projectName,
            @NonNull String serviceName,
            @NonNull String host,
            @NonNull Integer port
    ) {
        return create(client, projectName, serviceName, host, port, 10);
    }
}
