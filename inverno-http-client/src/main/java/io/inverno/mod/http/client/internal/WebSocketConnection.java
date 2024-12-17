/*
 * Copyright 2023 Jeremy KUHN
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
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.util.Optional;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Represents an WebSocket connection to an endpoint.
 * </p>
 * 
 * <p>
 * It is used to establish WebSocket connection with the endpoint.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public interface WebSocketConnection {

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
	 * Returns the underlying HTTP protocol. 
	 * <p>
	 * 
	 * <p>
	 * This should always return HTTP/1.1.
	 * </p>
	 * 
	 * @return the underlying HTTP protocol version
	 */
	default HttpVersion getProtocol() {
		return HttpVersion.HTTP_1_1;
	}
	
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
	 * Performs WebSocket handshake.
	 * </p>
	 *
	 * @param <A>              the exchange context type
	 * @param endpointExchange the endpoint exchange
	 *
	 * @return a mono emitting the WebSocket exchange if the handshake was successful
	 */
	<A extends ExchangeContext> Mono<WebSocketConnectionExchange<A>> handshake(EndpointExchange<A> endpointExchange, String subprotocol);
	
	/**
	 * <p>
	 * Closes the connection.
	 * </p>
	 * 
	 * @return a mono that completes when the connection is closed
	 */
	Mono<Void> close();
}
