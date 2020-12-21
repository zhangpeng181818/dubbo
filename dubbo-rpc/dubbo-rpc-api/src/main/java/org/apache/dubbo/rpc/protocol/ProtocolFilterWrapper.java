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
package org.apache.dubbo.rpc.protocol;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ListenableFilter;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProtocolServer;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.REFERENCE_FILTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SERVICE_FILTER_KEY;

/**
 * ListenerProtocol
 *
 * 该类实现了Protocol接口，其中也用到了装饰模式，是对Protocol的装饰，是在服务引用和暴露的方法上加上了过滤器功能。
 */
@Activate(order = 100)
public class ProtocolFilterWrapper implements Protocol {

    private final Protocol protocol;

    public ProtocolFilterWrapper(Protocol protocol) {
        if (protocol == null) {
            throw new IllegalArgumentException("protocol == null");
        }
        this.protocol = protocol;
    }

    //该方法就是创建带 Filter 链的 Invoker 对象。倒序的把每一个过滤器串连起来，形成一个invoker。
    private static <T> Invoker<T> buildInvokerChain(final Invoker<T> invoker, String key, String group) {
        Invoker<T> last = invoker;
        // 获得过滤器的所有扩展实现类实例集合
        List<Filter> filters = ExtensionLoader.getExtensionLoader(Filter.class).getActivateExtension(invoker.getUrl(), key, group);

        if (!filters.isEmpty()) {
            // 从最后一个过滤器开始循环，创建一个带有过滤器链的invoker对象
            for (int i = filters.size() - 1; i >= 0; i--) {
                final Filter filter = filters.get(i);
                // 记录last的invoker
                final Invoker<T> next = last;
                // 新建last
                last = new Invoker<T>() {

                    @Override
                    public Class<T> getInterface() {
                        return invoker.getInterface();
                    }

                    @Override
                    public URL getUrl() {
                        return invoker.getUrl();
                    }

                    @Override
                    public boolean isAvailable() {
                        return invoker.isAvailable();
                    }

                    @Override
                    public Result invoke(Invocation invocation) throws RpcException {
                        Result asyncResult;
                        try {
                            asyncResult = filter.invoke(next, invocation);
                        } catch (Exception e) {
                            if (filter instanceof ListenableFilter) {
                                ListenableFilter listenableFilter = ((ListenableFilter) filter);
                                try {
                                    Filter.Listener listener = listenableFilter.listener(invocation);
                                    if (listener != null) {
                                        listener.onError(e, invoker, invocation);
                                    }
                                } finally {
                                    listenableFilter.removeListener(invocation);
                                }
                            } else if (filter instanceof Filter.Listener) {
                                Filter.Listener listener = (Filter.Listener) filter;
                                listener.onError(e, invoker, invocation);
                            }
                            throw e;
                        } finally {

                        }
                        return asyncResult.whenCompleteWithContext((r, t) -> {
                            if (filter instanceof ListenableFilter) {
                                ListenableFilter listenableFilter = ((ListenableFilter) filter);
                                Filter.Listener listener = listenableFilter.listener(invocation);
                                try {
                                    if (listener != null) {
                                        if (t == null) {
                                            listener.onResponse(r, invoker, invocation);
                                        } else {
                                            listener.onError(t, invoker, invocation);
                                        }
                                    }
                                } finally {
                                    listenableFilter.removeListener(invocation);
                                }
                            } else if (filter instanceof Filter.Listener) {
                                Filter.Listener listener = (Filter.Listener) filter;
                                if (t == null) {
                                    listener.onResponse(r, invoker, invocation);
                                } else {
                                    listener.onError(t, invoker, invocation);
                                }
                            }
                        });
                    }

                    @Override
                    public void destroy() {
                        invoker.destroy();
                    }

                    @Override
                    public String toString() {
                        return invoker.toString();
                    }
                };
            }
        }

        return last;
    }

    @Override
    public int getDefaultPort() {
        return protocol.getDefaultPort();
    }

    //该方法是在服务暴露上做了过滤器链的增强，也就是加上了过滤器。
    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        // 如果是注册中心，则直接暴露服务
        if (UrlUtils.isRegistry(invoker.getUrl())) {
            return protocol.export(invoker);
        }
        // 服务提供侧暴露服务
        return protocol.export(buildInvokerChain(invoker, SERVICE_FILTER_KEY, CommonConstants.PROVIDER));
    }

//    该方法是在服务引用上做了过滤器链的增强，也就是加上了过滤器。
    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        // 如果是注册中心，则直接引用
        if (UrlUtils.isRegistry(url)) {
            return protocol.refer(type, url);
        }
        // 消费者侧引用服务
        return buildInvokerChain(protocol.refer(type, url), REFERENCE_FILTER_KEY, CommonConstants.CONSUMER);
    }

    @Override
    public void destroy() {
        protocol.destroy();
    }

    @Override
    public List<ProtocolServer> getServers() {
        return protocol.getServers();
    }

}
