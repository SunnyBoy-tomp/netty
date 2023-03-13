package com.laodie.rpc.client;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.thread.ThreadFactoryBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author laodie
 * @since 2020-07-29 10:16 下午
 **/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class RpcConnectManager {

    private static final int CPU_NUMS = Runtime.getRuntime().availableProcessors();

    /**
     * 一个连接地址对应一个实际的业务处理器
     */
    private final Map<InetSocketAddress, RpcClientHandler> connectedHandlerMap = new ConcurrentHashMap<>();

    /**
     * 所有连接成功的地址所对应的业务处理器列表
     */
    private final CopyOnWriteArrayList<RpcClientHandler> rpcClientHandlerList = new CopyOnWriteArrayList<>();

    private final EventLoopGroup group = new NioEventLoopGroup(CPU_NUMS);

    private final ReentrantLock connectedLock = new ReentrantLock();

    private final Condition connectedCondition = connectedLock.newCondition();

    private final long connectTimeoutMillis = 6000;

    private volatile boolean isRunning = true;

    private final AtomicInteger handlerIdx = new AtomicInteger(0);

    /**
     * 用于异步提交连接请求的线程池
     */
    private final ExecutorService connectPoolExecutor =
        new ThreadPoolExecutor(CPU_NUMS, 2 * CPU_NUMS, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(65536),
            new ThreadFactoryBuilder().setNamePrefix("connect-executor-").build(), new DiscardPolicy());

    /**
     * 内部枚举类 单例RpcConnectManager对象
     */
    private enum RpcConnectManagerHolder {
        /**
         *
         */
        HOLDER;

        private final RpcConnectManager rpcConnectManager;

        RpcConnectManagerHolder() {
            rpcConnectManager = new RpcConnectManager();
        }
    }

    /**
     * 获取RpcConnectManager单例对象
     *
     * @return RpcConnectManager 对象
     */
    public static RpcConnectManager getInstance() {
        return RpcConnectManagerHolder.HOLDER.rpcConnectManager;
    }

    //1. 异步连接线程池 真正的发起连接，连接失败监听，连接成功监听
    //2. 对于连接进来的资源做一个缓存 （做一个管理）updateConnectedServer

    /**
     * 发起连接方法
     *
     * @param serverAddress
     */
    public void connect(final String serverAddress) {
        List<String> allServerAddress = Arrays.asList(serverAddress.split(","));
        this.updateConnectedServer(allServerAddress);
    }

    /**
     * 更新缓存信息并发起连接
     * 192.168.2.11:5678,192.168.2.22:5678
     *
     * @param allServerAddress
     */
    public void updateConnectedServer(final List<String> allServerAddress) {
        if (CollUtil.isNotEmpty(allServerAddress)) {
            // 1.解析allServerAddress地址并临时存储到newAllServerNodeSet set集合中
            Set<InetSocketAddress> newAllServerNodeSet = new ConcurrentHashSet<>();
            allServerAddress.parallelStream().forEach(serverAddress -> {
                String[] server = serverAddress.split(":");
                if (server.length == 2) {
                    String host = server[0];
                    int port = Integer.parseInt(server[1]);
                    InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
                    newAllServerNodeSet.add(inetSocketAddress);
                }
            });
            // 2.调用建立连接方法 发起远程连接操作
            newAllServerNodeSet.forEach(this::connectAsync);
            // 3.如果allServerAddress中不存在连接地址，需要移除缓存
            rpcClientHandlerList.forEach(rpcClientHandler -> {
                InetSocketAddress remotePeer = (InetSocketAddress)rpcClientHandler.getRemotePeer();
                if (!newAllServerNodeSet.contains(remotePeer)) {
                    log.info("remove invalid server node {}", remotePeer);
                    RpcClientHandler handler = connectedHandlerMap.get(remotePeer);
                    if (handler != null) {
                        handler.close();
                        connectedHandlerMap.remove(remotePeer);
                    }
                    rpcClientHandlerList.remove(rpcClientHandler);
                }

            });
        } else {
            //添加告警
            log.error("no available server address");
            //清除所有的缓存
            this.clearConnected();
        }
    }

    /**
     * 异步发起连接方法
     *
     * @param server
     */
    private void connectAsync(final InetSocketAddress server) {
        if (!connectedHandlerMap.containsKey(server)) {
            connectPoolExecutor.submit(() -> {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new RpcClientInitializer());
                this.connect(bootstrap, server);
            });
        }
    }

    private void connect(final Bootstrap bootstrap, final InetSocketAddress server) {
        // 1. 真正建立连接
        ChannelFuture channelFuture = bootstrap.connect(server);
        // 2. 连接失败后添加监听，清除资源后重新发起连接
        channelFuture.channel().closeFuture().addListener((ChannelFutureListener)future -> {
            log.info("ChannelFuture.channel close operationComplete，remote peer = {}", server);
            future.channel().eventLoop().schedule(() -> {
                log.warn("connect fail, to connect!");
                clearConnected();
                connect(bootstrap, server);
            }, 3, TimeUnit.SECONDS);
        });
        // 3. 连接成功后添加监听，把连接放入缓存中
        channelFuture.addListener((ChannelFutureListener)future -> {
            log.info("success connect to remote server,remote peer = {}", server);
            RpcClientHandler rpcClientHandler = future.channel().pipeline().get(RpcClientHandler.class);
            addHandler(rpcClientHandler);
        });
    }

    /**
     * 添加rpcClientHandler到指定缓存中
     *
     * @param rpcClientHandler
     */
    private void addHandler(RpcClientHandler rpcClientHandler) {
        rpcClientHandlerList.add(rpcClientHandler);
        InetSocketAddress remotePeer = (InetSocketAddress)rpcClientHandler.getChannel().remoteAddress();
        connectedHandlerMap.put(remotePeer, rpcClientHandler);
        //唤醒可用的业务执行器
        signalAvailableHandler();
    }

    /**
     * 唤醒另外一端的线程（阻塞状态中）告知有新的连接接入
     */
    private void signalAvailableHandler() {
        connectedLock.lock();
        try {
            connectedCondition.signalAll();
        } finally {
            connectedLock.unlock();
        }
    }

    /**
     * 等待新连接接入通知方法
     *
     * @return
     * @throws InterruptedException
     */
    private boolean waitingForAvailableHandler() throws InterruptedException {
        connectedLock.lock();
        try {
            return connectedCondition.await(this.connectTimeoutMillis, TimeUnit.MILLISECONDS);
        } finally {
            connectedLock.unlock();
        }
    }

    /**
     * 选择一个实际的业务处理器
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public RpcClientHandler chooseHandler() {
        CopyOnWriteArrayList<RpcClientHandler> handlers
            = (CopyOnWriteArrayList<RpcClientHandler>)this.rpcClientHandlerList.clone();
        int size = handlers.size();
        while (isRunning && size <= 0) {
            try {
                boolean available = this.waitingForAvailableHandler();
                if (available) {
                    handlers = (CopyOnWriteArrayList<RpcClientHandler>)this.rpcClientHandlerList.clone();
                    size = handlers.size();
                }
            } catch (InterruptedException e) {
                log.error("waiting for available node interrupted! ");
                throw new RuntimeException("no connect any servers!", e);
            }
        }
        if (!isRunning) {
            return null;
        }
        //使用取模方式获取其中一个业务处理器进行实际的业务处理
        return rpcClientHandlerList.get(((handlerIdx.getAndIncrement() + size) % size));
    }

    /**
     * 连接失败时候，及时释放资源，清空缓存
     * 先删除所有的connectedHandlerMap中的数据
     * 再清空rpcClientHandlers list中的数据
     */
    private void clearConnected() {
        rpcClientHandlerList.parallelStream().forEach(rpcClientHandler -> {
            //通过RpcClientHandler 找到具体的remotePeer,从connectHandlerMap中移除缓存
            InetSocketAddress remotePeer = (InetSocketAddress)rpcClientHandler.getRemotePeer();
            RpcClientHandler clientHandler = connectedHandlerMap.get(remotePeer);
            if (clientHandler != null) {
                clientHandler.close();
                connectedHandlerMap.remove(remotePeer);
            }
        });
        rpcClientHandlerList.clear();
    }

    /**
     * 停止方法
     */
    public void stop() {
        this.isRunning = false;
        rpcClientHandlerList.parallelStream().forEach(RpcClientHandler::close);
        //调用一下唤醒操作
        this.signalAvailableHandler();
        //关闭线程池
        connectPoolExecutor.shutdown();
        group.shutdownGracefully();
    }

    /**
     * 重连方法 先释放对应的资源
     *
     * @param rpcClientHandler
     * @param remotePeer
     */
    public void reconnect(final RpcClientHandler rpcClientHandler, InetSocketAddress remotePeer) {
        if (rpcClientHandler != null) {
            rpcClientHandler.close();
            rpcClientHandlerList.remove(rpcClientHandler);
            connectedHandlerMap.remove(remotePeer);
        }
        this.connectAsync(remotePeer);
    }
}
