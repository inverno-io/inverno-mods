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
@FunctionalInterface
public interface RequestHandler<A, B> {

	void handle(Request<A> request, Response<B> response);
	
	default <U, V> RequestHandler<U, V> map(Function<RequestHandler<A, B>, RequestHandler<U, V>> mapper) {
		Objects.requireNonNull(mapper);
		return mapper.apply(this);
	}
	
	default RequestHandler<A, B> andThen(RequestHandler<A, B> after) {
		Objects.requireNonNull(after);

        return (request, response) -> {
            this.handle(request, response);
            after.handle(request, response);
        };
	}
}
