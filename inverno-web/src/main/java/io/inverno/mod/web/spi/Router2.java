/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.web.spi;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;

/**
 *
 * @author jkuhn
 */
public interface Router2<
		A extends ExchangeContext, 
		B extends Exchange<A>,
		C extends Router2<A, B, C, D, E, F, G, H>,
		D extends InterceptedRouter2<A, B, C, D, E, F, G, H>,
		E extends RouteManager2<A, B, C, E, H>,
		F extends RouteManager2<A, B, D, F, H>,
		G extends InterceptorManager2<A, B, D, G>,
		H extends InterceptableRoute<A, B>
	> extends Routable<A, B, C, E, H>, Interceptable<A, B, D, G>  {
	
}

