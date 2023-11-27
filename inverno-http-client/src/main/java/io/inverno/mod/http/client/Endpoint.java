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

import io.inverno.mod.http.client.ws.WebSocketExchange;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.OutboundRequestHeaders;
import java.net.SocketAddress;
import java.util.function.Consumer;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An endpoint represents the terminal end in an HTTP communication between a client and a server.
 * </p>
 *
 * <p>
 * It is obtained from the {@link HttpClient} and it is bound to an IP Socket Address of an HTTP server. It exposes methods to send HTTP requests or open Web sockets.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public interface Endpoint {

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
	 * Creates an HTTP request.
	 * </p>
	 * 
	 * @param method        the HTTP method
	 * @param requestTarget the request target path
	 * 
	 * @return a new request
	 */
	default Endpoint.Request<ExchangeContext, Exchange<ExchangeContext>, InterceptableExchange<ExchangeContext>> request(Method method, String requestTarget) {
		return this.request(method, requestTarget, null);
	}
	
	/**
	 * <p>
	 * Creates an HTTP request with a context.
	 * </p>
	 * 
	 * @param <A>           the exchange context type
	 * @param method        the HTTP method
	 * @param requestTarget the request target path
	 * @param context       the context
	 * 
	 * @return a new request
	 */
	<A extends ExchangeContext> Endpoint.Request<A, Exchange<A>, InterceptableExchange<A>> request(Method method, String requestTarget, A context);
	
	/**
	 * <p>
	 * Sends an HTTP request.
	 * </p>
	 * 
	 * @param <A>     the exchange context type
	 * @param request an HTTP request
	 * 
	 * @return an HTTP exchange mono
	 */
	<A extends ExchangeContext> Mono<Exchange<A>> send(HttpClient.Request<A, Exchange<A>, InterceptableExchange<A>> request);
	
	/**
	 * <p>
	 * Closes the endpoint.
	 * </p>
	 * 
	 * @return a mono which completes oce the endpoint is closed.
	 */
	Mono<Void> close();
	
	/**
	 * <p>
	 * Creates a WebSocket request.
	 * </p>
	 * 
	 * @param requestTarget the request target path
	 * 
	 * @return a new WebSocket request
	 */
	default Endpoint.WebSocketRequest<ExchangeContext, WebSocketExchange<ExchangeContext>, InterceptableExchange<ExchangeContext>> webSocketRequest(String requestTarget) {
		return this.webSocketRequest(requestTarget, null);
	}
	
	/**
	 * <p>
	 * Creates a WebSocket request with a context.
	 * </p>
	 * 
	 * @param <A>           the exchange context type
	 * @param requestTarget the request target path
	 * @param context       the context
	 * 
	 * @return a new WebSocket request
	 */
	<A extends ExchangeContext> Endpoint.WebSocketRequest<A, WebSocketExchange<A>, InterceptableExchange<A>> webSocketRequest(String requestTarget, A context);
	
	/**
	 * <p>
	 * Sends a WebSocket request.
	 * </p>
	 * 
	 * @param <A>     the exchange context type
	 * @param request a WebSocket request
	 * 
	 * @return a WebSocketExchange mono
	 */
	<A extends ExchangeContext> Mono<WebSocketExchange<A>> send(HttpClient.WebSocketRequest<A, WebSocketExchange<A>, InterceptableExchange<A>> request);
	
	/**
	 * <p>
	 * An HTTP client request bound to an endpoint.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 * 
	 * @param <A> the exchange context type
	 * @param <B> the exchange type
	 * @param <C> the interceptable exchange type
	 */
	interface Request<A extends ExchangeContext, B extends Exchange<A>, C extends InterceptableExchange<A>> extends HttpClient.Request<A, B, C> {

		@Override
		public Endpoint.Request<A, B, C> body(Consumer<RequestBodyConfigurator> bodyConfigurer);

		@Override
		public Endpoint.Request<A, B, C> authority(String authority);

		@Override
		public Endpoint.Request<A, B, C> headers(Consumer<OutboundRequestHeaders> headersConfigurer);

		@Override
		public Endpoint.Request<A, B, C> intercept(ExchangeInterceptor<A, C> interceptor);
		
		/**
		 * <p>
		 * Sends the request.
		 * </p>
		 * 
		 * @return an exchange mono
		 */
		Mono<B> send();
	}
	
	/**
	 * <p>
	 * A WebSocket client request bound to an endpoint.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 * 
	 * @param <A> the exchange context type
	 * @param <B> the WebSocket exchange type
	 * @param <C> the interceptable exchange type
	 */
	interface WebSocketRequest<A extends ExchangeContext, B extends WebSocketExchange<A>, C extends InterceptableExchange<A>> extends HttpClient.WebSocketRequest<A, B, C> {

		@Override
		public Endpoint.WebSocketRequest<A, B, C> subProtocol(String subProtocol);

		@Override
		public Endpoint.WebSocketRequest<A, B, C> authority(String authority);

		@Override
		public Endpoint.WebSocketRequest<A, B, C> headers(Consumer<OutboundRequestHeaders> headersConfigurer);

		@Override
		public Endpoint.WebSocketRequest<A, B, C> intercept(ExchangeInterceptor<A, C> interceptor);
		
		/**
		 * <p>
		 * Sends the request.
		 * </p>
		 * 
		 * @return a WebSocket exchange mono
		 */
		Mono<B> send();
	}
}
