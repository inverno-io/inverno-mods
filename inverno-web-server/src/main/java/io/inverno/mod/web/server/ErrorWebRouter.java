/*
 * Copyright 2020 Jeremy KUHN
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
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.web.server.spi.ErrorRouter;
import java.util.List;

/**
 * <p>
 * An error web router is used to handle failing requests for which an error was thrown during the initial processing.
 * </p>
 *
 * <p>
 * It determines the error web exchange handler to invoke based on the type of the error as well as the media type and language accepted by the client.
 * </p>
 *
 * <p>
 * An error web router is itself an error exchange handler that can be used as error handler of a HTTP server.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see ErrorExchange
 * @see ErrorWebExchange
 * @see ErrorWebRoute
 * @see ErrorWebRouteManager
 *
 * @param <A> the type of the exchange context
 */
public interface ErrorWebRouter<A extends ExchangeContext> extends
	ErrorRouter<A, ErrorWebExchange<A>, ErrorWebRouter<A>, ErrorWebInterceptedRouter<A>, ErrorWebRouteManager<A, ErrorWebRouter<A>>, ErrorWebRouteManager<A, ErrorWebInterceptedRouter<A>>, ErrorWebInterceptorManager<A, ErrorWebInterceptedRouter<A>>, ErrorWebRoute<A>>,
	ErrorWebRoutable<A, ErrorWebRouter<A>>,
	ErrorWebInterceptable<A, ErrorWebInterceptedRouter<A>> {

	/**
	 * <p>
	 * Configures the error web router using the specified configurer and returns it.
	 * </p>
	 *
	 * <p>
	 * If the specified configurer is null this method is a noop.
	 * </p>
	 *
	 * @param configurer an error web router configurer
	 *
	 * @return the error web router
	 */
	@SuppressWarnings("unchecked")
	default ErrorWebRouter<A> configure(ErrorWebRouterConfigurer<? super A> configurer) {
		configurer.configure((ErrorWebRouter)this);
		return this;
	}

	/**
	 * <p>
	 * Configures the web router using the specified configurers and returns it.
	 * </p>
	 *
	 * <p>
	 * If the specified list of configurers is null or empty this method is a noop.
	 * </p>
	 *
	 * @param configurers a list of error web router configurers
	 *
	 * @return the error web router
	 */
	default ErrorWebRouter<A> configure(List<ErrorWebRouterConfigurer<? super A>> configurers) {
		if(configurers != null) {
			for(ErrorWebRouterConfigurer<? super A> configurer : configurers) {
				this.configure(configurer);
			}
		}
		return this;
	}
}
