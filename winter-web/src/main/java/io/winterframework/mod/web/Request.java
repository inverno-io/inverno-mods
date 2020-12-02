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
public interface Request<A> {

	RequestHeaders headers();
	
	RequestParameters parameters();
	
	RequestCookies cookies();
	
	SocketAddress getRemoteAddress();
	
	// TODO path parameters
	
	Optional<A> body();
	
	default <E> Request<E> map(Function<? super A, ? extends E> bodyMapper) {
		Request<A> thisRequest = this;
		return new Request<E>() {
			
			private Optional<E> body;
			
			@Override
			public RequestHeaders headers() {
				return thisRequest.headers();
			}

			@Override
			public RequestParameters parameters() {
				return thisRequest.parameters();
			}

			@Override
			public RequestCookies cookies() {
				return thisRequest.cookies();
			}

			@Override
			public SocketAddress getRemoteAddress() {
				return thisRequest.getRemoteAddress();
			}

			@Override
			public Optional<E> body() {
				if(this.body == null) {
					this.body = thisRequest.body().map(bodyMapper::apply);
				}
				return this.body;
			}
		};
	}
}
