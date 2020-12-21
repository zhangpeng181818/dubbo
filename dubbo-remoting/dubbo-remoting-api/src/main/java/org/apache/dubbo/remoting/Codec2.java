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

import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;

import java.io.IOException;

/**
 * 这两个都是编解码器，那么什么叫做编解码器，在网络中只是讲数据看成是原始的字节序列，
 * 但是我们的应用程序会把这些字节组织成有意义的信息，那么网络字节流和数据间的转化就是很常见的任务。
 * 而编码器是讲应用程序的数据转化为网络格式，解码器则是讲网络格式转化为应用程序，同时具备这两种功能的单一组件就叫编解码器。
 * 在dubbo中Codec是老的编解码器接口，而Codec2是新的编解码器接口，并且dubbo已经用CodecAdapter把Codec适配成Codec2了
 *
 *
 *
 *
 *
 * 因为是编解码器，所以有两个方法分别是编码和解码，上述有以下几个关注的：
 *
 * Codec2是一个可扩展的接口，因为有@SPI注解。
 * 用到了Adaptive机制，首先去url中寻找key为codec的value，来加载url携带的配置中指定的codec的实现。
 * 该接口中有个枚举类型DecodeResult，因为解码过程中，需要解决 TCP 拆包、粘包的场景，所以增加了这两种解码结果，
 * 关于TCP 拆包、粘包的场景我就不多解释，不懂得朋友可以google一下。
 */
@SPI
public interface Codec2 {
    //编码
    @Adaptive({Constants.CODEC_KEY})
    void encode(Channel channel, ChannelBuffer buffer, Object message) throws IOException;


    //解码
    @Adaptive({Constants.CODEC_KEY})
    Object decode(Channel channel, ChannelBuffer buffer) throws IOException;

    // 需要更多输入和忽略一些输入
    enum DecodeResult {
        NEED_MORE_INPUT, SKIP_SOME_INPUT
    }

}

