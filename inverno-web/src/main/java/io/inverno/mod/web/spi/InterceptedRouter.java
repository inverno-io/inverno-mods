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

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeInterceptor;
import java.util.List;

/**
 * <p>
 * A router that applies interceptors to matching route as they are created or to all the routes currently defined in the router.
 * </p>
 * 
 * <p>
 * An intercepting router typically wraps an underlying {@link Router} and intercepts route creation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 * 
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the route
 * @param <C> the router type
 * @param <D> the intercepted router type
 * @param <E> the route manager type
 * @param <F> the interceptor manager type
 * @param <G> the interceptable route type
 * @param <H> the type of exchange handled by the router
 */
public interface InterceptedRouter<
		A extends ExchangeContext, 
		B extends Exchange<A>, 
		C extends Router<A, B, C, D, E, F, G, H>, 
		D extends InterceptedRouter<A, B, C, D, E, F, G, H>, 
		E extends RouteManager<A, B, C, D, E, F, G, H>, 
		F extends InterceptorManager<A, B, C, D, E, F, G, H>, 
		G extends InterceptableRoute<A, B>, 
		H extends Exchange<A>
	> extends Router<A, B, C, D, E, F, G, H> {
	
	/**
	 * <p>
	 * Returns the list of interceptors configured in the router.
	 * </p>
	 * 
	 * @return a list of exchange interceptors
	 */
	List<? extends ExchangeInterceptor<A, B>> getInterceptors();
	
	/**
	 * <p>
	 * Applies the interceptors to all the routes previously defined in the router.
	 * </p>
	 *
	 * <p>
	 * If a matching route is already intercepted by a given interceptor, the interceptor will be moved to the top of the list.</p>
	 *
	 * @return this router
	 */
	D applyInterceptors();
	
	/**
	 * <p>
	 * Reverts to the underlying non-intercepting router.
	 * </p>
	 * 
	 * @return the underlying non-intercepting router.
	 */
	C clearInterceptors();
}
