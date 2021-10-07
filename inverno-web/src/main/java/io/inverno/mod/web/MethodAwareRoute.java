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

import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;

/**
 * <p>
 * A route that specifies criteria used to determine whether the resource served
 * by the route can process a request based on its HTTP method.
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
public interface MethodAwareRoute<A extends ExchangeContext, B extends Exchange<A>> extends Route<A, B> {

	/**
	 * <p>
	 * Returns the HTTP method accepted by the resource served by the route.
	 * </p>
	 * 
	 * <p>
	 * This criteria should match the request HTTP method.
	 * </p>
	 * 
	 * @return a HTTP method or null
	 */
	Method getMethod();
}
