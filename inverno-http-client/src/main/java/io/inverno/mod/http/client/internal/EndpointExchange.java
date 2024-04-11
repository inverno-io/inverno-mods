/*
 * Copyright 2024 Jeremy Kuhn
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

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.ExchangeInterceptor;
import io.inverno.mod.http.client.HttpClientException;
import io.inverno.mod.http.client.InterceptableExchange;
import io.inverno.mod.http.client.ResetStreamException;
import io.inverno.mod.http.client.Response;
import io.inverno.mod.http.client.ws.WebSocketExchange;
import java.util.Optional;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An {@link Exchange} implementation representing the HTTP client exchange created from an endpoint and used to populate the request and send it or open a WebSocket.
 * </p>
 * 
 * <p>
 * This implementation exposes a mutable request and returns the response as a publisher which must be subscribed to actually send the request. In a similar way, it exposed the WebSocket exchange 
 * which must be subscribed to open a WebSocket.
 * </p>
 * 
 * <p>
 * Once the request has been sent, the exchange becomes a proxy for the connected exchange and it is no longer possible to modify the request resulting in {@link IllegalStateException} on such 
 * operations. 
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.8
 */
public class EndpointExchange<A extends ExchangeContext> implements Exchange<A> {

	private final AbstractEndpoint<A> endpoint;
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	private final A exchangeContext;
	private final EndpointRequest request;
	private final ExchangeInterceptor<? super A, InterceptableExchange<A>> exchangeInterceptor;
	
	private HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse> connectedExchange;
	private WebSocketConnectionExchange<A> connectectWSExchange;
	
	private Long resetCode;
	private Optional<Throwable> cancelCause;
	
	/**
	 * <p>
	 * Creates an Endpoint exchange.
	 * </p>
	 *
	 * @param endpoint            the endpoint
	 * @param headerService       the header service
	 * @param parameterConverter  the parameter converter
	 * @param exchangeContext     the exchange context
	 * @param request             the request
	 * @param exchangeInterceptor an optional exchange interceptor
	 */
	public EndpointExchange(AbstractEndpoint<A> endpoint, 
		HeaderService headerService, 
		ObjectConverter<String> parameterConverter, 
		A exchangeContext, 
		EndpointRequest request, 
		ExchangeInterceptor<? super A, InterceptableExchange<A>> exchangeInterceptor) {
		this.endpoint = endpoint;
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		this.exchangeContext = exchangeContext;
		this.request = request;
		this.exchangeInterceptor = exchangeInterceptor;
	}
	
	/**
	 * <p>
	 * Injects the HTTP connection exchange after the request has been sent.
	 * </p>
	 * 
	 * @param connectedExchange the connected exchange
	 */
	public void setConnectedExchange(HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse> connectedExchange) {
		this.request.setConnectedRequest(connectedExchange.request());
		this.connectedExchange = connectedExchange;
	}
	
	/**
	 * <p>
	 * Injects the WebSocket connection exchange after the request has been sent.
	 * </p>
	 * 
	 * @param connectectWSExchange the connected WebSocket exchange
	 */
	public void setConnectedExchange(WebSocketConnectionExchange<A> connectectWSExchange) {
		this.request.setConnectedRequest(connectectWSExchange.request());
		this.connectectWSExchange = connectectWSExchange;
	}
	
	@Override
	public HttpVersion getProtocol() {
		return this.connectedExchange != null ? this.connectedExchange.getProtocol() : this.connectectWSExchange != null ? HttpVersion.HTTP_1_1 : HttpVersion.HTTP;
	}

	@Override
	public A context() {
		return this.exchangeContext;
	}

	@Override
	public EndpointRequest request() {
		return this.request;
	}
	
	@Override
	public Mono<? extends Response> response() {
		return Mono.defer(() -> {
			if(this.resetCode != null) {
				return Mono.empty();
			}
			else if(this.exchangeInterceptor != null) {
				EndpointInterceptableExchange<A> interceptableExchange = new EndpointInterceptableExchange<>(this.headerService, this.parameterConverter, this);
				return this.exchangeInterceptor.intercept(interceptableExchange)
					.flatMap(ign -> this.endpoint.connection()
						.flatMap(connection -> connection.send(this))
						.map(exchange -> {
							this.setConnectedExchange(exchange);
							return (Response)exchange.response();
						})
					)
					.switchIfEmpty(Mono.just(interceptableExchange.response()))
					.doOnNext(response -> interceptableExchange.response().setConnectedResponse(response));
			}
			else {
				return this.endpoint.connection()
					.flatMap(connection -> connection.send(this))
					.map(exchange -> {
						this.setConnectedExchange(exchange);
						return exchange.response();
					});
			}
		});
	}

	@Override
	public Mono<? extends WebSocketExchange<A>> webSocket(String subProtocol) {
		return Mono.defer(() -> {
			if(this.resetCode != null) {
				return Mono.empty();
			}
			else if(this.exchangeInterceptor != null) {
				EndpointInterceptableExchange<A> interceptableExchange = new EndpointInterceptableExchange<>(this.headerService, this.parameterConverter, this);
				return this.exchangeInterceptor.intercept(interceptableExchange)
					.flatMap(ign -> this.endpoint.webSocketConnection()
						.flatMap(wsConnection -> wsConnection.handshake(this, subProtocol))
					)
					.doOnNext(exchange -> {
						this.setConnectedExchange(exchange);
						// Make sure the interceptable response is flagged as connected from now on
						interceptableExchange.response().setConnectedResponse(interceptableExchange.response());
					})
					.switchIfEmpty(Mono.error(() -> new HttpClientException("Can't open WebSocket on an intercepted exchange")));
			}
			else {
				return this.endpoint.webSocketConnection()
					.flatMap(wsConnection -> wsConnection.handshake(this, subProtocol))
					.doOnNext(this::setConnectedExchange);
			}
		});
	}
	
	@Override
	public void reset(long code) {
		if(this.connectedExchange != null) {
			this.connectedExchange.reset(code);
		}
		else if(this.connectectWSExchange != null) {
			// this is a noop a WS exchange is closed not canceled
		}
		else {
			this.resetCode = code;
		}
	}

	@Override
	public Optional<Throwable> getCancelCause() {
		if(this.cancelCause != null) {
			return this.cancelCause;
		}
		
		if(this.connectedExchange != null) {
			return this.connectedExchange.getCancelCause();
		}
		else if(this.connectectWSExchange != null) {
			return Optional.empty();
		}
		else if(this.resetCode != null) {
			this.cancelCause = Optional.of(new ResetStreamException(this.resetCode));
			return this.cancelCause;
		}
		return Optional.empty();
	}
}
