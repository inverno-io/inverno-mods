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

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author jkuhn
 *
 */
public interface Response<A> {
	
	boolean isHeadersWritten();
	
	Response<A> headers(Consumer<ResponseHeaders> headersConfigurer);
	
	Response<A> trailers(Consumer<ResponseTrailers> trailersConfigurer);
	
	Response<A> cookies(Consumer<ResponseCookies> cookiesConfigurer);
	
	A body();
	
	default <E> Response<E> map(Function<? super A, ? extends E> bodyMapper) {
		Response<A> sourceResponse = this;
		
		return new Response<E>() {
			
			private Optional<E> body;
			
			@Override
			public boolean isHeadersWritten() {
				return sourceResponse.isHeadersWritten();
			}

			@Override
			public Response<E> headers(Consumer<ResponseHeaders> headersConfigurer) throws IllegalStateException {
				sourceResponse.headers(headersConfigurer);
				return this;
			}
			
			@Override
			public Response<E> trailers(Consumer<ResponseTrailers> trailersConfigurer) {
				sourceResponse.trailers(trailersConfigurer);
				return this;
			}

			@Override
			public Response<E> cookies(Consumer<ResponseCookies> cookiesConfigurer) {
				sourceResponse.cookies(cookiesConfigurer);
				return this;
			}

			@Override
			public E body() {
				if(this.body == null) {
					this.body = Optional.ofNullable(bodyMapper.apply(sourceResponse.body()));
				}
				return this.body.get();
			}
		};
	}
}
