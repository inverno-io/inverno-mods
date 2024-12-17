/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.http.server.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.concurrent.Reactor;
import io.inverno.mod.http.server.HttpServerConfiguration;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Tracks active HTTP connections on the server.
 * </p>
 * 
 * <p>
 * It uses a {@link ChannelGroup} which automatically evicts closed connections.
 * </p>
 * 
 * <p>
 * When the group is closed or shutting down, the server shall not be able to accept new connections.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 * 
 * @see HttpServer
 * @see HttpServerChannelInitializer
 */
@Bean(visibility = Bean.Visibility.PRIVATE)
public class HttpConnectionGroup {
	
	private static final Logger LOGGER = LogManager.getLogger(HttpConnectionGroup.class);
	
	private final ChannelGroup channelGroup;
	
	private boolean closing;
	private boolean closed;

	/**
	 * <p>
	 * Creates an HTTP connection group.
	 * </p>
	 * 
	 * @param reactor the reactor
	 */
	public HttpConnectionGroup(Reactor reactor) {
		this.channelGroup = new DefaultChannelGroup(reactor.getAcceptorEventLoopGroup().next());
	}
	
	/**
	 * <p>
	 * Registers the specified channel in the group.
	 * </p>
	 * 
	 * @param channel the channel to register
	 */
	public void register(Channel channel) {
		this.channelGroup.add(channel);
	}

	/**
	 * <p>
	 * Returns a stream of active HTTP connections.
	 * </p>
	 * 
	 * @return a stream of connections
	 */
	private Stream<HttpConnection> getActiveConnections() {
		return this.channelGroup.stream().map(channel -> {
				ChannelHandler handler = channel.pipeline().get("connection");
				if(handler instanceof HttpConnection) {
					return (HttpConnection)handler;
				}
				return null;
			})
			.filter(Objects::nonNull);
	}
	
	/**
	 * <p>
	 * Shutdowns all connections right away.
	 * </p>
	 * 
	 * @return a mono that completes when all connections are closed
	 */
	public Mono<Void> shutdown() {
		if(this.closing || this.closed) {
			return Mono.empty();
		}
		return Flux.fromStream(this::getActiveConnections)
			.doFirst(() -> {
				LOGGER.debug("Shutting down connections...");
				this.closing = true;
			})
			.flatMap(HttpConnection::shutdown)
				.doOnError(e -> LOGGER.warn("Error shutting down connection", e))
				.onErrorResume(e -> true, e -> Mono.empty()
			)
			.doOnTerminate(() -> {
				this.closed = true;
				this.closing = false;
			})
			.then();
	}
	
	/**
	 * <p>
	 * Gracefully shutdown all connections.
	 * </p>
	 * 
	 * <p>
	 * This basically waits for inflight exchanges to complete before closing the connections.
	 * </p>
	 * 
	 * @return a mono that completes when all connections are closed
	 * 
	 * @see HttpServerConfiguration#graceful_shutdown_timeout() 
	 */
	public Mono<Void> shutdownGracefully() {
		if(this.closing || this.closed) {
			return Mono.empty();
		}
		return Flux.fromStream(this::getActiveConnections)
			.doFirst(() -> {
				LOGGER.debug("Shutting down connections gracefully...");
				this.closing = true;
			})
			.flatMap(HttpConnection::shutdownGracefully)
				.doOnError(e -> LOGGER.warn("Error shutting down connection", e))
				.onErrorResume(e -> true, e -> Mono.empty()
			)
			.doOnTerminate(() -> {
				this.closed = true;
				this.closing = false;
			})
			.then();
	}
	
	/**
	 * <p>
	 * Determines whether group's connections are shutting down.
	 * </p>
	 * 
	 * @return true if the group is shutting down, false otherwise
	 */
	public boolean isShuttingDown() {
		return this.closing;
	}
	
	/**
	 * <p>
	 * Determines whether group's connections are closed.
	 * </p>
	 * 
	 * @return true if the group is closed, false otherwise
	 */
	public boolean isClosed() {
		return this.closed;
	}
}
