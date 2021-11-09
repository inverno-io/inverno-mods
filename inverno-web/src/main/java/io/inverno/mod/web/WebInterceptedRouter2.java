/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.web;

import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.web.spi.InterceptedRouter2;

/**
 *
 * @author jkuhn
 */
public interface WebInterceptedRouter2<A extends ExchangeContext> extends InterceptedRouter2<
		A, 
		WebExchange<A>, 
		WebRouter2<A>, 
		WebInterceptedRouter2<A>, 
		WebRouteManager2<A, WebRouter2<A>>, 
		WebRouteManager2<A, WebInterceptedRouter2<A>>, 
		WebInterceptorManager2<A, WebInterceptedRouter2<A>>, 
		WebRoute<A>
	> , WebRoutable<A, WebInterceptedRouter2<A>>, WebInterceptable<A, WebInterceptedRouter2<A>> {

}
