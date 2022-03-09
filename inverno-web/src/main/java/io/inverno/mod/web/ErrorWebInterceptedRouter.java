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
package io.inverno.mod.web;

import io.inverno.mod.web.spi.ErrorInterceptedRouter;

import java.util.List;

/**
 * <p>
 * A web intercepted error router attaches interceptors to error route handlers based on the parameters of the Error Web
 * route including the error type, path or path pattern, the accepted content type and language.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface ErrorWebInterceptedRouter extends
	ErrorInterceptedRouter<ErrorWebExchange<Throwable>, ErrorWebRouter, ErrorWebInterceptedRouter, ErrorWebRouteManager<ErrorWebRouter>, ErrorWebRouteManager<ErrorWebInterceptedRouter>, ErrorWebInterceptorManager<ErrorWebInterceptedRouter>, ErrorWebRoute>,
	ErrorWebRoutable<ErrorWebInterceptedRouter>,
	ErrorWebInterceptable<ErrorWebInterceptedRouter> {

	/**
	 * <p>
	 * Configures the error web intercepted router using the specified error web router configurer.
	 * </p>
	 *
	 * <p>
	 * Web interceptors previously defined in this router will be applied first to the routes created within the
	 * configurer.
	 * </p>
	 *
	 * <p>
	 * If the specified configurer is null this method is a noop.
	 * </p>
	 *
	 * @param configurer an error web router configurer
	 *
	 * @return the error web intercepted router
	 */
	ErrorWebInterceptedRouter configure(ErrorWebRouterConfigurer configurer);

	/**
	 * <p>
	 * Configures the error web intercepted router using the specified configurers.
	 * </p>
	 *
	 * <p>
	 * Web interceptors previously defined in this router will be applied first to the routes created within the
	 * configurers.
	 * </p>
	 *
	 * <p>
	 * If the specified list of configurers is null or empty this method is a noop.
	 * </p>
	 *
	 * @param configurers a list of error web router configurers
	 *
	 * @return the web intercepted router
	 */
	default ErrorWebInterceptedRouter configure(List<ErrorWebRouterConfigurer> configurers) {
		if(configurers != null) {
			for(ErrorWebRouterConfigurer configurer : configurers) {
				this.configure(configurer);
			}
		}
		return this;
	}
}
