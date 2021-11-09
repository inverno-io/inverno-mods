/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.web.spi;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import java.util.function.Consumer;

/**
 *
 * @author jkuhn
 */
public interface Routable<
		A extends ExchangeContext, 
		B extends Exchange<A>, 
		C extends Routable<A, B, C, D, E>,
		D extends RouteManager2<A, B, C, D, E>,
		E extends Route<A, B>
	> {

	/**
	 * <p>
	 * Returns a route manager to define, enable, disable, remove or find routes
	 * in the router.
	 * </p>
	 * 
	 * @return a route manager
	 */
	D route();
	
	/**
	 * <p>
	 * Invokes the specified route configurer on a route manager.
	 * </p>
	 * 
	 * @param routeConfigurer a route configurer
	 * 
	 * @return the router
	 */
	@SuppressWarnings("unchecked")
	default C route(Consumer<D> routeConfigurer) {
		routeConfigurer.accept(this.route());
		return (C) this;
	}
}
