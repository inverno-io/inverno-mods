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
package io.winterframework.mod.web;

import java.util.function.Function;

/**
 * @author jkuhn
 *
 */
public interface ErrorExchange<A, B extends Throwable> extends Exchange<Void, A> {

	B getError();
	
	@Override
	default <E> ErrorExchange<E, B> mapResponse(Function<? super A, ? extends E> responseMapper) {
		ErrorExchange<A, B> thisExchange = this;
		return new ErrorExchange<E, B>() {

			@Override
			public Request<Void> request() {
				return thisExchange.request();
			}

			@Override
			public Response<E> response() {
				return thisExchange.response().map(responseMapper);
			}

			@Override
			public B getError() {
				return thisExchange.getError();
			}
		};
	}
	
	default <E extends Throwable> ErrorExchange<A, E> mapError(Function<? super B, ? extends E> errorMapper) {
		ErrorExchange<A, B> thisExchange = this;
		return new ErrorExchange<A, E>() {

			@Override
			public Request<Void> request() {
				return thisExchange.request();
			}

			@Override
			public Response<A> response() {
				return thisExchange.response();
			}

			@Override
			public E getError() {
				return errorMapper.apply(thisExchange.getError());
			}
		};
	}
}
