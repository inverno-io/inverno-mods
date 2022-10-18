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
import reactor.core.publisher.Mono;

/**
 * <p>
 * A WebSocket exchange handler is used to handle WebSocket exchange.
 * </p>
 * 
 * <p>
 * It allows to specify the behaviour of the server when a WebSocket connection has been established between a client and the server.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the type of the exchange context
 * @param <B> the type of WebSocket exchange handled by the handler
 */
@FunctionalInterface
public interface WebSocketExchangeHandler<A extends ExchangeContext, B extends WebSocketExchange<A>> {
	
	/**
	 * <p>
	 * Returns a Mono that defers the processing of the WebSocket exchange.
	 * </p>
	 *
	 * <p>
	 * The server subscribes to the returned Mono and after completion, emits received messages in the inbound frames stream and subscribes to the exchange outbound frames stream to send WebSocket
	 * frames to the client.
	 * </p>
	 *
	 * <p>
	 * By default, returns a Mono that defers the execution of {@link #handle(io.inverno.mod.http.server.ws.WebSocketExchange) }.
	 * </p>
	 * 
	 * @param webSocketExchange the WebSocket exchange to process
	 *
	 * @return an empty mono that completes when the WebSocket exchange has been processed
	 * 
	 * @throws WebSocketException if an error occurs during the processing of the exchange
	 */
	default Mono<Void> defer(B webSocketExchange) throws WebSocketException {
		return Mono.fromRunnable(() -> this.handle(webSocketExchange));
	}
	
	/**
	 * <p>
	 * Processes the specified WebSocket server exchange.
	 * </p>
	 *
	 * <p>
	 * This method is more convenient than {@link #defer(io.inverno.mod.http.server.ws.WebSocketExchange)} when the handling logic does not need to be reactive.
	 * </p>
	 *
	 * @param webSocketExchange the WebSocket exchange to process
	 *
	 * @throws WebSocketException if an error occurs during the processing of the exchange
	 */	
	void handle(B webSocketExchange) throws WebSocketException;
}
