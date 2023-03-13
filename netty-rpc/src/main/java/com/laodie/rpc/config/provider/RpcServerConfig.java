package com.laodie.rpc.config.provider;

import com.laodie.rpc.server.RpcServer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author laodie
 * @since 2020-08-09 10:00 下午
 **/
@Slf4j
@Data
public class RpcServerConfig {

    private final String host = "127.0.0.1";

    protected int port;

    private List<ProviderConfig> providerConfigs;

    private RpcServer rpcServer;

    public RpcServerConfig(List<ProviderConfig> providerConfigs) {
        this.providerConfigs = providerConfigs;
    }

    public void exporter() {
        if (rpcServer == null) {
            try {
                rpcServer = new RpcServer(host + ":" + port);
            } catch (InterruptedException e) {
                log.error("RpcServerConfig exporter exception : ", e);
            }
            providerConfigs.forEach(providerConfig -> rpcServer.registerProcessor(providerConfig));
        }

    }

}
