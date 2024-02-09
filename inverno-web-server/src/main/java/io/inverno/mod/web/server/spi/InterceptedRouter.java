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
package io.inverno.mod.web.server.spi;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.Exchange;
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
 * @param <F> the intercepted route manager type
 * @param <G> the interceptor manager type
 * @param <H> the interceptable route type
 */
public interface InterceptedRouter<
		A extends ExchangeContext, 
		B extends Exchange<A>,
		C extends Router<A, B, C, D, E, F, G, H>,
		D extends InterceptedRouter<A, B, C, D, E, F, G, H>,
		E extends RouteManager<A, B, C, E, H>,
		F extends RouteManager<A, B, D, F, H>,
		G extends InterceptorManager<A, B, D, G>,
		H extends InterceptableRoute<A, B>
	> extends Routable<A, B, D, F, H>, Interceptable<A, B, D, G> {
	
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
	 * @return this router
	 */
	D applyInterceptors();
	
	/**
	 * <p>
	 * Returns the underlying non-intercepting router.
	 * </p>
	 * 
	 * @return the underlying non-intercepting router.
	 */
	C getRouter();
}
