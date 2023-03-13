package com.laodie.rpc.client;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.laodie.rpc.codec.RpcRequest;
import com.laodie.rpc.codec.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.AbstractQueuedLongSynchronizer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author laodie
 * @since 2020-08-08 8:48 下午
 **/
@Slf4j
public class RpcFuture implements Future<Object> {

    private final int CPU_NUMBER = Runtime.getRuntime().availableProcessors();

    private final RpcRequest request;

    private RpcResponse response;

    private final long startTime;

    private final List<RpcCallback> pendingCallbacks = new ArrayList<>();

    private final Lock lock = new ReentrantLock();

    private final ExecutorService executor =
        new ThreadPoolExecutor(CPU_NUMBER, 2 * CPU_NUMBER, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(65536),
            new ThreadFactoryBuilder().setNamePrefix("rpc-client-executor").build());

    /**
     * 耗时阈值
     */
    private static final long TIME_THRESHOLD = 5000;

    private final Sync sync;

    public RpcFuture(RpcRequest request) {
        this.request = request;
        this.startTime = System.currentTimeMillis();
        this.sync = new Sync();
    }

    /**
     * 实际的回调处理
     *
     * @param response
     */
    public void done(RpcResponse response) {
        this.response = response;
        boolean release = sync.release(1);
        if (release) {
            invokerCallbacks();
        }
        //整体RPC调用耗时
        long costTime = System.currentTimeMillis() - startTime;
        if (TIME_THRESHOLD < costTime) {
            log.warn("the rpc response time is too slow,requestId = {} ,costTime = {}",
                this.request.getRequestId(), costTime);
        }
    }

    /**
     * 依次执行callback函数方法
     */
    private void invokerCallbacks() {
        lock.lock();
        try {
            this.pendingCallbacks.forEach(this::runCallback);
        } finally {
            lock.unlock();
        }
    }

    private void runCallback(RpcCallback rpcCallback) {
        final RpcResponse response = this.response;
        executor.submit(() -> {
            if (response.getThrowable() == null) {
                rpcCallback.success(response.getResult());
            } else {
                rpcCallback.failure(response.getThrowable());
            }
        });
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    public RpcFuture addFuture(RpcCallback rpcCallback) {
        lock.lock();
        try {
            if (isDone()) {
                runCallback(rpcCallback);
            } else {
                this.pendingCallbacks.add(rpcCallback);
            }
        } finally {
            lock.unlock();
        }
        return this;
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        sync.acquire(-1);
        if (this.response != null) {
            return this.response.getResult();
        }
        return null;
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean success = sync.tryAcquireNanos(-1, unit.toNanos(timeout));
        if (success) {
            if (this.response != null) {
                return this.response.getResult();
            }
        } else {
            throw new RuntimeException(
                "timeout exception requestId: " + this.request.getRequestId() + " ,className ："
                    + request.getClassName() + " ,methodName" + this.request.getMethodName());
        }
        return null;
    }

    static class Sync extends AbstractQueuedLongSynchronizer {

        private static final long serialVersionUID = -1392061618739331007L;

        private final int done = 1;

        private final int pending = 0;

        @Override
        protected boolean tryAcquire(long arg) {
            return getState() == done;
        }

        @Override
        protected boolean tryRelease(long arg) {
            if (getState() == pending) {
                if (compareAndSetState(pending, done)) {
                    return true;
                }
            }
            return false;
        }

        public boolean isDone() {
            return getState() == done;
        }
    }
}
