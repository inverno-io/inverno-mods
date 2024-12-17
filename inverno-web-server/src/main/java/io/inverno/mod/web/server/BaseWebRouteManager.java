/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.web.server;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.Exchange;
import java.util.Set;

/**
 * <p>
 * Base Web route manager.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see WebRouteManager
 * @see WebSocketRouteManager
 * @see ErrorWebRouteManager
 *
 * @param <A> the exchange context type
 * @param <B> the exchange type
 * @param <C> the Web route type
 * @param <D> the Web router type
 */
public interface BaseWebRouteManager<A extends ExchangeContext, B extends Exchange<A>, C extends BaseWebRoute<A, B>, D extends BaseWebRouter> {

	/**
	 * <p>
	 * Enables all the routes currently defined in the router that are matching the criteria specified in the route manager.
	 * </p>
	 *
	 * @return the router
	 */
	D enable();

	/**
	 * <p>
	 * Disables all the routes currently defined in the router that are matching the criteria specified in the route manager.
	 * </p>
	 *
	 * @return the router
	 */
	D disable();

	/**
	 * <p>
	 * Removes all the routes currently defined in the router that are matching the criteria specified in the route manager.
	 * </p>
	 *
	 * @return the router
	 */
	D remove();

	/**
	 * <p>
	 * Finds all the routes currently defined that are matching the criteria specified in the route manager.
	 * </p>
	 *
	 * @return a set of routes or an empty set if no route matches the criteria
	 */
	Set<C> findRoutes();
}
