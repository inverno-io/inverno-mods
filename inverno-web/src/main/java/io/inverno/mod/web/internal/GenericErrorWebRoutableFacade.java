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
package io.inverno.mod.web.internal;

import io.inverno.mod.web.*;
import java.util.Set;

/**
 * <p>
 * An {@link ErrorWebRoutable} facade for Error Web routers.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 *
 * @param <A> the type of the initial Error Web routable
 */
class GenericErrorWebRoutableFacade<A extends ErrorWebRoutable<A>> implements ErrorWebRoutable<GenericErrorWebRoutableFacade<A>> {

	private final A initialRoutable;

	public GenericErrorWebRoutableFacade(A initialRoutable) {
		this.initialRoutable = initialRoutable;
	}

	@Override
	public ErrorWebRouteManager<GenericErrorWebRoutableFacade<A>> route() {
		return new ErrorWebRouteManagerFacade(this.initialRoutable.route());
	}

	@Override
	public GenericErrorWebRoutableFacade<A> configureRoutes(ErrorWebRoutesConfigurer configurer) {
		this.initialRoutable.configureRoutes(configurer);
		return this;
	}

	@Override
	public Set<ErrorWebRoute> getRoutes() {
		return this.initialRoutable.getRoutes();
	}

	private class ErrorWebRouteManagerFacade implements ErrorWebRouteManager<GenericErrorWebRoutableFacade<A>> {

		private final ErrorWebRouteManager<A> routeManager;

		public ErrorWebRouteManagerFacade(ErrorWebRouteManager<A> routeManager) {
			this.routeManager = routeManager;
		}

		@Override
		public GenericErrorWebRoutableFacade<A> handler(ErrorWebExchangeHandler<? extends Throwable> handler) {
			this.routeManager.handler(handler);
			return GenericErrorWebRoutableFacade.this;
		}

		@Override
		public ErrorWebRouteManager<GenericErrorWebRoutableFacade<A>> error(Class<? extends Throwable> error) {
			this.routeManager.error(error);
			return this;
		}

		@Override
		public ErrorWebRouteManager<GenericErrorWebRoutableFacade<A>> path(String path, boolean matchTrailingSlash) throws IllegalArgumentException {
			this.routeManager.path(path, matchTrailingSlash);
			return this;
		}

		@Override
		public ErrorWebRouteManager<GenericErrorWebRoutableFacade<A>> produces(String mediaType) {
			this.routeManager.produces(mediaType);
			return this;
		}

		@Override
		public ErrorWebRouteManager<GenericErrorWebRoutableFacade<A>> language(String language) {
			this.routeManager.language(language);
			return this;
		}

		@Override
		public GenericErrorWebRoutableFacade<A> enable() {
			this.routeManager.enable();
			return GenericErrorWebRoutableFacade.this;
		}

		@Override
		public GenericErrorWebRoutableFacade<A> disable() {
			this.routeManager.disable();
			return GenericErrorWebRoutableFacade.this;
		}

		@Override
		public GenericErrorWebRoutableFacade<A> remove() {
			this.routeManager.remove();
			return GenericErrorWebRoutableFacade.this;
		}

		@Override
		public Set<ErrorWebRoute> findRoutes() {
			return this.routeManager.findRoutes();
		}
	}
}
