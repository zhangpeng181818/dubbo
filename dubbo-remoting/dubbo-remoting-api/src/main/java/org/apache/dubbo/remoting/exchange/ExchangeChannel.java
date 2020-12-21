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
package org.apache.dubbo.remoting.exchange;

import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * ExchangeChannel. (API/SPI, Prototype, ThreadSafe)
 *
 * Exchange层，也就是信息交换层。
 * 官方文档对这一层的解释是封装请求响应模式，同步转异步，
 * 以 Request, Response为中心，扩展接口为 Exchanger, ExchangeChannel, ExchangeClient, ExchangeServer。
 */
public interface ExchangeChannel extends Channel {

    /**
     * send request.
     *
     * @param request
     * @return response future
     * @throws RemotingException
     */
    @Deprecated
    CompletableFuture<Object> request(Object request) throws RemotingException;

    /**
     * send request.
     *
     * @param request
     * @param timeout
     * @return response future
     * @throws RemotingException
     */
    @Deprecated
    CompletableFuture<Object> request(Object request, int timeout) throws RemotingException;

    /**
     * send request.
     *
     * @param request
     * @return response future
     * @throws RemotingException
     */
    CompletableFuture<Object> request(Object request, ExecutorService executor) throws RemotingException;

    /**
     * send request.
     *
     * @param request
     * @param timeout
     * @return response future
     * @throws RemotingException
     */
    CompletableFuture<Object> request(Object request, int timeout, ExecutorService executor) throws RemotingException;

    /**
     * get message handler.
     *
     * @return message handler
     */
//    getExchangeHandler方法就是返回一个信息交换处理器
    ExchangeHandler getExchangeHandler();

    /**
     * graceful close.
     *
     * @param timeout
     */
    @Override
    void close(int timeout);
}
