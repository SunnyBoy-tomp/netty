package com.laodie.rpc.consumer;

/**
 * @author laodie
 * @since 2020-08-09 10:30 下午
 **/
public interface HelloService {

    String hello(String username);

    String hello(User user);

}
