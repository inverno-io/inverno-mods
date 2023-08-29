/*
 * Copyright 2020 Jeremy KUHN
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
package io.inverno.mod.http.server;

import io.inverno.mod.http.base.BaseExchange;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.ws.WebSocket;
import io.inverno.mod.http.server.ws.WebSocketExchange;
import java.util.Optional;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Represents an HTTP server exchange (request/response) between a client and a server.
 * </p>
 * 
 * <p>
 * An HTTP server exchange is created when a {@link Request} is received from a client by the HTTP server which then processes the exchange in the {@link ServerController}.
 * </p>
 *
 * <p>
 * When creating the exchange, the HTTP server also invokes {@link ServerController#createContext()} to create an exchange context and attach it to the new server exchange.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see Request
 * @see Response
 * @see ExchangeContext
 *
 * @param <A> the type of the exchange context
 */
public interface Exchange<A extends ExchangeContext> extends BaseExchange<A> {

	@Override
	Request request();
	
	@Override
	Response response();
	
	/**
	 * <p>
	 * Upgrades the exchange to a WebSocket exchange.
	 * </p>
	 * 
	 * <p>
	 * If the exchange cannot upgrade to the WebSocket protocol, an empty optional shall be returned. For instance, if the state of the exchange prevents the upgrade (e.g. error exchange) or if the
	 * underlying HTTP protocol does not support the upgrade operation. Currently only HTTP/1.1 can upgrade to the WebSocket protocol.
	 * </p>
	 *
	 * @param subProtocols a list of supported subprotocols negotiated during the handshake
	 * 
	 * @return an optional returning the WebSocket or an empty optional if the upgrade is not possible
	 */
	Optional<? extends WebSocket<A, ? extends WebSocketExchange<A>>> webSocket(String... subProtocols);
	
	/**
	 * <p>
	 * Specifies a finalizer which completes once the exchange is fully processed.
	 * </p>
	 *
	 * <p>
	 * An exchange is considered fully processed when the last chunk of the response has been fully sent to the client or following a terminal error.
	 * </p>
	 *
	 * <p>
	 * Note that using a finalizer actually impacts HTTP pipelining since the server wait for the response to be fully sent to the client before processing subsequent requests.
	 * </p>
	 * 
	 * <p>
	 * When invoked multiple time this method chains the finalizer one after the other.
	 * </p>
	 *
	 * @param finalizer a finalizer
	 */
	void finalizer(Mono<Void> finalizer);
}
