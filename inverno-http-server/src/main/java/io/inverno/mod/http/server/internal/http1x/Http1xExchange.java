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
package io.inverno.mod.http.server.internal.http1x;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.internal.header.HeadersValidator;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.HttpServerConfiguration;
import io.inverno.mod.http.server.Part;
import io.inverno.mod.http.server.ServerController;
import io.inverno.mod.http.server.internal.multipart.MultipartDecoder;
import io.inverno.mod.http.server.ws.WebSocket;
import io.inverno.mod.http.server.ws.WebSocketExchange;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import java.util.Optional;
import org.reactivestreams.Subscription;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Http/1.x {@link Exchange} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class Http1xExchange extends AbstractHttp1xExchange {
	
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	private final HeadersValidator headersValidator;
	
	private final ExchangeContext context;
	private final Http1xRequest request;
	private final Http1xResponse response;

	private final boolean keepAlive;
	
	private Http1xWebSocket webSocket;
	
	private Disposable disposable;
	
	/**
	 * <p>
	 * Creates an Http/1.x exchange.
	 * </p>
	 * 
	 * @param configuration         the server configuration
	 * @param controller            the server controller
	 * @param headerService         the header service
	 * @param parameterConverter    the parameter converter
	 * @param urlEncodedBodyDecoder the application/x-www-form-urlencoded body decoder
	 * @param multipartBodyDecoder  the multipart/form-data body decoder
	 * @param headersValidator      the header validator
	 * @param connection            the Http/1.x connection
	 * @param request               the originating HTTP request
	 */
	public Http1xExchange(
			HttpServerConfiguration configuration, 
			ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>> controller, 
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter, 
			MultipartDecoder<Parameter> urlEncodedBodyDecoder, 
			MultipartDecoder<Part> multipartBodyDecoder, 
			HeadersValidator headersValidator,
			Http1xConnection connection,
			HttpRequest request
		) {
		super(configuration, controller, connection, request.method() == HttpMethod.HEAD, request.protocolVersion());
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		this.headersValidator = headersValidator;
		
		this.context = controller.createContext();
		if(this.context != null) {
			this.context.init();
		}
		this.request = new Http1xRequest(headerService, parameterConverter, urlEncodedBodyDecoder, multipartBodyDecoder, connection, request);
		this.response = new Http1xResponse(headerService, parameterConverter, headersValidator, connection, this.version, this.head);

		if(this.version == io.netty.handler.codec.http.HttpVersion.HTTP_1_1) {
			if(request.headers().contains(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE, true)) {
				this.keepAlive = false;
				this.response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
			}
			else {
				this.keepAlive = true;
			}
		}
		else if(request.headers().contains(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE, true)) {
			this.keepAlive = true;
			this.response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
		}
		else {
			this.keepAlive = false;
		}
	}

	@Override
	Http1xExchange unwrap() {
		return this;
	}

	@Override
	boolean isKeepAlive() {
		return this.keepAlive;
	}

	@Override
	public void start() {
		try {
			this.controller.defer(this).subscribe(this);
		}
		catch(Throwable throwable) {
			this.handleError(throwable);
		}
	}

	@Override
	public void handleError(Throwable throwable) {
		if(this.response.headers().isWritten()) {
			this.dispose(throwable);
			this.connection.shutdown().subscribe();
		}
		else {
			this.createErrorExchange(throwable).start();
		}
	}
	
	/**
	 * <p>
	 * Handles WebSocket handshake errors.
	 * </p>
	 * 
	 * <p>
	 * If present the fallback handler is used as an exchange handler to re-process the exchange, otherwise the error is handled normally.
	 * </p>
	 * 
	 * @param throwable the error
	 * @param fallback  a fallback handler
	 */
	public void handleWebSocketHandshakeError(Throwable throwable, Mono<Void> fallback) {
		if(this.connection.executor().inEventLoop()) {
			if(fallback != null) {
				fallback.subscribe(new Http1xExchange.ExchangeHandlerSubscriber());
			}
			else {
				this.handleError(throwable);
			}
		}
		else {
			this.connection.executor().execute(() -> this.handleWebSocketHandshakeError(throwable, fallback));
		}
	}
	
	@Override
	public Http1xErrorExchange createErrorExchange(Throwable throwable) {
		return new Http1xErrorExchange(this, new Http1xResponse(this.headerService, this.parameterConverter, this.headersValidator, this.connection, this.version, this.head), throwable);
	}

	@Override
	protected void doDispose(Throwable cause) {
		if(this.disposable != null) {
			this.disposable.dispose();
		}
		this.request.dispose(cause);
		this.response.dispose(cause);
		if(this.webSocket != null) {
			this.webSocket.dispose(cause);
		}
	}

	@Override
	public ExchangeContext context() {
		return this.context;
	}

	@Override
	public Http1xRequest request() {
		return this.request;
	}

	@Override
	public Http1xResponse response() {
		return this.response;
	}
	
	@Override
	public Optional<? extends WebSocket<ExchangeContext, ? extends WebSocketExchange<ExchangeContext>>> webSocket(String... subprotocols) {
		if(this.configuration.ws_enabled()) {
			this.webSocket = new Http1xWebSocket(this.configuration, this.connection, this, subprotocols);
			return Optional.of(this.webSocket);
		}
		return Optional.empty();
	}


	@Override
	protected void hookOnSubscribe(Subscription subscription) {
		this.disposable = this;
		subscription.request(1);
	}

	@Override
	protected void hookOnComplete() {
		if(this.reset) {
			return;
		}
		if(this.webSocket == null) {
			this.response.send();
		}
		else {
			this.webSocket.connect();
		}
	}

	@Override
	protected void hookOnError(Throwable throwable) {
		if(!this.reset) {
			this.connection.onExchangeError(throwable);
		}
	}

	/**
	 * <p>
	 * The subscriber used to subscribe to the mono returned by the exchange handler and that sends the response on complete.
	 * </p>
	 *
	 * <p>
	 * This is actually only used when fall backing a failed WebSocket handshake.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.10
	 * 
	 * @see #handleWebSocketHandshakeError(Throwable, Mono)
	 */
	private class ExchangeHandlerSubscriber extends BaseSubscriber<Void> {

		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			Http1xExchange.this.hookOnSubscribe(subscription);
		}

		@Override
		protected void hookOnComplete() {
			Http1xExchange.this.hookOnComplete();
		}

		@Override
		protected void hookOnError(Throwable throwable) {
			Http1xExchange.this.hookOnError(throwable);
		}
	}
}