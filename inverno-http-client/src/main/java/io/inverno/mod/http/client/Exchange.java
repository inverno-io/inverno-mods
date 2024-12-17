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

import io.inverno.mod.http.base.BaseExchange;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.client.ws.WebSocketExchange;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Represents an HTTP client exchange (request/response) between a client and a server.
 * </p>
 * 
 * <p>
 * An HTTP client exchange is obtained from an {@link Endpoint} and allows to build and send an HTTP request or to open a WebSocket to an HTTP server represented by that endpoint.
 * </p>
 * 
 * <p>
 * The HTTP request is only sent when the exchange's response publisher is subscribed and if it hasn't been intercepted by an {@link ExchangeInterceptor} which is then responsible for providing the 
 * response.
 * </p>
 * 
 * <pre>{@code
 * endpoint.exchange(Method.POST, "/")
 * 	.flatMap(exchange -> {
 *		exchange.request()
 *			.headers(headers -> headers.contentType("text/plain"))
 *			.body().get().string().value("This is a request body");
 *		return exchange.response();
 *   })
 *  .subscribe(response -> {}); // send the request
 * }</pre>
 * 
 * <p>
 * As for an HTTP request, a WebSocket is only created when the WebSocket exchange publisher is subscribed.
 * </p>
 * 
 * <pre>{@code
 * endpoint.exchange("/")
 * 	.flatMap(Exchange::webSocket)
 *  .subscribe(wsExchange -> {}); // create a WebSocket connection
 * }</pre>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @see Endpoint
 * @see Request
 * @see Response
 * @see ExchangeContext
 * 
 * @param <A> the type of the exchange context
 */
public interface Exchange<A extends ExchangeContext> extends BaseExchange<A, Request, Mono<? extends Response>> {

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The connection is created and the request sent when the returned publisher is subscribed.
	 * </p>
	 *
	 * @throws IllegalStateException if the exchange is not bound to an endpoint
	 */
	@Override
	Mono<? extends Response> response() throws IllegalStateException;

	/**
	 * <p>
	 * Returns a WebSocket exchange publisher.
	 * </p>
	 * 
	 * <p>
	 * The WebSocket connection and the corresponding {@link WebSocketExchange} are created when the returned publisher is subscribed.
	 * </p>
	 * 
	 * @return a WebSocket exchange mono
	 *
	 * @throws IllegalStateException if the exchange is not bound to an endpoint
	 */
	default Mono<? extends WebSocketExchange<A>> webSocket() throws IllegalStateException {
		return this.webSocket(null);
	}

	/**
	 * <p>
	 * Returns a WebSocket exchange publisher requesting the specified subprotocol.
	 * </p>
	 * 
	 * <p>
	 * The WebSocket connection and the corresponding {@link WebSocketExchange} are created when the returned publisher is subscribed.
	 * </p>
	 * 
	 * @param subProtocol the subprotocol requested to the server by the client.
	 * 
	 * @return a WebSocket exchange mono
	 *
	 * @throws IllegalStateException if the exchange is not bound to an endpoint
	 */
	Mono<? extends WebSocketExchange<A>> webSocket(String subProtocol) throws IllegalStateException;
}
