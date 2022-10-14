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
package io.inverno.mod.base.net;

import java.net.SocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;

/**
 * <p>
 * A net service provides methods to create resource friendly IO and acceptor
 * event loop groups ad well as clients and servers.
 * </p>
 * 
 * <p>
 * This service should always be used to create event loop groups, clients or
 * servers (that eventually relies on event loop groups) as it allows to
 * centralize their usage so that thread creation and usage can be optimized
 * based on hardware capabilities.
 * </p>
 * 
 * <p>
 * For instance, a typical implementation would create a single event loop group
 * according to CPU capacities and/or configuration and rely on this limited
 * amount of event loops to provides event loop groups accross the application.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public interface NetService {

	/**
	 * <p>
	 * Represents the transport type supported at runtime.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	public static enum TransportType {
		/**
		 * <a href="https://en.wikipedia.org/wiki/Non-blocking_I/O_(Java)">Nio</a> transport type.
		 */
		NIO,
		/**
		 * <a href="https://en.wikipedia.org/wiki/Epoll">Epoll</a> transport type.
		 */
		EPOLL,
		/**
		 * <a href="https://en.wikipedia.org/wiki/Kqueue">Kqueue</a> transport type.
		 */
		KQUEUE,
		/**
		 * <a href="https://github.com/netty/netty-incubator-transport-io_uring">io_uring</a> transport type. 
		 */
		IO_URING;
	}

	/**
	 * <p>
	 * Returns the transport type.
	 * </p>
	 * 
	 * @return a trasport type
	 */
	TransportType getTransportType();
	
	/**
	 * <p>
	 * Creates a client bootstrap that will connect to the specified address with
	 * all available threads.
	 * </p>
	 * 
	 * @param socketAddress the socket address to connect to
	 * 
	 * @return a client bootstrap
	 */
	Bootstrap createClient(SocketAddress socketAddress);
	
	/**
	 * <p>
	 * Creates a client bootstrap that will connect to the specified address with the specified configuration and all available threads.
	 * </p>
	 *
	 * @param socketAddress       the socket address to connect to
	 * @param clientConfiguration the client configuration
	 *
	 * @return a client bootstrap
	 */
	Bootstrap createClient(SocketAddress socketAddress, NetClientConfiguration clientConfiguration);
	
	/**
	 * <p>
	 * Creates a client bootstrap that will connect to the specified address with amount of threads.
	 * </p>
	 *
	 * @param socketAddress the socket address to connect to
	 * @param nThreads      the number of threads to allocate
	 *
	 * @return a client bootstrap
	 * @throws IllegalArgumentException if the specified number of thread exceeds the number of threads available
	 */
	Bootstrap createClient(SocketAddress socketAddress, int nThreads) throws IllegalArgumentException;
	
	/**
	 * <p>
	 * Creates a client bootstrap that will connect to the specified address with the specified configuration and amount of threads.
	 * </p>
	 *
	 * @param socketAddress       the socket address to connect to
	 * @param clientConfiguration the client configuration
	 * @param nThreads            the number of threads to allocate
	 *
	 * @return a client bootstrap
	 * @throws IllegalArgumentException if the specified number of thread exceeds the number of threads available
	 */
	Bootstrap createClient(SocketAddress socketAddress, NetClientConfiguration clientConfiguration, int nThreads) throws IllegalArgumentException;
	
	/**
	 * <p>
	 * Creates a server bootstrap that will bind to the specified address with all
	 * available threads.
	 * </p>
	 * 
	 * @param socketAddress the socket address to bind to
	 * 
	 * @return a server bootstrap
	 */
	ServerBootstrap createServer(SocketAddress socketAddress);
	
	/**
	 * <p>
	 * Creates a server bootstrap that will bind to the specified address with the specified configuration and all available threads.
	 * </p>
	 *
	 * @param socketAddress       the socket address to bind to
	 * @param serverConfiguration the server configuration
	 *
	 * @return a server bootstrap
	 */
	ServerBootstrap createServer(SocketAddress socketAddress, NetServerConfiguration serverConfiguration);
	
	/**
	 * <p>
	 * Creates a server bootstrap that will bind to the specified address with the
	 * specified amount of threads.
	 * </p>
	 * 
	 * @param socketAddress the socket address to bind to
	 * @param nThreads      the number of threads to allocate
	 * 
	 * @return a server bootstrap
	 * @throws IllegalArgumentException if the specified number of thread exceeds
	 *                                  the number of threads available
	 */
	ServerBootstrap createServer(SocketAddress socketAddress, int nThreads) throws IllegalArgumentException;
	
	/**
	 * <p>
	 * Creates a server bootstrap that will bind to the specified address with the specified server configuration and amount of threads.
	 * </p>
	 *
	 * @param socketAddress       the socket address to bind to
	 * @param serverConfiguration the server configuration
	 * @param nThreads            the number of threads to allocate
	 *
	 * @return a server bootstrap
	 * @throws IllegalArgumentException if the specified number of thread exceeds the number of threads available
	 */
	ServerBootstrap createServer(SocketAddress socketAddress, NetServerConfiguration serverConfiguration, int nThreads) throws IllegalArgumentException;
	
	/**
	 * <p>
	 * Returns a ByteBuf allocator.
	 * </p>
	 * 
	 * <p>
	 * As for event loop groups, this service shall provide optimized ByteBuf
	 * allocators.
	 * </p>
	 * 
	 * @return a byte buf allocator
	 */
	ByteBufAllocator getByteBufAllocator();
	
	/**
	 * <p>
	 * Returns a direct ByteBuf allocator.
	 * </p>
	 * 
	 * <p>
	 * As for event loop groups, this service shall provide optimized ByteBuf
	 * allocators.
	 * </p>
	 * 
	 * @return a byte buf allocator
	 */
	ByteBufAllocator getDirectByteBufAllocator();
}
