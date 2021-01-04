/*
 * Copyright 2021 Jeremy KUHN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.winterframework.mod.commons.net;

import java.net.SocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.EventLoopGroup;

/**
 * @author jkuhn
 *
 */
public interface NetService {

	public static enum TransportType {
		EPOLL,
		KQUEUE,
		NIO;
	}
	
	TransportType getTransportType();
	
	EventLoopGroup createAcceptorEventLoopGroup();
	
	EventLoopGroup createIoEventLoopGroup();
	
	EventLoopGroup createIoEventLoopGroup(int nThreads);
	
	Bootstrap createClient(SocketAddress socketAddress);
	
	Bootstrap createClient(SocketAddress socketAddress, int nThreads);
	
	ServerBootstrap createServer(SocketAddress socketAddress);
	
	ServerBootstrap createServer(SocketAddress socketAddress, int nThreads);
	
	ByteBufAllocator getByteBufAllocator();
	
	ByteBufAllocator getDirectByteBufAllocator();
}
