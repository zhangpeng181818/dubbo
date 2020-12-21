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
package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.io.Bytes;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.support.RpcUtils;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;

/**
 * ConsistentHashLoadBalance
 *
 * 该类是负载均衡基于 hash 一致性的逻辑实现。
 * 一致性哈希算法由麻省理工学院的 Karger 及其合作者于1997年提供出的，一开始被大量运用于缓存系统的负载均衡。
 * 它的工作原理是这样的：首先根据 ip 或其他的信息为缓存节点生成一个 hash，在dubbo中使用参数进行计算hash。
 * 并将这个 hash 投射到 [0, 232 - 1] 的圆环上，当有查询或写入请求时，则生成一个 hash 值。
 * 然后查找第一个大于或等于该 hash 值的缓存节点，并到这个节点中查询或写入缓存项。
 * 如果当前节点挂了，则在下一次查询或写入缓存时，为缓存项查找另一个大于其 hash 值的缓存节点即可。
 * 大致效果如下图所示（引用一下官网的图）
 *
 * 每个缓存节点在圆环上占据一个位置。
 * 如果缓存项的 key 的 hash 值小于缓存节点 hash 值，则到该缓存节点中存储或读取缓存项，
 * 这里有两个概念不要弄混，缓存节点就好比dubbo中的服务提供者，会有很多的服务提供者，
 * 而缓存项就好比是服务引用的消费者。
 * 比如下面绿色点对应的缓存项也就是服务消费者将会被存储到 cache-2 节点中。
 * 由于 cache-3 挂了，原本应该存到该节点中的缓存项也就是服务消费者最终会存储到 cache-4 节点中，
 * 也就是调用cache-4 这个服务提供者。
 *
 * https://segmentfault.com/a/1190000018105767
 */
public class ConsistentHashLoadBalance extends AbstractLoadBalance {
    public static final String NAME = "consistenthash";

    /**
     * Hash nodes name
     */
    public static final String HASH_NODES = "hash.nodes";

    /**
     * Hash arguments name
     */
    public static final String HASH_ARGUMENTS = "hash.arguments";

    private final ConcurrentMap<String, ConsistentHashSelector<?>> selectors = new ConcurrentHashMap<String, ConsistentHashSelector<?>>();

    @SuppressWarnings("unchecked")
    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        // 获得方法名
        String methodName = RpcUtils.getMethodName(invocation);
        // 获得key
        String key = invokers.get(0).getUrl().getServiceKey() + "." + methodName;
        // using the hashcode of list to compute the hash only pay attention to the elements in the list
        // 获取 invokers 原始的 hashcode
        int invokersHashCode = invokers.hashCode();
        // 从一致性 hash 选择器集合中获得一致性 hash 选择器
        ConsistentHashSelector<T> selector = (ConsistentHashSelector<T>) selectors.get(key);
        // 如果等于空或者选择器的hash值不等于原始的值，则新建一个一致性 hash 选择器，并且加入到集合
        if (selector == null || selector.identityHashCode != invokersHashCode) {
            selectors.put(key, new ConsistentHashSelector<T>(invokers, methodName, invokersHashCode));
            selector = (ConsistentHashSelector<T>) selectors.get(key);
        }
        return selector.select(invocation);
    }

    private static final class ConsistentHashSelector<T> {

        private final TreeMap<Long, Invoker<T>> virtualInvokers;

        private final int replicaNumber;

        private final int identityHashCode;

        private final int[] argumentIndex;

        ConsistentHashSelector(List<Invoker<T>> invokers, String methodName, int identityHashCode) {
            this.virtualInvokers = new TreeMap<Long, Invoker<T>>();
            this.identityHashCode = identityHashCode;
            URL url = invokers.get(0).getUrl();
            this.replicaNumber = url.getMethodParameter(methodName, HASH_NODES, 160);
            String[] index = COMMA_SPLIT_PATTERN.split(url.getMethodParameter(methodName, HASH_ARGUMENTS, "0"));
            argumentIndex = new int[index.length];
            for (int i = 0; i < index.length; i++) {
                argumentIndex[i] = Integer.parseInt(index[i]);
            }
            for (Invoker<T> invoker : invokers) {
                String address = invoker.getUrl().getAddress();
                for (int i = 0; i < replicaNumber / 4; i++) {
                    byte[] digest = Bytes.getMD5(address + i);
                    for (int h = 0; h < 4; h++) {
                        long m = hash(digest, h);
                        virtualInvokers.put(m, invoker);
                    }
                }
            }
        }

        public Invoker<T> select(Invocation invocation) {
            String key = toKey(invocation.getArguments());
            byte[] digest = Bytes.getMD5(key);
            return selectForKey(hash(digest, 0));
        }

        private String toKey(Object[] args) {
            StringBuilder buf = new StringBuilder();
            for (int i : argumentIndex) {
                if (i >= 0 && i < args.length) {
                    buf.append(args[i]);
                }
            }
            return buf.toString();
        }

        private Invoker<T> selectForKey(long hash) {
            Map.Entry<Long, Invoker<T>> entry = virtualInvokers.ceilingEntry(hash);
            if (entry == null) {
                entry = virtualInvokers.firstEntry();
            }
            return entry.getValue();
        }
//计算hash值
        private long hash(byte[] digest, int number) {
            return (((long) (digest[3 + number * 4] & 0xFF) << 24)
                    | ((long) (digest[2 + number * 4] & 0xFF) << 16)
                    | ((long) (digest[1 + number * 4] & 0xFF) << 8)
                    | (digest[number * 4] & 0xFF))
                    & 0xFFFFFFFFL;
        }
    }

}
