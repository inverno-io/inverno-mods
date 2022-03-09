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
import io.inverno.mod.http.server.ErrorExchangeHandler;
import io.inverno.mod.http.server.ExchangeContext;


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
 * @param <A> the type of exchange handled by the route
 * @param <B> the error router type
 * @param <C> the intercepted error router type
 * @param <D> the error route manager type
 * @param <E> the intercepted error route manager type
 * @param <F> the error interceptor manager type
 * @param <G> the interceptable route type
 */
public interface ErrorRouter<
		A extends ErrorExchange<Throwable>,
		B extends ErrorRouter<A, B, C, D, E, F, G>,
		C extends ErrorInterceptedRouter<A, B, C, D, E, F, G>,
		D extends ErrorRouteManager<A, B, D, G>,
		E extends ErrorRouteManager<A, C, E, G>,
		F extends ErrorInterceptorManager<A, C, F>,
		G extends InterceptableRoute<ExchangeContext, A>
	> extends Routable<ExchangeContext, A, B, D, G>, Interceptable<ExchangeContext, A, C, F>, ErrorExchangeHandler<Throwable, ErrorExchange<Throwable>> {

}
