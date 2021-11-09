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
package io.inverno.mod.web.internal;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.web.spi.Route;

/**
 * <p>
 * A route interceptor specifies the criteria that determine whether a an exchange interceptor should be applied on a route.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 */
interface RouteInterceptor<A extends ExchangeContext, B extends Exchange<A>, C extends Route<A, B>, D extends RouteInterceptor<A, B, C, D>> {

	/**
	 * <p>
	 * Returns the ordered list of exchange interceptors to apply to the route.
	 * </p>
	 * 
	 * @return a list of exchange interceptors
	 */
	ExchangeInterceptor<A, B> getInterceptor();
	
	/**
	 * <p>
	 * Determines whether the specified route is matched by the route interceptor.
	 * </p>
	 * 
	 * <p>
	 * When a route is matched by a route interceptor, its exchange interceptor is chained before the route handler and therefore executed when a request is handled by the route.
	 * </p>
	 * 
	 * @param route	a route
	 * 
	 * @return a route interceptor if the route is a match, null otherwise
	 */
	D matches(C route);
}
