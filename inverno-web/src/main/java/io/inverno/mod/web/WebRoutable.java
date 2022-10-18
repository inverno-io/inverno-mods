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
import io.inverno.mod.web.spi.Routable;
import java.util.List;
import java.util.function.Consumer;

/**
 * <p>
 * A web routable allows to defined Web routes.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 * 
 * @see WebRouter
 * 
 * @param <A> the type of the exchange context
 * @param <B> the Web routable type
 */
public interface WebRoutable<A extends ExchangeContext, B extends WebRoutable<A, B>> extends Routable<A, WebExchange<A>, B, WebRouteManager<A, B>, WebRoute<A>> {

	/**
	 * <p>
	 * Returns a WebSocket route manager to define, enable, disable, remove or find WebSocket routes in the router.
	 * </p>
	 * 
	 * @return a WebSocket route manager
	 */
	WebSocketRouteManager<A, B> webSocketRoute();
	
	/**
	 * <p>
	 * Invokes the specified WebSocket route configurer on a WebSocket route manager.
	 * </p>
	 * 
	 * @param webSocketRouteConfigurer a WebSocket route configurer
	 * 
	 * @return the router
	 */
	@SuppressWarnings("unchecked")
	default B webSocketRoute(Consumer<WebSocketRouteManager<A, B>> webSocketRouteConfigurer) {
		webSocketRouteConfigurer.accept(this.webSocketRoute());
		return (B) this;
	}

	/**
	 * <p>
	 * Configures web routes using the specified configurer and returns the web routable.
	 * </p>
	 * 
	 * <p>
	 * If the specified configurer is null this method is a noop.
	 * </p>
	 * 
	 * @param configurer a web routes configurer
	 * 
	 * @return the web routable
	 */
	B configureRoutes(WebRoutesConfigurer<? super A> configurer);
	
	/**
	 * <p>
	 * Configures web routes using the specified configurers and returns the web routable.
	 * </p>
	 * 
	 * <p>
	 * If the specified list of configurers is null or empty this method is a noop.
	 * </p>
	 * 
	 * @param configurers a list of web routes configurers
	 * 
	 * @return the web routable
	 */
	@SuppressWarnings("unchecked")
	default B configureRoutes(List<WebRoutesConfigurer<? super A>> configurers) {
		if(configurers != null) {
			for(WebRoutesConfigurer<? super A> configurer : configurers) {
				this.configureRoutes(configurer);
			}
		}
		return (B)this;
	}
}
