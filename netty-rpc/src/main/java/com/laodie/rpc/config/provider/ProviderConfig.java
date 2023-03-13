package com.laodie.rpc.config.provider;

import com.laodie.rpc.config.RpcConfigAbstract;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author laodie
 * @since 2020-08-04 8:48 下午
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class ProviderConfig extends RpcConfigAbstract {

    protected Object ref;

}
