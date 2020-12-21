/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.remoting.transport.netty4;

import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.rpc.model.ApplicationModel;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.ThreadFactory;

/**
 *
 * https://segmentfault.com/a/1190000021054503?utm_source=tag-newest
 *
 * https://blog.csdn.net/xing317521/article/details/105054661/
 *
 *Linux有select、poll和epoll三个解决方案来实现多路复用，其中的select和poll，有点类似于上面的NIOServerDemo程序，他会把所有的文件描述符记录在一个数组中，通过在系统内核中遍历来筛选出有数据过来的Socket，只不过是从应用程序中遍历改成了在内核中遍历，本质还是一样的。
 *
 * Epoll则使用了事件机制，在复用器中注册了一个回调事件，当Socket中有数据过来的时候调用，通知用户处理信息，这样就不需要对全部的文件描述符进行轮训了，这就是Epoll对NIO进行的改进。
 */
public class NettyEventLoopFactory {
    public static EventLoopGroup eventLoopGroup(int threads, String threadFactoryName) {
        ThreadFactory threadFactory = new DefaultThreadFactory(threadFactoryName, true);
        return shouldEpoll() ? new EpollEventLoopGroup(threads, threadFactory) :
                new NioEventLoopGroup(threads, threadFactory);
    }

    public static Class<? extends SocketChannel> socketChannelClass() {
        return shouldEpoll() ? EpollSocketChannel.class : NioSocketChannel.class;
    }

    public static Class<? extends ServerSocketChannel> serverSocketChannelClass() {
        return shouldEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
    }

    private static boolean shouldEpoll() {
        Configuration configuration = ApplicationModel.getEnvironment().getConfiguration();
        if (configuration.getBoolean("netty.epoll.enable", false)) {
            String osName = configuration.getString("os.name");
            return osName.toLowerCase().contains("linux") && Epoll.isAvailable();
        }

        return false;
    }
}
