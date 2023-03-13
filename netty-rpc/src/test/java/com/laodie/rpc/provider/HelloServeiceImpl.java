package com.laodie.rpc.provider;

import com.laodie.rpc.consumer.HelloService;
import com.laodie.rpc.consumer.User;

/**
 * @author laodie
 * @since 2020-08-09 10:31 下午
 **/
public class HelloServeiceImpl implements HelloService {
    @Override
    public String hello(String username) {
        return "hello " + username;
    }

    @Override
    public String hello(User user) {
        return "hello " + user;
    }
}
