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
package io.inverno.mod.http.server.internal.http1x;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.ws.WebSocketStatus;
import io.inverno.mod.http.server.HttpServerConfiguration;
import io.inverno.mod.http.server.internal.http1x.ws.GenericWebSocketExchange;
import io.inverno.mod.http.server.ws.WebSocket;
import io.inverno.mod.http.server.ws.WebSocketExchange;
import io.inverno.mod.http.server.ws.WebSocketExchangeHandler;
import org.reactivestreams.Subscription;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;

/**
 * <p>
 * HTTP/1.x {@link WebSocket} implementation.
 * </p>
 * 
 * <p>
 * Note that WebSocket upgrade is only supported by HTTP/1.x protocol.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
class Http1xWebSocket implements WebSocket<ExchangeContext, WebSocketExchange<ExchangeContext>> {
	
	private final Http1xConnection connection;
	private final Http1xExchange exchange;
	private final String[] subProtocols;
	
	private WebSocketExchangeHandler<? super ExchangeContext, WebSocketExchange<ExchangeContext>> handler;
	private Mono<Void> fallback;
	
	private Disposable disposable;
	
	private GenericWebSocketExchange webSocketExchange;
	
	/**
	 * <p>
	 * Creates an HTTP/1.x WebSocket.
	 * </p>
	 *
	 * @param configuration  the server configuration
	 * @param request        the original HTTP/1.x exchange
	 * @param connection     the HTTP/1.x connection
	 * @param request        the original HTTP/1.x exchange
	 * @param subProtocols   the list of supported subprotocols
	 */
	public Http1xWebSocket(HttpServerConfiguration configuration, Http1xConnection connection, Http1xExchange exchange, String[] subProtocols) {
		this.connection = connection;
		this.exchange = exchange;
		this.subProtocols = subProtocols;
	}
	
	/**
	 * <p>
	 * Tries to upgrade the Http connection to a WebSocket connection.
	 * </p>
	 * 
	 * <p>
	 * This method executes on the connection event loop.
	 * </p>
	 */
	void connect() {
		if(this.connection.executor().inEventLoop()) {
			this.connection.writeWebSocketHandshake(this.subProtocols).subscribe(new Http1xWebSocket.WebSocketHandshakeSubscriber());
		}
		else {
			this.connection.executor().execute(this::connect);
		}
	}
	
	/**
	 * <p>
	 * Disposes the WebSocket connection.
	 * </p>
	 * 
	 * <p>
	 * This method dispose the connection upgrade disposal if any and close the WebSocket exchange if any.
	 * </p>
	 * 
	 * @param cause an error or null if disposal does not result from an error (e.g. shutdown) 
	 */
	void dispose(Throwable cause) {
		if(this.disposable != null) {
			this.disposable.dispose();
		}
		if(this.webSocketExchange != null) {
			if(cause == null) {
				this.webSocketExchange.close();
			}
			else {
				this.webSocketExchange.close(WebSocketStatus.INTERNAL_SERVER_ERROR, cause.getMessage());
			}
		}
	}
	
	/**
	 * <p>
	 * Returns the fallback mono to subscribe in case the opening handshake failed.
	 * </p>
	 * 
	 * @return a fallback handler
	 */
	public Mono<Void> getFallback() {
		return fallback;
	}

	@Override
	public WebSocket<ExchangeContext, WebSocketExchange<ExchangeContext>> handler(WebSocketExchangeHandler<? super ExchangeContext, WebSocketExchange<ExchangeContext>> handler) {
		this.handler = handler;
		return this;
	}

	@Override
	public WebSocket<ExchangeContext, WebSocketExchange<ExchangeContext>> or(Mono<Void> fallback) {
		this.fallback = fallback;
		return this;
	}
	
	/**
	 * <p>
	 * The WebSocket handshake subscriber that start the WebSocket exchange on complete.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.10
	 */
	private class WebSocketHandshakeSubscriber extends BaseSubscriber<GenericWebSocketExchange> {
		
		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			Http1xWebSocket.this.disposable = this;
			subscription.request(1);
		}

		@Override
		protected void hookOnNext(GenericWebSocketExchange value) {
			Http1xWebSocket.this.webSocketExchange = value;
		}
		
		@Override
		protected void hookOnComplete() {
			// We finished the upgrade, we can log the result
			Http1xWebSocket.this.exchange.response().headers(headers -> headers.status(Status.SWITCHING_PROTOCOLS));
			Http1xWebSocket.this.disposable = null;
			Http1xWebSocket.this.exchange.request().dispose(null);
			Http1xWebSocket.this.exchange.response().dispose(null);
			Http1xWebSocket.this.webSocketExchange.start(Http1xWebSocket.this.handler);
			// TODO log + dispose next exchanges if any
		}

		@Override
		protected void hookOnError(Throwable throwable) {
			Http1xWebSocket.this.exchange.handleWebSocketHandshakeError(throwable, Http1xWebSocket.this.fallback);
		}
	}
}
