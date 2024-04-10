/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.http.client;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import java.net.SocketAddress;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An endpoint represents the terminal end in an HTTP communication from a client to a server.
 * </p>
 *
 * <p>
 * It is obtained from the {@link HttpClient} and it is bound to an IP Socket Address of an HTTP server. It exposes methods to create HTTP client {@link Exchange} used to create and send HTTP requests
 * or open Web sockets.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @param <A> the exchange context type
 */
public interface Endpoint<A extends ExchangeContext> {

	/**
	 * <p>
	 * Returns the local socket address of the endpoint.
	 * </p>
	 * 
	 * @return a socket address
	 */
	SocketAddress getLocalAddress();
	
	/**
	 * <p>
	 * Returns the remote socket address of the endpoint.
	 * </p>
	 * 
	 * @return a socket address
	 */
	SocketAddress getRemoteAddress();
	
	/**
	 * <p>
	 * Creates an HTTP exchange.
	 * </p>
	 * 
	 * <p>
	 * This method is a shortcut for {@code exchange(Method.GET, "/", null)}.
	 * </p>
	 * 
	 * @return an HTTP exchange mono
	 */
	default Mono<? extends Exchange<A>> exchange() {
		return this.exchange(Method.GET, "/", null);
	}
	
	/**
	 * <p>
	 * Creates an HTTP exchange.
	 * </p>
	 * 
	 * <p>
	 * This method is a shortcut for {@code exchange(Method.GET, "/", context)}.
	 * </p>
	 * 
	 * @param context the exchange context
	 * 
	 * @return an HTTP exchange mono
	 */
	default Mono<? extends Exchange<A>> exchange(A context) {
		return this.exchange(Method.GET, "/", null);
	}
	
	/**
	 * <p>
	 * Creates an HTTP exchange.
	 * </p>
	 * 
	 * <p>
	 * This method is a shortcut for {@code exchange(Method.GET, requestTarget, null)}.
	 * </p>
	 * 
	 * @param requestTarget the request target path
	 * 
	 * @return an HTTP exchange mono
	 */
	default Mono<? extends Exchange<A>> exchange(String requestTarget) {
		return this.exchange(Method.GET, requestTarget, null);
	}
	
	/**
	 * <p>
	 * Creates an HTTP exchange.
	 * </p>
	 * 
	 * <p>
	 * This method is a shortcut for {@code exchange(method, requestTarget, null)}.
	 * </p>
	 * 
	 * @param method        the HTTP method
	 * @param requestTarget the request target path
	 * 
	 * @return an HTTP exchange mono
	 */
	default Mono<? extends Exchange<A>> exchange(Method method, String requestTarget) {
		return this.exchange(method, requestTarget, null);
	}
	
	/**
	 * <p>
	 * Creates an HTTP exchange with a context.
	 * </p>
	 * 
	 * @param method        the HTTP method
	 * @param requestTarget the request target path
	 * @param context       the exchange context
	 * 
	 * @return an HTTP exchange mono
	 */
	Mono<? extends Exchange<A>> exchange(Method method, String requestTarget, A context);
	
	/**
	 * <p>
	 * Shutdowns the endpoint right away.
	 * </p>
	 * 
	 * <p>
	 * This is a hard shutdown that closes all active connections right away.
	 * </p>
	 * 
	 * <p>
	 * Note that <i>detached</i> connections (e.g. WebSocket) are not considered here and must be closed individually.
	 * </p>
	 * 
	 * @return a mono which completes once the endpoint is shutdown
	 */
	Mono<Void> shutdown();
	
	/**
	 * <p>
	 * Gracefully shutdowns the endpoint.
	 * </p>
	 * 
	 * <p>
	 * This is a graceful shutdown that waits for all active exchanges to complete before shutting down the connections.
	 * </p>
	 * 
	 * <p>
	 * Note that <i>detached</i> connections (e.g. WebSocket) are not considered here and must be closed individually.
	 * </p>
	 * 
	 * @return a mono which completes once the endpoint is shutdown
	 * 
	 * @see HttpClientConfiguration#graceful_shutdown_timeout() 
	 */
	Mono<Void> shutdownGracefully();
}
