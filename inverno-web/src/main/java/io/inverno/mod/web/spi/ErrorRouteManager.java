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
package io.inverno.mod.web.spi;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.ErrorExchange;

/**
 * <p>
 * Base error route manager interface.
 * </p>
 *
 * <p>
 * An error route manager is used to manage the routes of an error router. It is created by an error router and allows to define, enable, disable, remove and find routes in an error router.
 * </p>
 *
 * <p>
 * A typical implementation should define methods to set criteria used by the router to match an error exchange to an error route and an error exchange handler that eventually handles the matched
 * error exchange.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 *
 * @see ErrorExchange
 * @see Route
 * @see ErrorRouter
 *
 * @param <A> the type of the exchange context
 * @param <B> the type of error exchange handled by the route
 * @param <C> the routable type
 * @param <D> the error route manager type
 * @param <E> the route type
 */
public interface ErrorRouteManager<
		A extends ExchangeContext,
		B extends ErrorExchange<A>,
		C extends Routable<A, B, C, D, E>,
		D extends ErrorRouteManager<A, B, C, D, E>,
		E extends Route<A, B>
	> extends RouteManager<A, B, C, D, E> {
	
	/**
	 * <p>
	 * Specifies the type of errors accepted by the route.
	 * </p>
	 *
	 * @param error a type of error
	 *
	 * @return the error route manager
	 *
	 * @see ErrorAware
	 */
	D error(Class<? extends Throwable> error);
}
