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
package io.inverno.mod.http.server.internal.http2;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.HttpServerConfiguration;
import io.inverno.mod.http.server.Part;
import io.inverno.mod.http.server.ServerController;
import io.inverno.mod.http.server.internal.multipart.MultipartDecoder;
import io.inverno.mod.http.server.ws.WebSocket;
import io.inverno.mod.http.server.ws.WebSocketExchange;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Headers;
import java.util.Optional;
import org.reactivestreams.Subscription;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;

/**
 * <p>
 * Http/2 {@link Exchange} implementation.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class Http2Exchange extends AbstractHttp2Exchange {

	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	private final boolean validateHeaders;
	
	private final ExchangeContext context;
	private final Http2Request request;
	private final Http2Response response;
	
	private Disposable disposable;
	
	/**
	 * <p>
	 * Creates an Http/2 exchange.
	 * </p>
	 *
	 * @param configuration         the server configuration
	 * @param controller            the server controller
	 * @param headerService         the header service
	 * @param parameterConverter    the parameter converter
	 * @param urlEncodedBodyDecoder the application/x-www-form-urlencoded body decoder
	 * @param multipartBodyDecoder  the multipart/form-data body decoder
	 * @param validateHeaders       true to validate headers, false otherwise
	 * @param connectionStream      the connection stream
	 * @param headers               the originating headers
	 */
	public Http2Exchange(
			HttpServerConfiguration configuration, 
			ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>> controller,
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter, 
			MultipartDecoder<Parameter> urlEncodedBodyDecoder, 
			MultipartDecoder<Part> multipartBodyDecoder,
			boolean validateHeaders,
			Http2ConnectionStream connectionStream,
			Http2Headers headers
		) {
		super(configuration, controller, connectionStream, headers);
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		this.validateHeaders = validateHeaders;
		
		this.context = controller.createContext();
		if(this.context != null) {
			this.context.init();
		}
		this.request = new Http2Request(headerService, parameterConverter, urlEncodedBodyDecoder, multipartBodyDecoder, connectionStream, headers);
		this.response = new Http2Response(headerService, parameterConverter, validateHeaders, connectionStream, this.head);
	}
	
	@Override
	public void start() {
		this.connectionStream.exchange = this;
		try {
			this.controller.defer(this).subscribe(new Http2Exchange.ExchangeHandlerSubscriber());
		}
		catch(Throwable throwable) {
			this.handleError(throwable);
		}
	}
	
	@Override
	public void handleError(Throwable throwable) {
		if(this.response.headers().isWritten()) {
			this.dispose(throwable);
			this.connectionStream.resetStream(Http2Error.INTERNAL_ERROR.code());
		}
		else {
			this.createErrorExchange(throwable).start();
		}
	}
	
	@Override
	public Http2ErrorExchange createErrorExchange(Throwable throwable) {
		return new Http2ErrorExchange(this, new Http2Response(this.headerService, this.parameterConverter, this.validateHeaders, this.connectionStream, this.head), throwable);
	}
	
	@Override
	protected void doDispose(Throwable cause) {
		if(this.disposable != null) {
			this.disposable.dispose();
		}
		this.request.dispose(cause);
		this.response.dispose(cause);
	}

	@Override
	public ExchangeContext context() {
		return this.context;
	}

	@Override
	public Http2Request request() {
		return this.request;
	}

	@Override
	public Http2Response response() {
		return this.response;
	}
	
	@Override
	public Optional<? extends WebSocket<ExchangeContext, ? extends WebSocketExchange<ExchangeContext>>> webSocket(String... subProtocols) {
		return Optional.empty();
	}

	/**
	 * <p>
	 * The subscriber used to subscribe to the mono returned by the exchange handler and that sends the response on complete.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.10
	 */
	private class ExchangeHandlerSubscriber extends BaseSubscriber<Void> {

		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			Http2Exchange.this.disposable = this;
			super.hookOnSubscribe(subscription);
		}

		@Override
		protected void hookOnComplete() {
			if(!Http2Exchange.this.reset) {
				Http2Exchange.this.response.send();
			}
		}

		@Override
		protected void hookOnError(Throwable throwable) {
			if(!Http2Exchange.this.reset) {
				Http2Exchange.this.connectionStream.onExchangeError(throwable);
			}
		}
	}
}
