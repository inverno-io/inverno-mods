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

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.web.spi.InterceptedRouter;
import java.util.List;

/**
 * <p>
 * A web intercepted router attaches interceptors to route handler based on the parameters of the Web route including
 * the path or path pattern, the method, the content type and the accepted content type and language.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 *
 * @param <A> the type of the exchange context
 */
public interface WebInterceptedRouter<A extends ExchangeContext> extends 
	InterceptedRouter<A, WebExchange<A>, WebRouter<A>, WebInterceptedRouter<A>, WebRouteManager<A, WebRouter<A>>, WebRouteManager<A, WebInterceptedRouter<A>>, WebInterceptorManager<A, WebInterceptedRouter<A>>, WebRoute<A>>, 
	WebRoutable<A, WebInterceptedRouter<A>>, 
	WebInterceptable<A, WebInterceptedRouter<A>> {
	
	/**
	 * <p>
	 * Configures the web intercepted router using the specified web router configurer and returns it.
	 * </p>
	 * 
	 * <p>
	 * Web interceptors previously defined in this router will be applied first to the routes created within the configurer.
	 * </p>
	 * 
	 * <p>
	 * If the specified configurer is null this method is a noop.
	 * </p>
	 * 
	 * @param configurer a web router configurer
	 * 
	 * @return the web intercepted router
	 */
	@SuppressWarnings("unchecked")
	WebInterceptedRouter<A> configure(WebRouterConfigurer<? super A> configurer);
	
	/**
	 * <p>
	 * Configures the web intercepted router using the specified configurers and returns it.
	 * </p>
	 * 
	 * <p>
	 * Web interceptors previously defined in this router will be applied first to the routes created within the configurers.
	 * </p>
	 * 
	 * <p>
	 * If the specified list of configurers is null or empty this method is a noop.
	 * </p>
	 * 
	 * @param configurers a list of web router configurers
	 * 
	 * @return the web intercepted router
	 */
	default WebInterceptedRouter<A> configure(List<WebRouterConfigurer<? super A>> configurers) {
		if(configurers != null) {
			for(WebRouterConfigurer<? super A> configurer : configurers) {
				this.configure(configurer);
			}
		}
		return this;
	}
}
