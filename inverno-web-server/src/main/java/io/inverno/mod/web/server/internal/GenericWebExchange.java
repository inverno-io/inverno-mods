/*
 * Copyright 2020 Jeremy Kuhn
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
package io.inverno.mod.web.server.internal;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ws.WebSocket;
import io.inverno.mod.web.server.WebExchange;
import io.inverno.mod.web.server.internal.ws.GenericWeb2Socket;
import io.inverno.mod.web.server.ws.Web2SocketExchange;
import java.util.Optional;

/**
 * <p>
 * Generic {@link WebExchange} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see io.inverno.mod.web.server.WebRequest
 * @see io.inverno.mod.web.server.WebResponse
 *
 * @param <A> the exchange context type
 */
public class GenericWebExchange<A extends ExchangeContext> implements WebExchange<A> {

	private final ServerDataConversionService dataConversionService;
	private final ObjectConverter<String> parameterConverter;
	private final Exchange<A> exchange;
	private final GenericWebResponse response;

	private GenericWebRequest request;

	/**
	 * <p>
	 * Creates a generic Web exchange.
	 * </p>
	 *
	 * @param dataConversionService the data conversion service
	 * @param parameterConverter    the parameter converter
	 * @param exchange              the originating exchange
	 */
	public GenericWebExchange(ServerDataConversionService dataConversionService, ObjectConverter<String> parameterConverter, Exchange<A> exchange) {
		this.dataConversionService = dataConversionService;
		this.parameterConverter = parameterConverter;
		this.exchange = exchange;
		this.response = new GenericWebResponse(dataConversionService, exchange.response());
	}

	@Override
	public HttpVersion getProtocol() {
		return this.exchange.getProtocol();
	}

	@Override
	public A context() {
		return this.exchange.context();
	}

	@Override
	public GenericWebRequest request() {
		if(this.request == null) {
			this.request = new GenericWebRequest(this.dataConversionService, this.parameterConverter, this.exchange.request());
		}
		return this.request;
	}

	@Override
	public GenericWebResponse response() {
		return this.response;
	}

	@Override
	public Optional<? extends WebSocket<A, Web2SocketExchange<A>>> webSocket(String... subprotocols) {
		return this.exchange.webSocket(subprotocols).map(webSocket -> new GenericWeb2Socket<>(this.dataConversionService, this.request, webSocket));
	}

	@Override
	public void reset(long code) {
		this.exchange.reset(code);
	}

	@Override
	public Optional<Throwable> getCancelCause() {
		return this.exchange.getCancelCause();
	}
}
