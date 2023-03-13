package com.laodie.rpc.provider;

import com.laodie.rpc.config.provider.ProviderConfig;
import com.laodie.rpc.config.provider.RpcServerConfig;

import java.util.ArrayList;
import java.util.List;



/**
 * @author laodie
 * @since 2020-08-09 10:33 下午
 **/
public class ProviderStarter {

    public static void main(String[] args) {
        ProviderConfig providerConfig = new ProviderConfig();
        providerConfig.setInterfaceClass("com.laodie.rpc.consumer.HelloService");
        providerConfig.setRef(new HelloServeiceImpl());
        List<ProviderConfig> providerConfigs = new ArrayList<>();
        providerConfigs.add(providerConfig);
        RpcServerConfig rpcServerConfig = new RpcServerConfig(providerConfigs);
        rpcServerConfig.setPort(8766);
        rpcServerConfig.exporter();
    }

}
