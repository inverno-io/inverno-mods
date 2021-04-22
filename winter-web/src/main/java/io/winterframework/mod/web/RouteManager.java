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
package io.winterframework.mod.web;

import java.util.Set;

import io.winterframework.mod.http.server.Exchange;
import io.winterframework.mod.http.server.ExchangeHandler;

/**
 * <p>
 * Base route manager interface.
 * </p>
 * 
 * <p>
 * A route manager is used to manage the routes of a router. It is created by a
 * router and allows to define, enable, disable, remove and find routes in a
 * router.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Exchange
 * @see Route
 * @see Router
 * 
 * @param <A> the type of exchange handled by the route
 * @param <B> the router type
 * @param <C> the route manager type
 * @param <D> the route type
 * @param <E> the router exchange type
 */
public interface RouteManager<A extends Exchange, B extends Router<A, B, C, D, E>, C extends RouteManager<A, B, C, D, E>, D extends Route<A>, E extends Exchange> {

	/**
	 * <p>
	 * Specifies the route exchange handler.
	 * </p>
	 *
	 * <p>
	 * This method basically appends the route specified in the route manager to the
	 * router it comes from.
	 * </p>
	 * 
	 * @param handler the route exchange handler
	 * 
	 * @return the router
	 */
	B handler(ExchangeHandler<? super A> handler);

	/**
	 * <p>
	 * Enables all the routes that matches the criteria specified in the route
	 * manager and defined in the router it comes from.
	 * </p>
	 * 
	 * @return the router
	 */
	B enable();
	
	/**
	 * <p>
	 * Disables all the routes that matches the criteria specified in the route
	 * manager and defined in the router it comes from.
	 * </p>
	 * 
	 * @return the router
	 */
	B disable();
	
	/**
	 * <p>
	 * Removes all the routes that matches the criteria specified in the route
	 * manager and defined in the router it comes from.
	 * </p>
	 * 
	 * @return the router
	 */
	B remove();
	
	/**
	 * <p>
	 * Finds all the routes that matches the criteria specified in the route manager
	 * and defined in the router it comes from.
	 * </p>
	 * 
	 * @return a set of routes or an empty set if no route matches the criteria
	 */
	Set<D> findRoutes();
}
