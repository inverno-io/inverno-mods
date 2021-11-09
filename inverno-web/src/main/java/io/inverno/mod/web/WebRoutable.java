/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.web;

import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.web.spi.Routable;

/**
 *
 * @author jkuhn
 */
public interface WebRoutable<A extends ExchangeContext, B extends WebRoutable<A, B>> extends Routable<
		A, 
		WebExchange<A>, 
		B, 
		WebRouteManager2<A, B>, 
		WebRoute<A>
	> {

}
