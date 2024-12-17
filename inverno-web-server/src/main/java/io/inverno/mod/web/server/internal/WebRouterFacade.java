/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.web.server.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.http.server.ws.WebSocketExchangeHandler;
import io.inverno.mod.web.server.WebExchange;
import io.inverno.mod.web.server.WebRouter;
import io.inverno.mod.web.server.WebRoute;
import io.inverno.mod.web.server.WebRouteManager;
import io.inverno.mod.web.server.WebServer;
import io.inverno.mod.web.server.WebSocketRoute;
import io.inverno.mod.web.server.WebSocketRouteManager;
import io.inverno.mod.web.server.ws.Web2SocketExchange;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * Scopes Web server instances when configuring Web routes.
 * </p>
 *
 * <p>
 * {@link WebServer} implements {@link WebRouter} but the Web server instance can't be exposed directly when configuring Web routes using {@link WebServer#configureRoutes(Configurer)} for instance as one
 * could then do a cast leading to undesirable side effects.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the exchange context type
 */
class WebRouterFacade<A extends ExchangeContext> implements WebRouter<A> {

	private final WebServer<A> server;

	/**
	 * <p>
	 * Creates a Web router facade wrapping the specified Web server.
	 * </p>
	 *
	 * @param server a Web server
	 */
	public WebRouterFacade(WebServer<A> server) {
		this.server = server;
	}

	@Override
	public WebRouteManager<A, ? extends WebRouter<A>> route() {
		return new WebRouteManagerFacade(this.server.route());
	}

	@Override
	public WebSocketRouteManager<A, ? extends WebRouter<A>> webSocketRoute() {
		return new WebSocketRouteManagerFacade(this.server.webSocketRoute());
	}

	@Override
	@SuppressWarnings("unchecked")
	public WebRouter<A> configureRoutes(Configurer<? super A> configurer) {
		if(configurer != null) {
			((Configurer<A>)configurer).configure(new WebRouterFacade<>(this.server));
		}
		return this;
	}

	@Override
	public WebRouter<A> configureRoutes(List<Configurer<? super A>> configurers) {
		if(configurers != null && !configurers.isEmpty()) {
			for(WebRouter.Configurer<? super A> configurer : configurers) {
				this.configureRoutes(configurer);
			}
		}
		return this;
	}

	@Override
	public Set<WebRoute<A>> getRoutes() {
		return this.server.getRoutes();
	}

	@Override
	public Set<WebSocketRoute<A>> getWebSocketRoutes() {
		return this.server.getWebSocketRoutes();
	}

	/**
	 * <p>
	 * Returns the wrapped Web server.
	 * </p>
	 *
	 * @return a Web server
	 */
	public WebServer<A> unwrap() {
		return this.server;
	}

	/**
	 * <p>
	 * Web route manager returning the {@link WebRouterFacade} instead of the Web server instance when setting routes.
	 * </p>
	 *
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	private class WebRouteManagerFacade implements WebRouteManager<A, WebRouter<A>> {

		private final WebRouteManager<A, ? extends WebServer<A>> routeManager;

		/**
		 * <p>
		 * Creates a Web route manager facade.
		 * </p>
		 *
		 * @param routeManager a route manager
		 */
		public WebRouteManagerFacade(WebRouteManager<A, ? extends WebServer<A>> routeManager) {
			this.routeManager = routeManager;
		}

		@Override
		public WebRouteManager<A, WebRouter<A>> path(String path, boolean matchTrailingSlash) {
			this.routeManager.path(path, matchTrailingSlash);
			return this;
		}

		@Override
		public WebRouteManager<A, WebRouter<A>> method(Method method) {
			this.routeManager.method(method);
			return this;
		}

		@Override
		public WebRouteManager<A, WebRouter<A>> consume(String mediaRange) {
			this.routeManager.consume(mediaRange);
			return this;
		}

		@Override
		public WebRouteManager<A, WebRouter<A>> produce(String mediaType) {
			this.routeManager.produce(mediaType);
			return this;
		}

		@Override
		public WebRouteManager<A, WebRouter<A>> language(String language) {
			this.routeManager.language(language);
			return this;
		}

		/**
		 * {@inheritDoc}
		 *
		 * <p>
		 * This method returns the Web router facade.
		 * </p>
		 *
		 * @return the Web router facade
		 */
		@Override
		public WebRouter<A> handler(ExchangeHandler<? super A, WebExchange<A>> handler) {
			this.routeManager.handler(handler);
			return WebRouterFacade.this;
		}

		@Override
		public WebRouter<A> enable() {
			this.routeManager.enable();
			return WebRouterFacade.this;
		}

		@Override
		public WebRouter<A> disable() {
			this.routeManager.disable();
			return WebRouterFacade.this;
		}

		@Override
		public WebRouter<A> remove() {
			this.routeManager.remove();
			return WebRouterFacade.this;
		}

		@Override
		public Set<WebRoute<A>> findRoutes() {
			return this.routeManager.findRoutes();
		}
	}

	/**
	 * <p>
	 * WebSocket route manager returning the {@link WebRouterFacade} instead of the Web server instance when setting routes.
	 * </p>
	 *
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	private class WebSocketRouteManagerFacade implements WebSocketRouteManager<A, WebRouter<A>> {

		private final WebSocketRouteManager<A, ? extends WebServer<A>> routeManager;

		/**
		 * <p>
		 * Creates a WebSocket route manager facade.
		 * </p>
		 *
		 * @param routeManager a route manager
		 */
		public WebSocketRouteManagerFacade(WebSocketRouteManager<A, ? extends WebServer<A>> routeManager) {
			this.routeManager = routeManager;
		}

		@Override
		public WebSocketRouteManager<A, WebRouter<A>> path(String path, boolean matchTrailingSlash) {
			this.routeManager.path(path, matchTrailingSlash);
			return this;
		}

		@Override
		public WebSocketRouteManager<A, WebRouter<A>> language(String language) {
			this.routeManager.language(language);
			return this;
		}

		@Override
		public WebSocketRouteManager<A, WebRouter<A>> subprotocol(String subprotocol) {
			this.routeManager.subprotocol(subprotocol);
			return this;
		}

		/**
		 * {@inheritDoc}
		 *
		 * <p>
		 * This method returns the Web router facade.
		 * </p>
		 *
		 * @return the Web router facade
		 */
		@Override
		public WebRouter<A> handler(WebSocketExchangeHandler<? super A, Web2SocketExchange<A>> handler) {
			this.routeManager.handler(handler);
			return WebRouterFacade.this;
		}

		@Override
		public WebRouter<A> enable() {
			this.routeManager.enable();
			return WebRouterFacade.this;
		}

		@Override
		public WebRouter<A> disable() {
			this.routeManager.disable();
			return WebRouterFacade.this;
		}

		@Override
		public WebRouter<A> remove() {
			this.routeManager.remove();
			return WebRouterFacade.this;
		}

		@Override
		public Set<WebSocketRoute<A>> findRoutes() {
			return this.routeManager.findRoutes();
		}
	}
}
