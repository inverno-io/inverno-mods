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
import io.inverno.mod.base.net.NetClientConfiguration;
import io.inverno.mod.http.base.BaseRequest;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.OutboundRequestHeaders;
import java.net.InetSocketAddress;
import java.util.function.Consumer;

/**
 * <p>
 * An HTTP client is used to create {@link Endpoint}, {@link HttpClient.Request} or {@link HttpClient.WebSocketRequest}.
 * </p>
 * 
 * <p>
 * It exposes {@code endpoint()} methods to create endpoints bound to a the address of an HTTP server. From the endpoint thus obtained it is then possible to create and send requests or open
 * WebSockets to that particular server.
 * </p>
 * 
 * <p>The following code show how to send a request to an HTTP server:</p>
 * 
 * <pre>{@code
 * HttpClient httpCLient = ...;
 * 
 * Endpoint endpoint = httpClient.endpoint("example.com". 80).build();
 * 
 * String response = endpoint.request(Method.GET, "/")
 *	.send()
 *	.flatMapMany(exchange -> exchange.response().body().string().stream())
 *	.reduceWith(() -> new StringBuilder(), (acc, chunk) -> acc.append(chunk))
 *	.map(StringBuilder::toString).block();
 * 
 * endpoint.close().block();
 * }</pre>
 * 
 * <p>
 * It also provides {@link #request(io.inverno.mod.http.base.Method, java.lang.String, io.inverno.mod.http.base.ExchangeContext)} method and
 * {@link #webSocketRequest(java.lang.String, io.inverno.mod.http.base.ExchangeContext)} which can be used to create request or WebSocket requests that are independent from any endpoint and as such
 * can be sent to multiple endpoints.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public interface HttpClient {

	/**
	 * <p>
	 * Creates an endpoint builder to create an Endpoint bound to the specified host and port.
	 * </p>
	 * 
	 * @param host the host of the HTTP server
	 * @param port the port of the HTTP server
	 * 
	 * @return an endpoint builder
	 */
	default EndpointBuilder endpoint(String host, int port) {
		return this.endpoint(InetSocketAddress.createUnresolved(host, port));
	}
	
	/**
	 * <p>
	 * Creates an endpoint builder to create an Endpoint bound to specified server address.
	 * </p>
	 * 
	 * @param remoteAddress the address of the HTTP server
	 * 
	 * @return an endpoint builder
	 */
	EndpointBuilder endpoint(InetSocketAddress remoteAddress);
	
	/**
	 * <p>
	 * A builder of {@link Endpoint}.
	 * </p>
	 * 
	 * <p>
	 * Endpoint builders are created by invoking {@link #endpoint(java.net.InetSocketAddress) }.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	interface EndpointBuilder {
		
		/**
		 * <p>
		 * Sets the local address with the specified host and port.
		 * </p>
		 * 
		 * @param host the local host
		 * @param port the local port
		 * 
		 * @return this builder 
		 */
		default EndpointBuilder localAddress(String host, int port) {
			return localAddress(new InetSocketAddress(host, port));
		}

		/**
		 * <p>
		 * Sets the local address.
		 * </p>
		 * 
		 * @param localAddress the local address
		 * 
		 * @return this builder 
		 */
		EndpointBuilder localAddress(InetSocketAddress localAddress);
		
		/**
		 * <p>
		 * Sets a specific HTTP client configuration for the endpoint.
		 * </p>
		 * 
		 * <p>
		 * A base configuration is provided in the enclosing HTTP client, this method allows to override it for the resulting endpoint only.
		 * </p>
		 * 
		 * @param configuration an HTTP client configuration
		 * 
		 * @return this builder
		 */
		EndpointBuilder configuration(HttpClientConfiguration configuration);
	
		/**
		 * <p>
		 * Sets a specific Net client configuration for the endpoint.
		 * </p>
		 * 
		 * <p>
		 * A base configuration is provided in the enclosing HTTP client, this method allows to override it for the resulting endpoint only.
		 * </p>
		 * 
		 * @param netConfiguration a net client configuration
		 * 
		 * @return this builder
		 */
		EndpointBuilder netConfiguration(NetClientConfiguration netConfiguration);

		/**
		 * <p>
		 * Builds the endpoint.
		 * </p>
		 * 
		 * @return an endpoint
		 */
		Endpoint build();
	}
	
	/**
	 * <p>
	 * Creates an HTTP client request.
	 * </p>
	 * 
	 * @param method the HTTP method
	 * @param requestTarget the request target path
	 * 
	 * @return an HTTP client
	 */
	default HttpClient.Request<ExchangeContext, Exchange<ExchangeContext>, InterceptableExchange<ExchangeContext>> request(Method method, String requestTarget) {
		return this.request(method, requestTarget, null);
	}
	
	/**
	 * <p>
	 * Creates an HTTP client request.
	 * </p>
	 *
	 * @param <A>           the exchange context type
	 * @param method        the HTTP method
	 * @param requestTarget the request target path
	 * @param context       the context
	 *
	 * @return an HTTP client
	 */
	<A extends ExchangeContext> HttpClient.Request<A, Exchange<A>, InterceptableExchange<A>> request(Method method, String requestTarget, A context);
	
	/**
	 * <p>
	 * An HTTP client request.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 * 
	 * @param <A> the exchange context type
	 * @param <B> the exchange type
	 * @param <C> the interceptable exchange type
	 */
	interface Request<A extends ExchangeContext, B extends Exchange<A>, C extends InterceptableExchange<A>> extends BaseRequest {

		/**
		 * <p>
		 * Specifies an interceptor to intercept the request before it is sent to the endpoint.
		 * </p>
		 * 
		 * <p>
		 * When invoked multiple time this method chains the interceptors one after the other.
		 * </p>
		 * 
		 * @param interceptor an exchange interceptor
		 * 
		 * @return the request
		 * 
		 * @throws IllegalStateException when the request has already been sent 
		 */
		HttpClient.Request<A, B, C> intercept(ExchangeInterceptor<A, C> interceptor) throws IllegalStateException;
		
		/**
		 * <p>
		 * Specifies HTTP headers to be sent in the request.
		 * </p>
		 * 
		 * @param headersConfigurer an HTTP headers configurer
		 * 
		 * @return the request
		 * 
		 * @throws IllegalStateException when the request has already been sent 
		 */
		HttpClient.Request<A, B, C> headers(Consumer<OutboundRequestHeaders> headersConfigurer) throws IllegalStateException;

		/**
		 * <p>
		 * Specifies the request authority.
		 * </p>
		 * 
		 * <p>
		 * This corresponds to the authority form as defined by <a href="https://www.rfc-editor.org/rfc/rfc7230#section-5.3.3">RFC1230 Section 5.3.3</a> specified in the {@code host} header or
		 * {@code :authority} psuedo-header.
		 * </p>
		 * 
		 * @param authority the request authority
		 * 
		 * @return the request
		 * 
		 * @throws IllegalStateException when the request has already been sent 
		 */
		HttpClient.Request<A, B, C> authority(String authority) throws IllegalStateException;

		/**
		 * <p>
		 * Specifies the request body.
		 * </p>
		 * 
		 * @param bodyConfigurer a body configurer
		 * 
		 * @return the request
		 * 
		 * @throws IllegalStateException when the request has already been sent 
		 */
		HttpClient.Request<A, B, C> body(Consumer<RequestBodyConfigurator> bodyConfigurer) throws IllegalStateException;
	}
	
	/**
	 * <p>
	 * Creates a WebSocket request.
	 * </p>
	 * 
	 * @param <A> the exchange context type
	 * @param requestTarget the request target path
	 * @param context the context
	 * 
	 * @return a WebSocket request
	 */
	<A extends ExchangeContext> HttpClient.WebSocketRequest<A, WebSocketExchange<A>, InterceptableExchange<A>> webSocketRequest(String requestTarget, A context);
	
	/**
	 * <p>
	 * A WebSocket client request.
	 * </p>
	 * 
	 * @param <A> the exchange context type
	 * @param <B> the WebSocket exchange type
	 * @param <C> the interceptable exchange type
	 */
	interface WebSocketRequest<A extends ExchangeContext, B extends WebSocketExchange<A>, C extends InterceptableExchange<A>> extends BaseRequest {
		
		/**
		 * <p>
		 * Specifies an interceptor to intercept the request before it is sent to the endpoint.
		 * </p>
		 * 
		 * <p>
		 * When invoked multiple time this method chains the interceptors one after the other.
		 * </p>
		 * 
		 * @param interceptor an exchange interceptor
		 * 
		 * @return the WebSocket request
		 */
		HttpClient.WebSocketRequest<A, B, C> intercept(ExchangeInterceptor<A, C> interceptor);
		
		/**
		 * <p>
		 * Specifies HTTP headers to be sent in the request.
		 * </p>
		 * 
		 * @param headersConfigurer an HTTP headers configurer
		 * 
		 * @return the WebSocket request
		 */
		HttpClient.WebSocketRequest<A, B, C> headers(Consumer<OutboundRequestHeaders> headersConfigurer);

		/**
		 * <p>
		 * Specifies the request authority.
		 * </p>
		 * 
		 * <p>
		 * This corresponds to the authority form as defined by <a href="https://www.rfc-editor.org/rfc/rfc7230#section-5.3.3">RFC1230 Section 5.3.3</a> specified in the {@code host} header or
		 * {@code :authority} psuedo-header.
		 * </p>
		 * 
		 * @param authority the request authority
		 * 
		 * @return the WebSocket request
		 */
		HttpClient.WebSocketRequest<A, B, C> authority(String authority);
		
		/**
		 * <p>
		 * Specifies the subprotocol requested to the server by the client.
		 * </p>
		 * 
		 * @param subProtocol the subprotocol
		 * 
		 * @return the WebSocket request
		 */
		HttpClient.WebSocketRequest<A, B, C> subProtocol(String subProtocol);
		
		/**
		 * <p>
		 * Returns the subprotocol requested to the server by the client.
		 * </p>
		 * 
		 * @return the subprotocol or null if no particular subprotocol is to be requested
		 */
		String getSubProtocol();
	}
}
