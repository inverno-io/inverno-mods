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
package io.inverno.mod.http.server.ws;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.ws.BaseWebSocketExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.Request;

/**
 * <p>
 * Represents a WebSocket exchange between a client and the server.
 * </p>
 * 
 * <p>
 * A WebSocket exchange is bidirectional and as a result provided an {@link Inbound} and an {@link Outbound} exposing WebSocket frames and messages respectively received and sent by the server.
 * </p>
 * 
 * <p>
 * A WebSocket exchange is created from the HTTP server exchange (see {@link Exchange#webSocket(java.lang.String...) }) once the WebSocket opening handshake has completed. It is processed in a
 * {@link WebSocketExchangeHandler} specified in the {@link WebSocket}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see WebSocket
 * @see WebSocketExchangeHandler
 * 
 * @param <A> the type of the exchange context
 */
public interface WebSocketExchange<A extends ExchangeContext> extends BaseWebSocketExchange<A> {
	
	/**
	 * <p>
	 * Returns the original HTTP upgrade request.
	 * </p>
	 * 
	 * @return the HTTP request
	 */
	Request request();
}
