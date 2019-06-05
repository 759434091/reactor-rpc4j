package com.github.hept59434091.reactor.rpc4j.util;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.lang.NonNull;

import com.baidu.bjf.remoting.protobuf.Codec;
import com.baidu.bjf.remoting.protobuf.ProtobufProxy;
import com.github.hept59434091.reactor.rpc4j.annotation.ProtoBufMethod;
import com.github.hept59434091.reactor.rpc4j.annotation.ProtoBufService;
import com.github.hept59434091.reactor.rpc4j.pojo.RpcRequestMeta;

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

/**
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-04-12
 */
public class Rpc4jUtil {
    @SuppressWarnings("SpellCheckingInspection")
    public static final byte[] MAGIC_CODE = "PRPC".getBytes();
    private static final int MAGIC_CODE_LEN = MAGIC_CODE.length;

    public static String getMethodSignature(@NonNull RpcRequestMeta rpcRequestMeta) {
        return rpcRequestMeta.getServiceName() + "$$" + rpcRequestMeta.getMethodName();
    }

    public static String getMethodSignature(@NonNull String serviceName, @NonNull String methodName) {
        return serviceName + "$$" + methodName;
    }

    public static String getMethodName(@NonNull ProtoBufMethod protobufMethod, @NonNull Method method) {
        return Optional.of(protobufMethod)
                .map(ProtoBufMethod::methodName)
                .filter(srName -> !srName.isEmpty())
                .orElse(method.getName());
    }

    public static String getServiceName(@NonNull ProtoBufService protobufService,
                                        @NonNull String beanName) {
        return Optional.of(protobufService)
                .map(ProtoBufService::serviceName)
                .filter(srName -> !srName.isEmpty())
                .orElse(beanName);
    }

    public static String getServiceName(@NonNull String signature) {
        int stringsLen = 2;
        String[] strings = signature.split("\\$\\$");
        if (strings.length != stringsLen || strings[0].isEmpty()) {
            throw new IllegalArgumentException("Invalid signature. " + signature);
        }

        return strings[0];
    }

    public static Codec getReqCodec(@NonNull Method method, @NonNull String signature) {
        return Optional.of(method)
                .map(Method::getGenericParameterTypes)
                .filter(types -> types.length == 1)
                .map(types -> types[0])
                .map(type -> {
                    if (type instanceof ParameterizedTypeImpl) {
                        return Optional
                                .of(type)
                                .map(ParameterizedTypeImpl.class::cast)
                                .map(ParameterizedTypeImpl::getActualTypeArguments)
                                .filter(types -> types.length == 1)
                                .map(types -> types[0])
                                .map(Class.class::cast)
                                .orElseThrow(() -> new RuntimeException("Invalid service method " +
                                        signature +
                                        " . 1 Cannot get request Codec"));
                    }
                    return (Class) type;
                })
                .map((Function<Class, Codec>) ProtobufProxy::create)
                .orElseThrow(() -> new RuntimeException("Invalid service method " +
                        signature +
                        " . 1 Cannot get request Codec"));
    }

    public static Codec getResCodec(@NonNull Method method, @NonNull String signature) {
        return Optional.of(method)
                .map(Method::getGenericReturnType)
                .map(ParameterizedTypeImpl.class::cast)
                .map(ParameterizedTypeImpl::getActualTypeArguments)
                .filter(types -> types.length == 1)
                .map(types -> types[0])
                .map(Class.class::cast)
                .map((Function<Class, Codec>) ProtobufProxy::create)
                .orElseThrow(() -> new RuntimeException("Invalid service method " +
                        signature +
                        " . 1 Cannot get response Codec"));
    }

    public static boolean validateMagicCode(@NonNull byte[] bytes) {
        if (bytes.length != MAGIC_CODE_LEN) {
            return false;
        }
        for (int i = 0; i < MAGIC_CODE_LEN; i++) {
            if (MAGIC_CODE[i] != bytes[i]) {
                return false;
            }
        }
        return true;
    }

    public static String getInstanceName(Class clazz) {
        char startChar = 'A';
        char endChar = 'Z';
        char caseStep = 32;

        char[] chars = clazz.getSimpleName().toCharArray();
        if (chars[0] >= startChar && chars[0] <= endChar) {
            chars[0] = (char) (chars[0] + caseStep);
        }
        return new String(chars);
    }
}