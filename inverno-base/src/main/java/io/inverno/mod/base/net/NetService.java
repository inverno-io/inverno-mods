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

import io.netty.resolver.AddressResolverGroup;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import java.util.List;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A net service provides methods to create resource friendly network clients and servers and resolve host addresses with DNS resolution.
 * </p>
 * 
 * <p>
 * This service should always be used to create clients or servers as it allows to centralize their usage so that thread creation and usage can be optimized based on hardware capabilities.
 * </p>
 *
 * <p>
 * A typical implementation would rely on the {@link io.inverno.mod.base.concurrent.Reactor} to obtain optimized event loop groups for the creation of client and server bootstraps.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see io.inverno.mod.base.concurrent.Reactor
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
	enum TransportType {
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
		IO_URING
	}

	/**
	 * <p>
	 * Returns the transport type.
	 * </p>
	 * 
	 * @return a transport type
	 */
	TransportType getTransportType();
	
	/**
	 * <p>
	 * Creates a client bootstrap that will connect to the specified address with all available threads.
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
	 *
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
	 *
	 * @throws IllegalArgumentException if the specified number of thread exceeds the number of threads available
	 */
	Bootstrap createClient(SocketAddress socketAddress, NetClientConfiguration clientConfiguration, int nThreads) throws IllegalArgumentException;
	
	/**
	 * <p>
	 * Creates a server bootstrap that will bind to the specified address with all available threads.
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
	 * Creates a server bootstrap that will bind to the specified address with the specified amount of threads.
	 * </p>
	 * 
	 * @param socketAddress the socket address to bind to
	 * @param nThreads      the number of threads to allocate
	 * 
	 * @return a server bootstrap
	 *
	 * @throws IllegalArgumentException if the specified number of thread exceeds the number of threads available
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
	 *
	 * @throws IllegalArgumentException if the specified number of thread exceeds the number of threads available
	 */
	ServerBootstrap createServer(SocketAddress socketAddress, NetServerConfiguration serverConfiguration, int nThreads) throws IllegalArgumentException;

	/**
	 * <p>
	 * Resolves an address associated to the specified host name.
	 * </p>
	 *
	 * @param host a host name
	 *
	 * @return a mono emitting an address
	 */
	default Mono<InetAddress> resolve(String host) {
		return this.resolve(InetSocketAddress.createUnresolved(host, 0)).map(InetSocketAddress::getAddress);
	}

	/**
	 * <p>
	 * Resolves a socket address associated to the specified host name and port.
	 * </p>
	 *
	 * @param host a host name
	 * @param port a port
	 *
	 * @return a mono emitting a socket address
	 */
	default Mono<InetSocketAddress> resolve(String host, int port) {
		return this.resolve(InetSocketAddress.createUnresolved(host, port));
	}

	/**
	 * <p>
	 * Resolves a socket address associated to the socket address.
	 * </p>
	 *
	 * @param socketAddress a socket address
	 *
	 * @return a mono emitting a socket address
	 */
	Mono<InetSocketAddress> resolve(InetSocketAddress socketAddress);

	/**
	 * <p>
	 * Resolves all addresses associated to the specified host name.
	 * </p>
	 *
	 * @param host a host name
	 *
	 * @return a mono emitting a list of addresses
	 */
	default Mono<List<InetAddress>> resolveAll(String host) {
		return this.resolveAll(InetSocketAddress.createUnresolved(host, 0)).map(addresses -> addresses.stream().map(InetSocketAddress::getAddress).collect(Collectors.toList()));
	}

	/**
	 * <p>
	 * Resolves all socket addresses associated to the specified host name and port.
	 * </p>
	 *
	 * @param host a host name
	 * @param port a port
	 *
	 * @return a mono emitting a list of socket addresses
	 */
	default Mono<List<InetSocketAddress>> resolveAll(String host, int port) {
		return this.resolveAll(InetSocketAddress.createUnresolved(host, port));
	}

	/**
	 * <p>
	 * Resolves all socket addresses associated to the specified socket address.
	 * </p>
	 *
	 * @param socketAddress a socket address
	 *
	 * @return a mono emitting a list of socket addresses
	 */
	Mono<List<InetSocketAddress>> resolveAll(InetSocketAddress socketAddress);

	/**
	 * <p>
	 * Returns the DNS resolver.
	 * </p>
	 *
	 * @return the name resolver
	 */
	AddressResolverGroup<InetSocketAddress> getResolver();

	/**
	 * <p>
	 * Returns a {@code ByteBuf} allocator.
	 * </p>
	 * 
	 * <p>
	 * As for event loop groups, this service shall provide optimized {@code ByteBuf} allocators.
	 * </p>
	 * 
	 * @return a byte buf allocator
	 */
	ByteBufAllocator getByteBufAllocator();
	
	/**
	 * <p>
	 * Returns a direct {@code ByteBuf} allocator.
	 * </p>
	 * 
	 * <p>
	 * As for event loop groups, this service shall provide optimized {@code ByteBuf} allocators.
	 * </p>
	 * 
	 * @return a byte buf allocator
	 */
	ByteBufAllocator getDirectByteBufAllocator();
}
