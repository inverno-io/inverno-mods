/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.web.server;


import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.server.ErrorExchange;
import java.util.Optional;
import java.util.function.Function;

/**
 * <p>
 * An error exchange that extends HTTP server {@link ErrorExchange}.
 * </p>
 *
 * <p>
 * It especially provides response body encoding based on the response content type.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @param <A> the type of the exchange context
 */
public interface ErrorWebExchange<A extends ExchangeContext> extends ErrorExchange<A> {

	@Override
	WebRequest request();

	@Override
	WebResponse response();

	@Override
	default ErrorWebExchange<A> mapError(Function<? super Throwable, ? extends Throwable> errorMapper) {
		ErrorWebExchange<A> thisExchange = this;
		return new ErrorWebExchange<>() {

			@Override
			public HttpVersion getProtocol() {
				return thisExchange.getProtocol();
			}

			@Override
			public A context() {
				return thisExchange.context();
			}

			@Override
			public WebRequest request() {
				return thisExchange.request();
			}

			@Override
			public WebResponse response() {
				return thisExchange.response();
			}

			@Override
			public void reset(long code) {
				thisExchange.reset(code);
			}

			@Override
			public Optional<Throwable> getCancelCause() {
				return thisExchange.getCancelCause();
			}

			@Override
			public Throwable getError() {
				return errorMapper.apply(thisExchange.getError());
			}
		};
	}
}
