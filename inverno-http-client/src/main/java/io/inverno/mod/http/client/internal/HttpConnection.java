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
import io.inverno.mod.http.client.HttpClientConfiguration;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.util.Optional;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Represents an HTTP client connection.
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
	 * Returns the local socket address of the connection.
	 * </p>
	 * 
	 * @return a socket address
	 */
	SocketAddress getLocalAddress();

	/**
	 * <p>
	 * Returns the certificates that were sent to the remote peer during handshaking.
	 * </p>
	 * 
	 * @return an optional returning the list of local certificates or an empty optional if no certificates were sent.
	 */
	Optional<Certificate[]> getLocalCertificates();

	/**
	 * <p>
	 * Returns the remote socket address of the client or last proxy that opened the connection.
	 * </p>
	 * 
	 * @return a socket address
	 */
	SocketAddress getRemoteAddress();

	/**
	 * <p>
	 * Returns the certificates that were received from the remote peer during handshaking.
	 * </p>
	 * 
	 * @return an optional returning the list of remote certificates or an empty optional if no certificates were received.
	 */
	Optional<Certificate[]> getRemoteCertificates();
	
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
	 * This basically waits for the inflight exchange to complete before closing the connection.
	 * </p>
	 * 
	 * @return a mono that completes when the connection is closed
	 * 
	 * @see HttpClientConfiguration#graceful_shutdown_timeout() 
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
		 * Notifies when the connection can be recycled.
		 * </p>
		 */
		void recycle();
		
		/**
		 * <p>
		 * Notifies when the connection is closed.
		 * </p>
		 */
		void close();
	}
}
