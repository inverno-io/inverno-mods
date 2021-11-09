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
public interface Interceptable<
		A extends ExchangeContext, 
		B extends Exchange<A>, 
		C extends Interceptable<A, B, C, D>,
		D extends InterceptorManager2<A, B, C, D>
	> {

	/**
	 * <p>
	 * Returns an interceptor manager to define route interceptors.
	 * </p>
	 * 
	 * @return an interceptor manager
	 */
	D intercept();
	
	@SuppressWarnings("unchecked")
	default C interceptRoute(Consumer<D> interceptorConfigurer) {
		interceptorConfigurer.accept(this.intercept());
		return (C) this;
	}
}
