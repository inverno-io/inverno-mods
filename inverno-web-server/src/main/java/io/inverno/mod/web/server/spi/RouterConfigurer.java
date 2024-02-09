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

/**
 * <p>
 * Base router configurer interface.
 * </p>
 *
 * <p>
 * A router configurer is used to configure interceptors and routes in a router.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Router
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
public interface RouterConfigurer<
		A extends ExchangeContext, 
		B extends Exchange<A>,
		C extends Router<A, B, C, D, E, F, G, H>,
		D extends InterceptedRouter<A, B, C, D, E, F, G, H>,
		E extends RouteManager<A, B, C, E, H>,
		F extends RouteManager<A, B, D, F, H>,
		G extends InterceptorManager<A, B, D, G>,
		H extends InterceptableRoute<A, B>
	> {

	/**
	 * <p>
	 * Configures the specified router.
	 * </p>
	 *
	 * @param router the router to configure
	 */
	void configure(C router);
}
