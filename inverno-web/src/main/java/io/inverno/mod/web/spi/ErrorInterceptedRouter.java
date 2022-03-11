/*
 * Copyright 2022 Jeremy KUHN
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
import io.inverno.mod.http.server.ExchangeInterceptor;

import java.util.List;

/**
 * <p>
 * An error router that applies interceptors to matching route as they are created or to all the routes currently defined in the error router.
 * </p>
 *
 * <p>
 * An intercepting error router typically wraps an underlying {@link ErrorRouter} and intercepts route creation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 *
 * @param <A> the type of the exchange context
 * @param <B> the type of error exchange handled by the interceptor
 * @param <C> the error router type
 * @param <D> the intercepted error router type
 * @param <E> the error route manager type
 * @param <F> the intercepted error route manager type
 * @param <G> the error interceptor manager type
 * @param <H> the interceptable route type
 */
public interface ErrorInterceptedRouter<
		A extends ExchangeContext,
		B extends ErrorExchange<A>,
		C extends ErrorRouter<A, B, C, D, E, F, G, H>,
		D extends ErrorInterceptedRouter<A, B, C, D, E, F, G, H>,
		E extends ErrorRouteManager<A, B, C, E, H>,
		F extends ErrorRouteManager<A, B, D, F, H>,
		G extends ErrorInterceptorManager<A, B, D, G>,
		H extends InterceptableRoute<A, B>
	> extends Routable<A, B, D, F, H>, Interceptable<A, B, D, G> {
	
	/**
	 * <p>
	 * Returns the list of interceptors configured in the error router.
	 * </p>
	 *
	 * @return a list of exchange interceptors
	 */
	List<? extends ExchangeInterceptor<A, B>> getInterceptors();

	/**
	 * <p>
	 * Applies the interceptors to all the routes previously defined in the error router.
	 * </p>
	 *
	 * <p>
	 * If a matching route is already intercepted by a given interceptor, the interceptor will be moved to the top of the list.
	 * </p>
	 *
	 * @return this error router
	 */
	D applyInterceptors();

	/**
	 * <p>
	 * Returns the underlying non-intercepting error router.
	 * </p>
	 *
	 * @return the underlying non-intercepting error router.
	 */
	C getRouter();
}
