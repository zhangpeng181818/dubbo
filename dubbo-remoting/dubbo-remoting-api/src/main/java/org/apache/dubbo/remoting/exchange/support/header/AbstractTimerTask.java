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

package org.apache.dubbo.remoting.exchange.support.header;

import org.apache.dubbo.common.timer.Timeout;
import org.apache.dubbo.common.timer.Timer;
import org.apache.dubbo.common.timer.TimerTask;
import org.apache.dubbo.remoting.Channel;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * AbstractTimerTask
 */
public abstract class AbstractTimerTask implements TimerTask {

    /**
     * 通道管理
     */
    private final ChannelProvider channelProvider;

    private final Long tick;

    protected volatile boolean cancel = false;

    AbstractTimerTask(ChannelProvider channelProvider, Long tick) {
        if (channelProvider == null || tick == null) {
            throw new IllegalArgumentException();
        }
        this.tick = tick;
        this.channelProvider = channelProvider;
    }
    // 最后一次接收到消息的时间戳
    static Long lastRead(Channel channel) {
        return (Long) channel.getAttribute(HeartbeatHandler.KEY_READ_TIMESTAMP);
    }

    static Long lastWrite(Channel channel) {
        return (Long) channel.getAttribute(HeartbeatHandler.KEY_WRITE_TIMESTAMP);
    }

    static Long now() {
        return System.currentTimeMillis();
    }

    public void cancel() {
        this.cancel = true;
    }

    //重新将定时任务放入
    private void reput(Timeout timeout, Long tick) {
        if (timeout == null || tick == null) {
            throw new IllegalArgumentException();
        }

        if (cancel) {
            return;
        }

        Timer timer = timeout.timer();
        if (timer.isStop() || timeout.isCancelled()) {
            return;
        }

        timer.newTimeout(timeout.task(), tick, TimeUnit.MILLISECONDS);
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        Collection<Channel> c = channelProvider.getChannels();
        // 遍历所有通道
        for (Channel channel : c) {
            // 如果通道关闭了，则跳过
            if (channel.isClosed()) {
                continue;
            }
            doTask(channel);
        }
        reput(timeout, tick);
    }

    protected abstract void doTask(Channel channel);

    // 获得所有的通道集合，需要心跳的通道数组
//    该接口就定义了一个方法，获得需要心跳的通道集合。可想而知，会对集合内的通道都做心跳检测。
    interface ChannelProvider {
        Collection<Channel> getChannels();
    }
}
