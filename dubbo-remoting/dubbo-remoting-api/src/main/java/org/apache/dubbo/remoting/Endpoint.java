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
package org.apache.dubbo.remoting;

import org.apache.dubbo.common.URL;

import java.net.InetSocketAddress;

/**
 * Endpoint. (API/SPI, Prototype, ThreadSafe)
 *
 * dubbo抽象出一个端的概念，也就是Endpoint接口，这个端就是一个点，而点对点之间是可以双向传输。
 * 在端的基础上在衍生出通道、客户端以及服务端的概念，也就是下面要介绍的Channel、Client、Server三个接口。
 * 在传输层，其实Client和Server的区别只是在语义上区别，并不区分请求和应答职责，在交换层客户端和服务端也是一个点，
 * 但是已经是有方向的点，所以区分了明确的请求和应答职责。两者都具备发送的能力，只是客户端和服务端所关注的事情不一样，
 * 这个在后面会分开介绍，而Endpoint接口抽象的方法就是它们共同拥有的方法。这也就是它们都能被抽象成端的原因。
 *
 * @see org.apache.dubbo.remoting.Channel
 * @see org.apache.dubbo.remoting.Client
 * @see RemotingServer
 */
public interface Endpoint {

    /**
     * get url.
     *
     * @return url
     */
    // 获得该端的url
    URL getUrl();

    /**
     * get channel handler.
     *
     * @return channel handler
     */
    // 获得该端的通道处理器
    ChannelHandler getChannelHandler();

    /**
     * get local address.
     *
     * @return local address.
     */
    // 获得该端的本地地址
    InetSocketAddress getLocalAddress();

    /**
     * send message.
     *
     * @param message
     * @throws RemotingException
     */
    // 发送消息
    void send(Object message) throws RemotingException;

    /**
     * send message.
     *
     * @param message
     * @param sent    already sent to socket?
     */
    // 发送消息，sent是是否已经发送的标记
    void send(Object message, boolean sent) throws RemotingException;

    /**
     * close the channel.
     */
    // 关闭
    void close();

    /**
     * Graceful close the channel.
     */
    // 优雅的关闭，也就是加入了等待时间
    void close(int timeout);

    // 开始关闭
    void startClose();

    /**
     * is closed.
     *
     * @return closed
     */
    // 判断是否已经关闭
    boolean isClosed();

    /**
     * 前三个方法是获得该端本身的一些属性，
     * 两个send方法是发送消息，其中第二个方法多了一个sent的参数，为了区分是否是第一次发送消息。
     * 后面几个方法是提供了关闭通道的操作以及判断通道是否关闭的操作。
     */

}