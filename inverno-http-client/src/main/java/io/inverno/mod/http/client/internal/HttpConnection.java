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
	 * Sends a request to the connected endpoint.
	 * </p>
	 *
	 * @param <A>              the exchange context type
	 * @param endpointExchange the endpoint exchange
	 *
	 * @return a mono emitting the connected exchange once response headers has been received
	 */
	<A extends ExchangeContext> Mono<HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> send(EndpointExchange<A> endpointExchange);
	
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
		void onExchangeTerminate(HttpConnectionExchange<?, ?, ?> exchange);

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
