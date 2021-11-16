/*
 * Copyright 2021 Jeremy KUHN
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

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import java.util.function.Consumer;

/**
 * <p>
 * Defines method to specify interceptors on a router.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 * 
 * @see InterceptedRouter
 * @see Router
 * 
 * @param <A> the type of the exchange context
 * @param <B> the type of intercepted exchange
 * @param <C> the interceptable type
 * @param <D> the interceptor manager type
 */
public interface Interceptable<
		A extends ExchangeContext, 
		B extends Exchange<A>, 
		C extends Interceptable<A, B, C, D>,
		D extends InterceptorManager<A, B, C, D>
	> {

	/**
	 * <p>
	 * Returns an interceptor manager to define route interceptors.
	 * </p>
	 * 
	 * @return an interceptor manager
	 */
	D intercept();
	
	/**
	 * <p>
	 * Invokes the specified interceptor configurer on an interceptor manager.
	 * </p>
	 * 
	 * @param interceptorConfigurer an interceptor configurer
	 * 
	 * @return an intercepted router
	 */
	@SuppressWarnings("unchecked")
	default C intercept(Consumer<D> interceptorConfigurer) {
		interceptorConfigurer.accept(this.intercept());
		return (C) this;
	}
}
