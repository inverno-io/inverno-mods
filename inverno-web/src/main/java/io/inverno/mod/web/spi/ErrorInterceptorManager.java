/*
 * Copyright 2022 Jeremy KUHN
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

import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.ExchangeContext;

/**
 * <p>
 * Base error interceptor manager interface.
 * </p>
 *
 * <p>
 * An error interceptor manager is used to configure interceptors in a {@link ErrorInterceptedRouter}. It is created by an error
 * router and allows to define interceptors in an intercepting router.
 * </p>
 *
 * <p>
 * A typical implementation should define methods to set criteria used by the router to match an error route to an interceptor and an exchange interceptor that is eventually chained with the error route exchange
 * handler.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 *
 * @param <A> the type of exchange handled by the interceptor
 * @param <B> the interceptable type
 * @param <C> the error interceptor manager type
 */
public interface ErrorInterceptorManager<
		A extends ErrorExchange<Throwable>,
		B extends Interceptable<ExchangeContext, A, B, C>,
		C extends ErrorInterceptorManager<A, B, C>
	> extends InterceptorManager<ExchangeContext, A, B, C> {

	/**
	 * <p>
	 * Specifies the type of errors accepted by the interceptor.
	 * </p>
	 *
	 * @param error a type of error
	 *
	 * @return the error interceptor manager
	 *
	 * @see ErrorAware
	 */
	C error(Class<? extends Throwable> error);
}
