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
package io.inverno.mod.web.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ws.WebSocket;
import io.inverno.mod.web.Web2SocketExchange;
import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.WebRequest;
import io.inverno.mod.web.WebResponse;
import java.util.Optional;
import java.util.function.Consumer;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link WebExchange} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see WebRequest
 * @see WebResponse
 */
class GenericWebExchange implements WebExchange<ExchangeContext> {

	private final Exchange<ExchangeContext> exchange;
	
	private final GenericWebRequest request;
	
	private final GenericWebResponse response;
	
	private final Consumer<Mono<Void>> finalizerConsumer;
	
	private final DataConversionService dataConversionService;
	
	/**
	 * <p>
	 * Creates a generic web exchange with the specified request and response.
	 * </p>
	 * 
	 * @param exchange          the original exchange
	 * @param request           a web request
	 * @param response          a web response
	 * @param finalizerSupplier the deferred exchange finalizer
	 */
	public GenericWebExchange(Exchange<ExchangeContext> exchange, GenericWebRequest request, GenericWebResponse response, Consumer<Mono<Void>> finalizerConsumer, DataConversionService dataConversionService) {
		this.exchange = exchange;
		this.request = request;
		this.response = response;
		this.finalizerConsumer = finalizerConsumer;
		this.dataConversionService = dataConversionService;
	}

	@Override
	public HttpVersion getProtocol() {
		return this.exchange.getProtocol();
	}

	@Override
	public GenericWebRequest request() {
		return this.request;
	}

	@Override
	public GenericWebResponse response() {
		return this.response;
	}
	
	@Override
	public Optional<? extends WebSocket<ExchangeContext, Web2SocketExchange<ExchangeContext>>> webSocket(String... subProtocols) {
		return this.exchange.webSocket(subProtocols).map(webSocket -> new GenericWeb2Socket(webSocket, this.request, this.dataConversionService));
	}
	
	@Override
	public ExchangeContext context() {
		return this.exchange.context();
	}
	
	@Override
	public void finalizer(Mono<Void> finalizer) {
		this.finalizerConsumer.accept(finalizer);
	}
}
