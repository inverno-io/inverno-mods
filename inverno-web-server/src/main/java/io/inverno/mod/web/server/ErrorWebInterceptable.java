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
package io.inverno.mod.web.server;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.web.server.spi.Interceptable;
import java.util.List;

/**
 * <p>
 * An Error Web interceptable allows to defined Error Web interceptors.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 *
 * @see ErrorWebRouter
 *
 * @param <A> the type of the exchange context
 * @param <B> the Error Web interceptable type
 */
public interface ErrorWebInterceptable<A extends ExchangeContext, B extends ErrorWebInterceptable<A, B>> extends Interceptable<A, ErrorWebExchange<A>, B, ErrorWebInterceptorManager<A, B>> {

	/**
	 * <p>
	 * Configures error web route interceptors using the specified configurer and returns an error web interceptable.
	 * </p>
	 *
	 * <p>
	 * If the specified configurer is null this method is a noop.
	 * </p>
	 *
	 * @param configurer an error web interceptors configurer
	 *
	 * @return an error web interceptable
	 */
	B configureInterceptors(ErrorWebInterceptorsConfigurer<? super A> configurer);

	/**
	 * <p>
	 * Configures error web route interceptors using the specified configurers and returns an error web interceptable.
	 * </p>
	 *
	 * <p>
	 * If the specified list of configurers is null or empty this method is a noop.
	 * </p>
	 *
	 * @param configurers a list of error web interceptors configurers
	 *
	 * @return an error web interceptable
	 */
	B configureInterceptors(List<ErrorWebInterceptorsConfigurer<? super A>> configurers);
}
