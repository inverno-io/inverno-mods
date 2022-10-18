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
package io.inverno.mod.web.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.http.server.ws.WebSocketExchangeHandler;
import io.inverno.mod.web.Web2SocketExchange;
import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.WebRoutable;
import io.inverno.mod.web.WebRoute;
import io.inverno.mod.web.WebRouteManager;
import io.inverno.mod.web.WebRoutesConfigurer;
import io.inverno.mod.web.WebSocketRoute;
import io.inverno.mod.web.WebSocketRouteManager;
import java.util.Set;

/**
 * <p>
 * A {@link WebRoutable} facade for Web routers.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 * 
 * @param <A> the type of the initial Web routable
 */
class GenericWebRoutableFacade<A extends WebRoutable<ExchangeContext, A>> implements WebRoutable<ExchangeContext, GenericWebRoutableFacade<A>> {

	private final A initialRoutable;
	
	public GenericWebRoutableFacade(A initialRoutable) {
		this.initialRoutable = initialRoutable;
	}

	@Override
	public WebRouteManager<ExchangeContext, GenericWebRoutableFacade<A>> route() {
		return new WebRouteManagerFacade(this.initialRoutable.route());
	}

	@Override
	public WebSocketRouteManager<ExchangeContext, GenericWebRoutableFacade<A>> webSocketRoute() {
		return new WebSocketRouteManagerFacade(this.initialRoutable.webSocketRoute());
	}
	
	@Override
	public GenericWebRoutableFacade<A> configureRoutes(WebRoutesConfigurer<? super ExchangeContext> configurer) {
		this.initialRoutable.configureRoutes(configurer);
		return this;
	}

	@Override
	public Set<WebRoute<ExchangeContext>> getRoutes() {
		return this.initialRoutable.getRoutes();
	}
	
	private class WebRouteManagerFacade implements WebRouteManager<ExchangeContext, GenericWebRoutableFacade<A>> {

		private final WebRouteManager<ExchangeContext, A> routeManager;

		public WebRouteManagerFacade(WebRouteManager<ExchangeContext, A> routeManager) {
			this.routeManager = routeManager;
		}

		@Override
		public WebRouteManager<ExchangeContext, GenericWebRoutableFacade<A>> path(String path, boolean matchTrailingSlash) throws IllegalArgumentException {
			this.routeManager.path(path, matchTrailingSlash);
			return this;
		}

		@Override
		public WebRouteManager<ExchangeContext, GenericWebRoutableFacade<A>> method(Method method) {
			this.routeManager.method(method);
			return this;
		}

		@Override
		public WebRouteManager<ExchangeContext, GenericWebRoutableFacade<A>> consumes(String mediaRange) {
			this.routeManager.consumes(mediaRange);
			return this;
		}

		@Override
		public WebRouteManager<ExchangeContext, GenericWebRoutableFacade<A>> produces(String mediaType) {
			this.routeManager.produces(mediaType);
			return this;
		}

		@Override
		public WebRouteManager<ExchangeContext, GenericWebRoutableFacade<A>> language(String language) {
			this.routeManager.language(language);
			return this;
		}
		
		@Override
		public GenericWebRoutableFacade<A> handler(ExchangeHandler<? super ExchangeContext, WebExchange<ExchangeContext>> handler) {
			this.routeManager.handler(handler);
			return GenericWebRoutableFacade.this;
		}

		@Override
		public GenericWebRoutableFacade<A> enable() {
			this.routeManager.enable();
			return GenericWebRoutableFacade.this;
		}

		@Override
		public GenericWebRoutableFacade<A> disable() {
			this.routeManager.disable();
			return GenericWebRoutableFacade.this;
		}

		@Override
		public GenericWebRoutableFacade<A> remove() {
			this.routeManager.remove();
			return GenericWebRoutableFacade.this;
		}

		@Override
		public Set<WebRoute<ExchangeContext>> findRoutes() {
			return this.routeManager.findRoutes();
		}
	}
	
	private class WebSocketRouteManagerFacade implements WebSocketRouteManager<ExchangeContext, GenericWebRoutableFacade<A>> {
		
		private final WebSocketRouteManager<ExchangeContext, A> webSocketRouteManager;

		public WebSocketRouteManagerFacade(WebSocketRouteManager<ExchangeContext, A> webSocketRouteManager) {
			this.webSocketRouteManager = webSocketRouteManager;
		}

		@Override
		public WebSocketRouteManager<ExchangeContext, GenericWebRoutableFacade<A>> path(String path, boolean matchTrailingSlash) throws IllegalArgumentException {
			this.webSocketRouteManager.path(path, matchTrailingSlash);
			return this;
		}

		@Override
		public WebSocketRouteManager<ExchangeContext, GenericWebRoutableFacade<A>> language(String language) {
			this.webSocketRouteManager.language(language);
			return this;
		}

		@Override
		public WebSocketRouteManager<ExchangeContext, GenericWebRoutableFacade<A>> subprotocol(String subprotocol) {
			this.webSocketRouteManager.subprotocol(subprotocol);
			return this;
		}

		@Override
		public GenericWebRoutableFacade<A> handler(WebSocketExchangeHandler<? super ExchangeContext, Web2SocketExchange<ExchangeContext>> handler) {
			this.webSocketRouteManager.handler(handler);
			return GenericWebRoutableFacade.this;
		}

		@Override
		public GenericWebRoutableFacade<A> enable() {
			this.webSocketRouteManager.enable();
			return GenericWebRoutableFacade.this;
		}

		@Override
		public GenericWebRoutableFacade<A> disable() {
			this.webSocketRouteManager.disable();
			return GenericWebRoutableFacade.this;
		}

		@Override
		public GenericWebRoutableFacade<A> remove() {
			this.webSocketRouteManager.remove();
			return GenericWebRoutableFacade.this;
		}

		@Override
		public Set<WebSocketRoute<ExchangeContext>> findRoutes() {
			return this.webSocketRouteManager.findRoutes();
		}
	}
}
