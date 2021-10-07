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
package io.inverno.mod.web;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;

/**
 * <p>
 * A route that specifies criteria used to determine whether the resource served
 * by the route can process a request that failed with a particular type of
 * error.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Route
 * 
 * @param <A> the type of the exchange context
 * @param <B> the type of web exchange handled by the route
 */
public interface ErrorAwareRoute<A extends ExchangeContext, B extends Exchange<A>> extends Route<A, B> {

	/**
	 * <p>
	 * Returns the type of errors supported by the resource served by the route.
	 * </p>
	 * 
	 * @return an error type or null
	 */
	Class<? extends Throwable> getError();
}
