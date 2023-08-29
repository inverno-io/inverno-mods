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

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ReactiveExchangeHandler;

/**
 * <p>
 * Base route interface.
 * </p>
 *
 * <p>
 * A route specifies an exchange handler and a set of criteria used by a router to determine the exchange handler to execute in response of a particular request matching these criteria.
 * </p>
 *
 * <p>
 * A route defines then a <i>path</i> to a resource, it is used by a router to route a request to the handler matching the resource being requested.
 * </p>
 *
 * <p>
 * A route is defined in a router using a route manager.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see Exchange
 * @see Router
 *
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the route
 */
public interface Route<A extends ExchangeContext, B extends Exchange<A>> {

	/**
	 * <p>
	 * Returns the route handler used to process a request matching the route's criteria.
	 * </p>
	 *
	 * @return an exchange handler
	 */
	ReactiveExchangeHandler<A, B> getHandler();
	
	/**
	 * <p>Enables the route.</p>
	 */
	void enable();

	/**
	 * <p>Disables the route.</p>
	 */
	void disable();

	/**
	 * <p>
	 * Determines whether the route is disabled.
	 * </p>
	 * 
	 * @return true if the route is disabled, false otherwise
	 */
	boolean isDisabled();
	
	/**
	 * <p>
	 * Removes the route from the router that contains it.
	 * </p>
	 */
	void remove();
}
