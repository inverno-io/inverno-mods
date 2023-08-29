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
package io.inverno.mod.http.client.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.RequestBodyConfigurator;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Represents an HTTP connection to an endpoint.
 * </p>
 * 
 * <p>
 * It is used to send request to the endoint.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
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
	 * Returns the maximum number of concurrent requests that can be sent on the connection.
	 * </p>
	 * 
	 * @return the maximum number of concurrent requests
	 */
	Long getMaxConcurrentRequests();
	
	/**
	 * <p>
	 * Sets the connection handler.
	 * </p>
	 * 
	 * @param handler the handler to set
	 */
	void setHandler(HttpConnection.Handler handler);

	/**
	 * <p>
	 * Sends an empty request to the connected endpoint.
	 * </p>
	 *
	 * @param <A>             the exchange context type
	 * @param exchangeContext the context
	 * @param method          the HTTP method
	 * @param authority       the request authority
	 * @param headers         the list of HTTP header entries
	 * @param path            the request target path
	 *
	 * @return a mono emitting the exchange once response headers has been received
	 */
	default <A extends ExchangeContext> Mono<Exchange<A>> send(
			A exchangeContext,
			Method method, 
			String authority, 
			List<Map.Entry<String, String>> headers, 
			String path) {
		return this.send(exchangeContext, method, authority, headers, path, null, null, null);
	}
	
	/**
	 * <p>
	 * Sends a request to the connected endpoint.
	 * </p>
	 *
	 * @param <A>                   the exchange context type
	 * @param exchangeContext       the context
	 * @param method                the HTTP method
	 * @param authority             the request authority
	 * @param headers               the list of HTTP header entries
	 * @param path                  the request target path
	 * @param requestBodyConfigurer a request body configurer or null to create an empty request
	 *
	 * @return a mono emitting the exchange once response headers has been received
	 */
	default <A extends ExchangeContext> Mono<Exchange<A>> send(
			A exchangeContext,
			Method method, 
			String authority, 
			List<Map.Entry<String, String>> headers, 
			String path, 
			Consumer<RequestBodyConfigurator> requestBodyConfigurer) {
		return this.send(exchangeContext, method, authority, headers, path, requestBodyConfigurer, null, null);
	}
	
	/**
	 * <p>
	 * Sends a request to the endpoint.
	 * </p>
	 *
	 * @param <A>                     the exchange context type
	 * @param exchangeContext         the context
	 * @param method                  the HTTP method
	 * @param authority               the request authority
	 * @param headers                 the list of HTTP header entries
	 * @param path                    the request target path
	 * @param requestBodyConfigurer   a request body configurer or null to create an empty request
	 * @param requestBodyTransformer  a request payload transformer or null
	 * @param responseBodyTransformer a response payload transformer or null
	 *
	 * @return a mono emitting the exchange once response headers has been received
	 */
	<A extends ExchangeContext> Mono<Exchange<A>> send(
			A exchangeContext,
			Method method, 
			String authority, 
			List<Map.Entry<String, String>> headers, 
			String path, 
			Consumer<RequestBodyConfigurator> requestBodyConfigurer,
			Function<Publisher<ByteBuf>, Publisher<ByteBuf>> requestBodyTransformer,
			Function<Publisher<ByteBuf>, Publisher<ByteBuf>> responseBodyTransformer);
	
	/**
	 * <p>
	 * Closes the connection.
	 * </p>
	 * 
	 * @return a mono that completes when the connection is closed
	 */
	Mono<Void> close();
	
	// TODO shutdown gracefully
	
	/**
	 * <p>
	 * A connection handler used to handle connection lifecycle events.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	interface Handler {
		
		/**
		 * <p>
		 * Notifies when a connection upgrade has been received.
		 * </p>
		 * 
		 * <p>
		 * This method is invoked on accepted or rejected upgrade.
		 * </p>
		 * 
		 * @param upgradedConnection the upgraded connection
		 */
		void onUpgrade(HttpConnection upgradedConnection);
		
		/**
		 * <p>
		 * Notifies when connection settings change.
		 * </p>
		 * 
		 * @param maxConcurrentRequests the new amount of maximum concurrent requests
		 */
		void onSettingsChange(long maxConcurrentRequests);
		
		/**
		 * <p>
		 * Notifies when an exchange terminates.
		 * </p>
		 * 
		 * @param exchange the exchange
		 */
		void onExchangeTerminate(Exchange<?> exchange);

		/**
		 * <p>
		 * Notifies when a connection error occurs.
		 * </p>
		 * 
		 * @param t the error
		 */
		void onError(Throwable t);

		/**
		 * <p>
		 * Notifies when the connection is closed.
		 * </p>
		 */
		void onClose();
	}
}
