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

import io.inverno.mod.base.net.NetClientConfiguration;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import java.net.InetSocketAddress;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An HTTP client is used to create an {@link Endpoint} representing an HTTP server and on which client-to-server HTTP exchanges are initiated.
 * </p>
 * 
 * <p>
 * The {@link #endpoint(java.net.InetSocketAddress)} method creates endpoints bound to a the address of an HTTP server. HTTP client exchanges (request/response) are initiated from the endpoint thus 
 * obtained.
 * </p>
 * 
 * <p>
 * The following code show how to send a request to an HTTP server:
 * </p>
 * 
 * <pre>{@code
 * HttpClient httpClient = ...;
 * 
 * Endpoint endpoint = httpClient.endpoint("example.com". 80).build();
 * 
 * String response = endpoint.exchange(Method.GET, "/")
 * 	.flatMap(Exchange::response)
 * 	.flatMapMany(response -> response.body().string().stream())
 * 	.reduceWith(() -> new StringBuilder(), (acc, chunk) -> acc.append(chunk))
 * 	.map(StringBuilder::toString)
 *  .block();
 * 
 * endpoint.close().block();
 * }</pre>
 *
 * <p>
 * {@link UnboundExchange} are created created from the client, they must be explicitly bound to an actual endpoint before they can be sent, so {@link UnboundExchange#bind(Endpoint)} must be invoked
 * before publishers returned by {@link UnboundExchange#response()} or {@link UnboundExchange#webSocket()} are subscribed.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public interface HttpClient {

	/**
	 * <p>
	 * Creates an unbound HTTP exchange.
	 * </p>
	 *
	 * <p>
	 * This method is a shortcut for {@code exchange(Method.GET, "/", null)}.
	 * </p>
	 *
	 * @return an HTTP exchange mono
	 */
	default <T extends ExchangeContext> Mono<? extends UnboundExchange<T>> exchange() {
		return this.exchange(Method.GET, "/", null);
	}

	/**
	 * <p>
	 * Creates an unbound HTTP exchange.
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
	default <T extends ExchangeContext> Mono<? extends UnboundExchange<T>> exchange(T context) {
		return this.exchange(Method.GET, "/", null);
	}

	/**
	 * <p>
	 * Creates an unbound HTTP exchange.
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
	default <T extends ExchangeContext> Mono<? extends UnboundExchange<T>> exchange(String requestTarget) {
		return this.exchange(Method.GET, requestTarget, null);
	}

	/**
	 * <p>
	 * Creates an unbound HTTP exchange.
	 * </p>
	 *
	 * <p>
	 * This method is a shortcut for {@code exchange(Method.GET, requestTarget, context)}.
	 * </p>
	 *
	 * @param requestTarget the request target path
	 * @param context       the exchange context
	 *
	 * @return an HTTP exchange mono
	 */
	default <T extends ExchangeContext> Mono<? extends UnboundExchange<T>> exchange(String requestTarget, T context) {
		return this.exchange(Method.GET, requestTarget, context);
	}

	/**
	 * <p>
	 * Creates an unbound HTTP exchange.
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
	default <T extends ExchangeContext> Mono<? extends UnboundExchange<T>> exchange(Method method, String requestTarget) {
		return this.exchange(method, requestTarget, null);
	}

	/**
	 * <p>
	 * Creates an unbound HTTP exchange with a context.
	 * </p>
	 *
	 * @param method        the HTTP method
	 * @param requestTarget the request target path
	 * @param context       the exchange context
	 *
	 * @return an HTTP exchange mono
	 *
	 * @throws IllegalArgumentException if the specified request target is invalid
	 */
	<T extends ExchangeContext> Mono<? extends UnboundExchange<T>> exchange(Method method, String requestTarget, T context) throws IllegalArgumentException;

	/**
	 * <p>
	 * Creates an endpoint builder to create an {@link Endpoint} bound to the specified host and port.
	 * </p>
	 * 
	 * @param <A>  the exchange context type
	 * @param host the host of the HTTP server
	 * @param port the port of the HTTP server
	 * 
	 * @return an endpoint builder
	 */
	default <A extends ExchangeContext> EndpointBuilder<A, Exchange<A>, InterceptedExchange<A>> endpoint(String host, int port) {
		return this.endpoint(InetSocketAddress.createUnresolved(host, port));
	}
	
	/**
	 * <p>
	 * Creates an endpoint builder to create an Endpoint bound to specified server address.
	 * </p>
	 *
	 * @param <A>           the exchange context type
	 * @param remoteAddress the address of the HTTP server
	 *
	 * @return an endpoint builder
	 */
	<A extends ExchangeContext> EndpointBuilder<A, Exchange<A>, InterceptedExchange<A>> endpoint(InetSocketAddress remoteAddress);
	
	/**
	 * <p>
	 * A builder of {@link Endpoint}.
	 * </p>
	 * 
	 * <p>
	 * Endpoint builders are created by invoking {@link #endpoint(java.net.InetSocketAddress) }.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 * 
	 * @param <A> the exchange context type
	 * @param <B> the exchange type
	 * @param <C> the intercepted exchange type
	 */
	interface EndpointBuilder<A extends ExchangeContext, B extends Exchange<A>, C extends InterceptedExchange<A>> {
		
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
		default EndpointBuilder<A, B, C> localAddress(String host, int port) {
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
		EndpointBuilder<A, B, C> localAddress(InetSocketAddress localAddress);
		
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
		EndpointBuilder<A, B, C> configuration(HttpClientConfiguration configuration);
	
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
		EndpointBuilder<A, B, C> netConfiguration(NetClientConfiguration netConfiguration);
		
		/**
		 * <p>
		 * Specifies an interceptor to intercept requests before they are sent to the endpoint.
		 * </p>
		 * 
		 * <p>
		 * When invoked multiple time this method chains the interceptors one after the other.
		 * </p>
		 * 
		 * @param exchangeInterceptor an exchange interceptor
		 * 
		 * @return the request
		 */
		EndpointBuilder<A, B, C> interceptor(ExchangeInterceptor<? super A, C> exchangeInterceptor);

		/**
		 * <p>
		 * Builds the endpoint.
		 * </p>
		 * 
		 * @return an endpoint
		 */
		Endpoint<A> build();
	}
}
