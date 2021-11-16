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
package io.inverno.mod.web;

import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.web.spi.Interceptable;
import java.util.List;

/**
 * <p>
 * A Web interceptable allows to defined Web interceptors.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 * 
 * @see WebRouter
 * 
 * @param <A> the type of the exchange context
 * @param <B> the Web intercptable type
 */
public interface WebInterceptable<A extends ExchangeContext, B extends WebInterceptable<A, B>> extends Interceptable<A, WebExchange<A>, B, WebInterceptorManager<A, B>> {

	/**
	 * <p>
	 * Configures web route interceptors using the specified configurer and returns a web interceptable.
	 * </p>
	 * 
	 * <p>
	 * If the specified configurer is null this method is a noop.
	 * </p>
	 * 
	 * @param configurer a web interceptors configurer
	 * 
	 * @return a web interceptable
	 */
	B configureInterceptors(WebInterceptorsConfigurer<? super A> configurer);
	
	/**
	 * <p>
	 * Configures web route interceptors using the specified configurers and returns a web interceptable.
	 * </p>
	 * 
	 * <p>
	 * If the specified list of configurers is null or empty this method is a noop.
	 * </p>
	 * 
	 * @param configurers a list of web interceptors configurers
	 * 
	 * @return a web interceptable
	 */
	B configureInterceptors(List<WebInterceptorsConfigurer<? super A>> configurers);
}
