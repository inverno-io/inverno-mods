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

import java.util.Objects;
import java.util.function.Function;

/**
 * @author jkuhn
 *
 */
public interface Exchange<A, B> {

	Request<A> request();
	
	Response<B> response();
	
	default <E, F, G extends Exchange<E, F>> G map(Function<? super Exchange<A, B>, ? extends G> mapper) {
		Objects.requireNonNull(mapper);
		return mapper.apply(this);
	}
	
	default <E> Exchange<E, B> mapRequest(Function<? super A, ? extends E> requestMapper) {
		return this.map(requestMapper, Function.identity());
	}

	default <E> Exchange<A, E> mapResponse(Function<? super B, ? extends E> responseMapper) {
		return this.map(Function.identity(), responseMapper);
	}
	
	default <E, F> Exchange<E, F> map(Function<? super A, ? extends E> requestMapper, Function<? super B, ? extends F> responseMapper) {
		Exchange<A, B> thisExchange = this;
		return new Exchange<E, F>() {

			@Override
			public Request<E> request() {
				return thisExchange.request().map(requestMapper);
			}

			@Override
			public Response<F> response() {
				return thisExchange.response().map(responseMapper);
			}
		};
	}
}
