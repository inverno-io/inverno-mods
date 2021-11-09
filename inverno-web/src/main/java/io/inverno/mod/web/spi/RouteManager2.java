/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.web.spi;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import java.util.Set;

/**
 *
 * @author jkuhn
 */
public interface RouteManager2<
		A extends ExchangeContext, 
		B extends Exchange<A>, 
		C extends Routable<A, B, C, D, E>, 
		D extends RouteManager2<A, B, C, D, E>,
		E extends Route<A, B>
	> {

	/**
	 * <p>
	 * Enables all the routes that matches the criteria specified in the route
	 * manager and defined in the router it comes from.
	 * </p>
	 * 
	 * @return the router
	 */
	C enable();
	
	/**
	 * <p>
	 * Disables all the routes that matches the criteria specified in the route
	 * manager and defined in the router it comes from.
	 * </p>
	 * 
	 * @return the router
	 */
	C disable();
	
	/**
	 * <p>
	 * Removes all the routes that matches the criteria specified in the route
	 * manager and defined in the router it comes from.
	 * </p>
	 * 
	 * @return the router
	 */
	C remove();
	
	/**
	 * <p>
	 * Finds all the routes that matches the criteria specified in the route manager
	 * and defined in the router it comes from.
	 * </p>
	 * 
	 * @return a set of routes or an empty set if no route matches the criteria
	 */
	Set<E> findRoutes();
}
