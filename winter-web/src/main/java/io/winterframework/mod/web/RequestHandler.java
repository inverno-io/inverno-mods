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
public interface RequestHandler<A, B, C> {

	void handle(Request<A, B> request, Response<C> response);
	
	default <T, U, V, W extends RequestHandler<T, U, V>> W map(Function<? super RequestHandler<A, B, C>, ? extends W> mapper) {
		Objects.requireNonNull(mapper);
		return mapper.apply(this);
	}
	
	default RequestHandler<A, B, C> doBefore(RequestHandler<A, B, C> after) {
		Objects.requireNonNull(after);

        return (request, response) -> {
            this.handle(request, response);
            after.handle(request, response);
        };
	}
	
	default RequestHandler<A, B, C> doAfter(RequestHandler<A, B, C> after) {
		Objects.requireNonNull(after);

        return (request, response) -> {
            this.handle(request, response);
            after.handle(request, response);
        };
	}
}
