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
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.web.server.ErrorWebExchange;
import io.inverno.mod.web.server.ErrorWebRouter;
import io.inverno.mod.web.server.ErrorWebRoute;
import io.inverno.mod.web.server.ErrorWebRouteManager;
import io.inverno.mod.web.server.WebServer;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * Scopes Web server instances when configuring error Web routes.
 * </p>
 *
 * <p>
 * {@link WebServer} implements {@link ErrorWebRouter} but the Web server instance can't be exposed directly when configuring error Web routes using
 * {@link WebServer#configureErrorRoutes(Configurer)} for instance as one could then do a cast leading to undesirable side effects.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the exchange context type
 */
class ErrorWebRouterFacade<A extends ExchangeContext> implements ErrorWebRouter<A> {

	private final WebServer<A> server;

	/**
	 * <p>
	 * Creates an error Web router facade wrapping the specified Web server.
	 * </p>
	 *
	 * @param server a Web server
	 */
	public ErrorWebRouterFacade(WebServer<A> server) {
		this.server = server;
	}

	@Override
	public ErrorWebRouteManager<A, ? extends ErrorWebRouter<A>> routeError() {
		return new ErrorWebRouteManagerFacade(this.server.routeError());
	}

	@Override
	@SuppressWarnings("unchecked")
	public ErrorWebRouter<A> configureErrorRoutes(Configurer<? super A> configurer) {
		if(configurer != null) {
			((Configurer<A>)configurer).configure(new ErrorWebRouterFacade<>(this.server));
		}
		return this;
	}

	@Override
	public ErrorWebRouter<A> configureErrorRoutes(List<Configurer<? super A>> configurers) {
		if(configurers != null && !configurers.isEmpty()) {
			for(ErrorWebRouter.Configurer<? super A> configurer : configurers) {
				this.configureErrorRoutes(configurer);
			}
		}
		return this;
	}

	@Override
	public Set<ErrorWebRoute<A>> getErrorRoutes() {
		return this.server.getErrorRoutes();
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
	 * Error Web route manager returning the {@link ErrorWebRouterFacade} instead of the Web server instance when setting routes.
	 * </p>
	 *
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	private class ErrorWebRouteManagerFacade implements ErrorWebRouteManager<A, ErrorWebRouter<A>> {

		private final ErrorWebRouteManager<A, ? extends WebServer<A>> routeManager;

		/**
		 * <p>
		 * Creates an error Web route manager facade.
		 * </p>
		 *
		 * @param routeManager a route manager
		 */
		public ErrorWebRouteManagerFacade(ErrorWebRouteManager<A, ? extends WebServer<A>> routeManager) {
			this.routeManager = routeManager;
		}

		@Override
		public ErrorWebRouteManager<A, ErrorWebRouter<A>> error(Class<? extends Throwable> error) {
			this.routeManager.error(error);
			return this;
		}

		@Override
		public ErrorWebRouteManager<A, ErrorWebRouter<A>> path(String path, boolean matchTrailingSlash) {
			this.routeManager.path(path, matchTrailingSlash);
			return this;
		}

		@Override
		public ErrorWebRouteManager<A, ErrorWebRouter<A>> consume(String mediaRange) {
			this.routeManager.consume(mediaRange);
			return this;
		}

		@Override
		public ErrorWebRouteManager<A, ErrorWebRouter<A>> produce(String mediaType) {
			this.routeManager.produce(mediaType);
			return this;
		}

		@Override
		public ErrorWebRouteManager<A, ErrorWebRouter<A>> language(String language) {
			this.routeManager.language(language);
			return this;
		}

		/**
		 * {@inheritDoc}
		 *
		 * <p>
		 * This method returns the error Web router facade.
		 * </p>
		 *
		 * @return the error Web router facade
		 */
		@Override
		public ErrorWebRouter<A> handler(ExchangeHandler<? super A, ErrorWebExchange<A>> handler) {
			this.routeManager.handler(handler);
			return ErrorWebRouterFacade.this;
		}

		@Override
		public ErrorWebRouter<A> enable() {
			this.routeManager.enable();
			return ErrorWebRouterFacade.this;
		}

		@Override
		public ErrorWebRouter<A> disable() {
			this.routeManager.disable();
			return ErrorWebRouterFacade.this;
		}

		@Override
		public ErrorWebRouter<A> remove() {
			this.routeManager.remove();
			return ErrorWebRouterFacade.this;
		}

		@Override
		public Set<ErrorWebRoute<A>> findRoutes() {
			return this.routeManager.findRoutes();
		}
	}
}
