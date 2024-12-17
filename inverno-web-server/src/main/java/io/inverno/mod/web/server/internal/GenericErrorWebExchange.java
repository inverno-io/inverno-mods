/*
 * Copyright 2021 Jeremy Kuhn
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
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.web.server.ErrorWebExchange;
import java.util.Optional;

/**
 * <p>
 * Generic {@link ErrorWebExchange} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class GenericErrorWebExchange<A extends ExchangeContext> implements ErrorWebExchange<A> {

	private final ServerDataConversionService dataConversionService;
	private final ObjectConverter<String> parameterConverter;
	private final ErrorExchange<A> exchange;
	private final GenericWebResponse response;

	private GenericWebRequest request;

	/**
	 * <p>
	 * Creates a generic error Web exchange.
	 * </p>
	 *
	 * @param dataConversionService the data conversion service
	 * @param parameterConverter    the parameter converter
	 * @param exchange              the original error exchange
	 */
	public GenericErrorWebExchange(ServerDataConversionService dataConversionService, ObjectConverter<String> parameterConverter, ErrorExchange<A> exchange) {
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
	public void reset(long code) {
		this.exchange.reset(code);
	}

	@Override
	public Optional<Throwable> getCancelCause() {
		return this.exchange.getCancelCause();
	}

	@Override
	public Throwable getError() {
		return this.exchange.getError();
	}
}
