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
package io.inverno.mod.web;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.web.spi.Router;
import java.util.List;

/**
 * <p>
 * A web router is used to handle HTTP requests.
 * </p>
 *
 * <p>
 * It determines the web exchange handler to invoke based on the parameters of the request including the absolute path, the method, the content type and the accepted content type and language.
 * </p>
 *
 * <p>
 * An web router is itself an exchange handler that can be used as root handler of a HTTP server.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see WebExchange
 * @see WebRoute
 * @see WebRouteManager
 *
 * @param <A> the type of the exchange context
 */
public interface WebRouter<A extends ExchangeContext> extends 
	Router<A, WebExchange<A>, WebRouter<A>, WebInterceptedRouter<A>, WebRouteManager<A, WebRouter<A>>, WebRouteManager<A, WebInterceptedRouter<A>>, WebInterceptorManager<A, WebInterceptedRouter<A>>, WebRoute<A>>, 
	WebRoutable<A, WebRouter<A>>, 
	WebInterceptable<A, WebInterceptedRouter<A>> {
	
	/**
	 * <p>
	 * Configures the web router using the specified configurer and returns it.
	 * </p>
	 * 
	 * <p>
	 * If the specified configurer is null this method is a noop.
	 * </p>
	 * 
	 * @param configurer a web router configurer
	 * 
	 * @return the web router
	 */
	@SuppressWarnings("unchecked")
	default WebRouter<A> configure(WebRouterConfigurer<? super A> configurer) {
		configurer.configure((WebRouter)this);
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
	 * @param configurers a list of web router configurers
	 * 
	 * @return the web router
	 */
	default WebRouter<A> configure(List<WebRouterConfigurer<? super A>> configurers) {
		if(configurers != null) {
			for(WebRouterConfigurer<? super A> configurer : configurers) {
				this.configure(configurer);
			}
		}
		return this;
	}
}
