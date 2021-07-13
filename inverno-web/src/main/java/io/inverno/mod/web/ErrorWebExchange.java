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
package io.inverno.mod.web;

import java.util.function.Function;

import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.Request;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An error exchange that extends the HTTP server {@link ErrorExchange} with
 * features for the Web.
 * </p>
 * 
 * <p>
 * It especially supports response body encoding based on the response content
 * type.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ErrorWebRoute
 * @see ErrorWebRouteManager
 * @see ErrorWebRouter
 * 
 * @param <A> the error type
 */
public interface ErrorWebExchange<A extends Throwable> extends ErrorExchange<A> {

	@Override
	WebResponse response();
	
	@Override
	default <T extends Throwable> ErrorWebExchange<T> mapError(Function<? super A, ? extends T> errorMapper) {
		ErrorWebExchange<A> thisExchange = this;
		return new ErrorWebExchange<T>() {

			@Override
			public Request request() {
				return thisExchange.request();
			}

			@Override
			public WebResponse response() {
				return thisExchange.response();
			}
			
			@Override
			public Exchange finalizer(Mono<Void> finalizer) {
				thisExchange.finalizer(finalizer);
				return this;
			}

			@Override
			public T getError() {
				return errorMapper.apply(thisExchange.getError());
			}
		};
	}
}
