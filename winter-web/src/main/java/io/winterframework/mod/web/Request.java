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

import java.net.SocketAddress;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author jkuhn
 *
 */
public interface Request<A, B> {

	RequestHeaders headers();
	
	RequestParameters parameters();
	
	RequestCookies cookies();
	
	SocketAddress getRemoteAddress();
	
	// TODO path parameters
	
	Optional<A> body();
	
	B context();
	
	default <E> Request<E, B> mapBody(Function<? super A, ? extends E> bodyMapper) {
		return this.map(bodyMapper, Function.identity());
	}
	
	default <E> Request<A, E> mapContext(Function<? super B, ? extends E> contextMapper) {
		return this.map(Function.identity(), contextMapper);
	}
	
	default <E, F> Request<E, F> map(Function<? super A, ? extends E> bodyMapper, Function<? super B, ? extends F> contextMapper) {
		
		Request<A, B> sourceRequest = this;
		
		return new Request<E, F>() {
			
			private Optional<E> body;
			
			private Optional<? extends F> context;

			@Override
			public RequestHeaders headers() {
				return sourceRequest.headers();
			}

			@Override
			public RequestParameters parameters() {
				return sourceRequest.parameters();
			}

			@Override
			public RequestCookies cookies() {
				return sourceRequest.cookies();
			}

			@Override
			public SocketAddress getRemoteAddress() {
				return sourceRequest.getRemoteAddress();
			}

			@Override
			public Optional<E> body() {
				if(this.body == null) {
					this.body = sourceRequest.body().map(bodyMapper::apply);
				}
				return this.body;
			}
			
			@Override
			public F context() {
				if(this.context == null) {
					this.context = Optional.ofNullable(contextMapper.apply(sourceRequest.context()));
				}
				return this.context.get();
			}
		};
	}
}
