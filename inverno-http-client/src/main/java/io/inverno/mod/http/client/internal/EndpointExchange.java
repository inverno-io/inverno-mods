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
import io.inverno.mod.http.client.Endpoint;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.ExchangeInterceptor;
import io.inverno.mod.http.client.HttpClientException;
import io.inverno.mod.http.client.InterceptedExchange;
import io.inverno.mod.http.client.ResetStreamException;
import io.inverno.mod.http.client.Response;
import io.inverno.mod.http.client.UnboundExchange;
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
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.8
 */
public class EndpointExchange<A extends ExchangeContext> implements UnboundExchange<A> {

	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	private final A exchangeContext;
	private final EndpointRequest request;

	private AbstractEndpoint<A> endpoint;
	private ExchangeInterceptor<A, InterceptedExchange<A>> exchangeInterceptor;

	private HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse> connectedExchange;
	private WebSocketConnectionExchange<A> connectedWSExchange;
	
	private Long resetCode;

	/**
	 * <p>
	 * Creates an unbound Endpoint exchange.
	 * </p>
	 *
	 * @param headerService       the header service
	 * @param parameterConverter  the parameter converter
	 * @param exchangeContext     the exchange context
	 * @param request             the request
	 */
	public EndpointExchange(
			HeaderService headerService,
			ObjectConverter<String> parameterConverter,
			A exchangeContext,
			EndpointRequest request) {
		this(headerService, parameterConverter, null, exchangeContext, request);
	}

	/**
	 * <p>
	 * Creates a Endpoint exchange bound to the specified endpoint.
	 * </p>
	 *
	 * @param headerService       the header service
	 * @param parameterConverter  the parameter converter
	 * @param endpoint            the endpoint
	 * @param exchangeContext     the exchange context
	 * @param request             the request
	 */
	public EndpointExchange(
		HeaderService headerService,
		ObjectConverter<String> parameterConverter,
		AbstractEndpoint<A> endpoint,
		A exchangeContext,
		EndpointRequest request) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		this.endpoint = endpoint;
		this.exchangeContext = exchangeContext;
		this.request = request;
	}

	@Override
	@SuppressWarnings("unchecked")
	public UnboundExchange<A> intercept(ExchangeInterceptor<? super A, ? extends InterceptedExchange<A>> interceptor) {
		if(this.exchangeInterceptor == null) {
			this.exchangeInterceptor = (ExchangeInterceptor<A, InterceptedExchange<A>>)interceptor;
		}
		else {
			this.exchangeInterceptor = this.exchangeInterceptor.andThen((ExchangeInterceptor<A, InterceptedExchange<A>>)interceptor);
		}
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Exchange<A> bind(Endpoint<? super A> endpoint) throws IllegalArgumentException, IllegalStateException {
		if(this.endpoint != null) {
			throw new IllegalArgumentException("Exchange already bound");
		}
		if(!(endpoint instanceof AbstractEndpoint)) {
			throw new IllegalArgumentException("Invalid endpoint which was not created with provided HttpClient");
		}
		this.endpoint = (AbstractEndpoint<A>)endpoint;
		return this;
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
	 * @param connectedWSExchange the connected WebSocket exchange
	 */
	public void setConnectedExchange(WebSocketConnectionExchange<A> connectedWSExchange) {
		this.request.setConnectedRequest(connectedWSExchange.request());
		this.connectedWSExchange = connectedWSExchange;
	}
	
	@Override
	public HttpVersion getProtocol() {
		return this.connectedExchange != null ? this.connectedExchange.getProtocol() : this.connectedWSExchange != null ? HttpVersion.HTTP_1_1 : HttpVersion.HTTP;
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
	public Mono<? extends Response> response() throws IllegalStateException {
		return Mono.defer(() -> {
			if(this.endpoint == null) {
				throw new IllegalStateException("Exchange is not bound to an endpoint");
			}
			if(this.resetCode != null) {
				return Mono.empty();
			}
			else if(this.endpoint.getExchangeInterceptor() != null || this.exchangeInterceptor != null) {
				ExchangeInterceptor<A, InterceptedExchange<A>> interceptor;
				if(this.endpoint.getExchangeInterceptor() != null) {
					interceptor = this.exchangeInterceptor != null ? this.endpoint.getExchangeInterceptor().andThen(this.exchangeInterceptor) : this.endpoint.getExchangeInterceptor();
				}
				else {
					interceptor = this.exchangeInterceptor;
				}

				EndpointInterceptedExchange<A> interceptedExchange = new EndpointInterceptedExchange<>(this.headerService, this.parameterConverter, this);
				return interceptor.intercept(interceptedExchange)
					.flatMap(ign -> this.endpoint.connection()
						.flatMap(connection -> connection.send(this))
						.map(exchange -> {
							this.setConnectedExchange(exchange);
							return (Response)exchange.response();
						})
					)
					.switchIfEmpty(Mono.just(interceptedExchange.response()))
					.doOnNext(response -> interceptedExchange.response().setConnectedResponse(response));
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
	public Mono<? extends WebSocketExchange<A>> webSocket(String subProtocol) throws IllegalStateException {
		return Mono.defer(() -> {
			if(this.endpoint == null) {
				throw new IllegalStateException("Exchange is not bound to an endpoint");
			}
			if(this.resetCode != null) {
				return Mono.empty();
			}
			else if(endpoint.getExchangeInterceptor() != null) {
				EndpointInterceptedExchange<A> interceptedExchange = new EndpointInterceptedExchange<>(this.headerService, this.parameterConverter, this);
				return endpoint.getExchangeInterceptor().intercept(interceptedExchange)
					.flatMap(ign -> this.endpoint.webSocketConnection()
						.flatMap(wsConnection -> wsConnection.handshake(this, subProtocol))
					)
					.doOnNext(exchange -> {
						this.setConnectedExchange(exchange);
						// Make sure the intercepted response is flagged as connected from now on
						interceptedExchange.response().setConnectedResponse(interceptedExchange.response());
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
		else if(this.connectedWSExchange == null) {
			this.resetCode = code;
		}
		// otherwise this is a noop a WS exchange is closed not canceled
	}

	@Override
	public Optional<Throwable> getCancelCause() {
		if(this.resetCode != null) {
			return Optional.of(new ResetStreamException(this.resetCode));
		}
		if(this.connectedExchange != null) {
			return this.connectedExchange.getCancelCause();
		}
		return Optional.empty();
	}
}
