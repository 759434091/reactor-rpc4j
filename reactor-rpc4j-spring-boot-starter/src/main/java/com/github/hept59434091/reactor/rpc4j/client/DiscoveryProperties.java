package com.github.hept59434091.reactor.rpc4j.client;

import lombok.Data;

/**
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-05-05
 */
@Data
public class DiscoveryProperties {
    private String[] endpoints;
    private String rpcPortName;
    private String projectName;
}
