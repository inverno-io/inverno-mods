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

import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.server.HttpServerConfiguration;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Represents an HTTP server connection.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public interface HttpConnection {
	
	/**
	 * <p>
	 * Determines whether the connection is secured.
	 * </p>
	 * 
	 * @return true if connection has been established using TLS protocol, false otherwise
	 */
	boolean isTls();
	
	/**
	 * <p>
	 * Returns the HTTP protocol version.
	 * <p>
	 * 
	 * @return the HTTP protocol version
	 */
	HttpVersion getProtocol();
	
	/**
	 * <p>
	 * Shutdowns the connection right away.
	 * </p>
	 * 
	 * @return a mono that completes when the connection is closed
	 */
	Mono<Void> shutdown();
	
	/**
	 * <p>
	 * Gracefully shutdown the connection.
	 * </p>
	 * 
	 * <p>
	 * This basically waits for inflight exchanges to complete before closing the connection.
	 * </p>
	 * 
	 * @return a mono that completes when the connection is closed
	 * 
	 * @see HttpServerConfiguration#graceful_shutdown_timeout() 
	 */
	Mono<Void> shutdownGracefully();
	
	/**
	 * <p>
	 * Determines whether the connection is closed.
	 * </p>
	 * 
	 * @return true if the connection is closed, false otherwise
	 */
	boolean isClosed();
}
