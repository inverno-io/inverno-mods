/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.web;

import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.web.spi.Interceptable;

/**
 *
 * @author jkuhn
 */
public interface WebInterceptable<A extends ExchangeContext, B extends WebInterceptable<A, B>> extends Interceptable<
		A, 
		WebExchange<A>, 
		B, 
		WebInterceptorManager2<A, B>
	> {

}
