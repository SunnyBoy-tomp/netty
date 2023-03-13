package com.laodie.rpc.config;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author laodie
 * @since 2020-08-04 8:42 下午
 **/
@Data
public abstract class RpcConfigAbstract {

    private final AtomicInteger generator = new AtomicInteger(0);

    private String id;

    protected String interfaceClass = null;

    /**
     * 服务调用方（consumer端独有）
     */
    protected Class<?> proxyClass = null;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        if (StrUtil.isBlank(id)) {
            id = "rpc-cfg-gen-" + generator.getAndIncrement();
        }
        return id;
    }
}
