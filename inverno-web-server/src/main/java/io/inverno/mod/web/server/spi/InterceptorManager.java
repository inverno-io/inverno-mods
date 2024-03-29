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
package io.inverno.mod.web.server.spi;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeInterceptor;
import java.util.List;

/**
 * <p>
 * Base interceptor manager interface.
 * </p>
 *
 * <p>
 * An interceptor manager is used to configure interceptors in a {@link InterceptedRouter}. It is created by a router and allows to define interceptors in an intercepting router.
 * </p>
 *
 * <p>
 * A typical implementation should define methods to set criteria used by the router to match a route to an interceptor and an exchange interceptor that is eventually chained with the route exchange
 * handler.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 *
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the interceptor
 * @param <C> the interceptable type
 * @param <D> the interceptor manager type
 */
public interface InterceptorManager<
		A extends ExchangeContext, 
		B extends Exchange<A>, 
		C extends Interceptable<A, B, C, D>,
		D extends InterceptorManager<A, B, C, D>
	> {
	
	/**
	 * <p>
	 * Specifies the exchange interceptor to apply to the resources matching the criteria defined in the interceptor manager.
	 * </p>
	 *
	 * <p>
	 * This method basically appends the interceptor and the associated route criteria to the intercepted router it comes from.
	 * </p>
	 *
	 * @param interceptor the exchange interceptor
	 *
	 * @return the router
	 */
	C interceptor(ExchangeInterceptor<? super A, B> interceptor);

	/**
	 * <p>
	 * Specifies multiple exchange interceptors to apply to the resources matching the criteria defined in the interceptor manager.
	 * </p>
	 *
	 * <p>
	 * This method basically appends the interceptors and the associated route criteria to the intercepted router it comes from.
	 * </p>
	 *
	 * @param interceptors a list of exchange interceptors
	 *
	 * @return the router
	 */
	C interceptors(List<ExchangeInterceptor<? super A, B>> interceptors);
}
