package com.github.hept59434091.reactor.registry;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * RegisterProperties
 *
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-05-05
 */
@Data
public class RegisterProperties {
    private Integer ttl = 10;
    @NotBlank
    private String projectName;
    @NotBlank
    private String host;
    @NotNull
    private Integer port;
    @NotNull
    private String[] endpoints;
    @NotNull
    private String rpcPortName = "EM_PORT_RPC";
}
