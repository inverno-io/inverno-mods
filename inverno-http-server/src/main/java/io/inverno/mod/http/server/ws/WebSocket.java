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
import io.inverno.mod.http.server.Exchange;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A server-side WebSocket.
 * </p>
 * 
 * <p>
 * A WebSocket is created by upgrading to the WebSocket protocol using {@link Exchange#webSocket(java.lang.String...) }. It is used to specify the {@link WebSocketExchangeHandler} which handles the
 * {@link WebSocketExchange} once the opening handshake has been completed.
 * </p>
 * 
 * <p>
 * A fallback action can also be specified which is executed if the opening handshake failed.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the type of the exchange context
 * @param <B> the type of WebSocket exchange
 */
public interface WebSocket<A extends ExchangeContext, B extends WebSocketExchange<A>> {

	/**
	 * <p>
	 * Specifies the handler used to handle the WebSocket exchange.
	 * </p>
	 * 
	 * @param handler a WebSocket exchange handler
	 * 
	 * @return this WebSocket
	 */
	WebSocket<A, B> handler(WebSocketExchangeHandler<? super A, B> handler);
	
	/**
	 * <p>
	 * Specifies the fallback action to execute when the opening handshake fails.
	 * </p>
	 * 
	 * <p>
	 * Unlike {@link #or(reactor.core.publisher.Mono)}, the specified fallback action is executed synchronously and might block the I/O thread.
	 * </p>
	 * 
	 * <p>
	 * This is a convenience method which delegates to {@link #or(reactor.core.publisher.Mono) } which should be prefered.
	 * </p>
	 * 
	 * @param fallback a fallback action
	 * 
	 * @return this WebSocket
	 */
	default WebSocket<A, B> or(Runnable fallback) {
		return this.or(Mono.fromRunnable(fallback));
	}
	
	/**
	 * <p>
	 * Specifies a fallback action to execute when the opening handshake fails.
	 * </p>
	 * 
	 * <p>
	 * Unlike {@link #or(java.lang.Runnable) }, the specified fallback action is executed asynchronously and will not block the I/O thread.
	 * </p>
	 * 
	 * @param fallback a fallback action
	 * 
	 * @return this WebSocket
	 */
	WebSocket<A, B> or(Mono<Void> fallback);
}
