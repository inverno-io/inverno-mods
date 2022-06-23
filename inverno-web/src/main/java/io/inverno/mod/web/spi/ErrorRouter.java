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
package io.inverno.mod.web.spi;

import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeHandler;


/**
 * <p>
 * Base error router interface.
 * </p>
 *
 * <p>
 * An error router uses route definitions to determine the error exchange
 * handler to invoke in order to process an error.
 * </p>
 *
 * <p>
 * Routes are defined in the router using an error route manager that allows to
 * specify route criteria and eventually the error exchange handler to invoke to
 * process an error that matches the criteria.
 * </p>
 *
 * <p>
 * An error router is itself an error exchange handler that implements a routing
 * logic to delegate the actual error exchange processing to the error exchange
 * handler defined in the route matching the original request. An error router
 * is typically used as error handler in a HTTP server.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 *
 * @see ErrorExchange
 * @see ErrorExchangeHandler
 * @see Route
 * @see ErrorRouteManager
 *
 * @param <A> the type of the exchange context
 * @param <B> the type of error exchange handled by the route
 * @param <C> the error router type
 * @param <D> the intercepted error router type
 * @param <E> the error route manager type
 * @param <F> the intercepted error route manager type
 * @param <G> the error interceptor manager type
 * @param <H> the interceptable route type
 */
public interface ErrorRouter<
		A extends ExchangeContext,
		B extends ErrorExchange<A>,
		C extends ErrorRouter<A, B, C, D, E, F, G, H>,
		D extends ErrorInterceptedRouter<A, B, C, D, E, F, G, H>,
		E extends ErrorRouteManager<A, B, C, E, H>,
		F extends ErrorRouteManager<A, B, D, F, H>,
		G extends ErrorInterceptorManager<A, B, D, G>,
		H extends InterceptableRoute<A, B>
	> extends Routable<A, B, C, E, H>, Interceptable<A, B, D, G>, ExchangeHandler<A, ErrorExchange<A>> {
	
}
