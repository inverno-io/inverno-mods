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

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.ServerController;

/**
 * <p>
 * Configures the routes and the interceptors of the {@link WebRouter} and {@link ErrorWebRouter} used to create the {@link ServerController} injected
 * in the HTTP server to process client requests.
 * </p>
 * 
 * <p>
 * This configurer also exposes a {@link #createContext()} method used to create the exchange context eventually attached to an exchange and an error
 * exchange.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 *
 * @see WebRouterConfigurer
 * @see ErrorWebRouterConfigurer
 *
 * @param <A> the type of the exchange context
 */
public interface WebServerControllerConfigurer<A extends ExchangeContext> extends WebRouterConfigurer<A>, ErrorWebRouterConfigurer<A> {
	
	/**
	 * <p>
	 * Creates a context matching routes and interceptors requirement.
	 * </p>
	 *
	 * <p>
	 * This context supplier is used by the Web Server controller to create a specific context attached to {@link WebExchange} and
	 * {@link ErrorWebExchange} required by the routes and interceptors configured by the configurer.
	 * </p>
	 *
	 * @return a new context instance
	 */
	default A createContext() {
		return null;
	}
}
