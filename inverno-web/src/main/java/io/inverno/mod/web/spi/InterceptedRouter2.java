/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.web.spi;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeInterceptor;
import java.util.List;

/**
 *
 * @author jkuhn
 */
public interface InterceptedRouter2<
		A extends ExchangeContext, 
		B extends Exchange<A>,
		C extends Router2<A, B, C, D, E, F, G, H>,
		D extends InterceptedRouter2<A, B, C, D, E, F, G, H>,
		E extends RouteManager2<A, B, C, E, H>,
		F extends RouteManager2<A, B, D, F, H>,
		G extends InterceptorManager2<A, B, D, G>,
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
