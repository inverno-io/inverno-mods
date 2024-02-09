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
package io.inverno.mod.web.server.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.web.server.ErrorWebExchange;
import io.inverno.mod.web.server.ErrorWebInterceptedRouter;
import io.inverno.mod.web.server.ErrorWebInterceptorManager;
import io.inverno.mod.web.server.ErrorWebInterceptorsConfigurer;
import io.inverno.mod.web.server.ErrorWebRoute;
import io.inverno.mod.web.server.ErrorWebRouteManager;
import io.inverno.mod.web.server.ErrorWebRouter;
import io.inverno.mod.web.server.ErrorWebRouterConfigurer;
import io.inverno.mod.web.server.ErrorWebRoutesConfigurer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link ErrorWebInterceptedRouter} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
class GenericErrorWebInterceptedRouter extends AbstractErrorWebRouter implements ErrorWebInterceptedRouter<ExchangeContext> {

	private final GenericErrorWebRouter router;

	private final ErrorWebRouterFacade routerFacade;

	private final List<ErrorWebRouteInterceptor<ExchangeContext>> routeInterceptors;

	/**
	 * <p>
	 * Creates a generic error web intercepted router from a web router.
	 * </p>
	 *
	 * @param router an error web router
	 */
	public GenericErrorWebInterceptedRouter(GenericErrorWebRouter router) {
		this.routeInterceptors = new ArrayList<>();
		this.router = router;
		this.routerFacade = null;
	}

	/**
	 * <p>
	 * Creates a generic error web intercepted router from an error web intercepted router.
	 * </p>
	 *
	 * @param interceptedRouter an error web intercepted router
	 */
	public GenericErrorWebInterceptedRouter(GenericErrorWebInterceptedRouter interceptedRouter) {
		this.routeInterceptors = new ArrayList<>();
		this.routeInterceptors.addAll(interceptedRouter.routeInterceptors);
		this.router = interceptedRouter.router;
		this.routerFacade = interceptedRouter.routerFacade;
	}

	/**
	 * <p>
	 * Creates a generic error web intercepted router from an error web router facade.
	 * </p>
	 *
	 * @param routerFacade an error web router facade
	 */
	public GenericErrorWebInterceptedRouter(ErrorWebRouterFacade routerFacade) {
		this.routeInterceptors = new ArrayList<>();
		this.routeInterceptors.addAll(routerFacade.interceptedRouter.routeInterceptors);
		this.router = routerFacade.interceptedRouter.router;
		this.routerFacade = routerFacade;
	}

	/**
	 * <p>
	 * Adds the specified error web route interceptor to the error web intercepted router.
	 * </p>
	 *
	 * @param routeInterceptor an error web route interceptor
	 */
	void addRouteInterceptor(ErrorWebRouteInterceptor<ExchangeContext> routeInterceptor) {
		this.routeInterceptors.add(routeInterceptor);
	}

	@Override
	void setRoute(ErrorWebRoute<ExchangeContext> route) {
		route.setInterceptors(this.routeInterceptors.stream()
			.map(interceptor -> interceptor.matches(route))
			.filter(Objects::nonNull)
			.map(routeInterceptor -> routeInterceptor.getInterceptor())
			.collect(Collectors.toList())
		);
		this.router.setRoute(route);
	}

	@Override
	void enableRoute(ErrorWebRoute<ExchangeContext> route) {
		this.router.enableRoute(route);
	}

	@Override
	void disableRoute(ErrorWebRoute<ExchangeContext> route) {
		this.router.disableRoute(route);
	}

	@Override
	void removeRoute(ErrorWebRoute<ExchangeContext> route) {
		this.router.removeRoute(route);
	}

	@Override
	public ErrorWebInterceptorManager<ExchangeContext, ErrorWebInterceptedRouter<ExchangeContext>> intercept() {
		return new GenericErrorWebInterceptorManager(new GenericErrorWebInterceptedRouter(this), CONTENT_TYPE_CODEC, ACCEPT_LANGUAGE_CODEC);
	}

	@Override
	public ErrorWebRouteManager<ExchangeContext, ErrorWebInterceptedRouter<ExchangeContext>> route() {
		return new GenericErrorWebInterceptedRouteManager(this);
	}

	@Override
	public Set<ErrorWebRoute<ExchangeContext>> getRoutes() {
		return this.router.getRoutes();
	}

	@Override
	public List<? extends ExchangeInterceptor<ExchangeContext, ErrorWebExchange<ExchangeContext>>> getInterceptors() {
		return this.routeInterceptors.stream().map(routeInterceptor -> routeInterceptor.getInterceptor()).collect(Collectors.toList());
	}

	/**
	 * <p>
	 * Scans all routes from the wrapped router and apply the interceptors defined in the intercepted router. 
	 * </p>
	 * 
	 * <p>
	 * If interceptors are already specified on the route, they will be evaluated after the ones defined in the intercepted router. 
	 * </p>
	 */
	@Override
	public ErrorWebInterceptedRouter<ExchangeContext> applyInterceptors() {
		this.router.getFilteredRoutes().stream().forEach(route -> {
			LinkedList<ExchangeInterceptor<ExchangeContext, ErrorWebExchange<ExchangeContext>>> interceptors = new LinkedList<>(route.getInterceptors());
			this.routeInterceptors.stream()
					.map(routeInterceptor -> routeInterceptor.matches(route))
					.filter(Objects::nonNull)
					.forEach(routeInterceptor -> {
						// TODO deal with wrapped interceptors
						interceptors.remove(routeInterceptor.getInterceptor());
						interceptors.add(routeInterceptor.getInterceptor());
					});
			route.setInterceptors(interceptors);
		});
		return this;
	}

	@Override
	public ErrorWebRouter<ExchangeContext> getRouter() {
		return (this.router != null ? this.router : this.routerFacade);
	}

	@Override
	public ErrorWebInterceptedRouter<ExchangeContext> configure(ErrorWebRouterConfigurer<? super ExchangeContext> configurer) {
		if(configurer != null) {
			new ErrorWebRouterFacade().configure(configurer);
		}
		return this;
	}

	@Override
	public ErrorWebInterceptedRouter<ExchangeContext> configureInterceptors(ErrorWebInterceptorsConfigurer<? super ExchangeContext> configurer) {
		if(configurer != null) {
			GenericErrorWebInterceptableFacade facade = new GenericErrorWebInterceptableFacade(this);
			configurer.configure(facade);

			return facade.getInterceptedRouter();
		}
		return this;
	}

	@Override
	public ErrorWebInterceptedRouter<ExchangeContext> configureInterceptors(List<ErrorWebInterceptorsConfigurer<? super ExchangeContext>> configurers) {
		if(configurers != null && !configurers.isEmpty()) {
			GenericErrorWebInterceptableFacade facade = new GenericErrorWebInterceptableFacade(this);
			for(ErrorWebInterceptorsConfigurer<? super ExchangeContext> configurer : configurers) {
				configurer.configure(facade);
			}
			return facade.getInterceptedRouter();
		}
		return this;
	}

	@Override
	public ErrorWebInterceptedRouter<ExchangeContext> configureRoutes(ErrorWebRoutesConfigurer<? super ExchangeContext> configurer) {
		GenericErrorWebRoutableFacade<ErrorWebInterceptedRouter<ExchangeContext>> facade = new GenericErrorWebRoutableFacade<>(this);
		configurer.configure(facade);
		return this;
	}

	private class ErrorWebRouterFacade implements ErrorWebRouter<ExchangeContext> {

		private final GenericErrorWebInterceptedRouter interceptedRouter;

		public ErrorWebRouterFacade() {
			this.interceptedRouter = GenericErrorWebInterceptedRouter.this;
		}

		@Override
		public ErrorWebInterceptorManager<ExchangeContext, ErrorWebInterceptedRouter<ExchangeContext>> intercept() {
			return new GenericErrorWebInterceptorManager(new GenericErrorWebInterceptedRouter(this), CONTENT_TYPE_CODEC, ACCEPT_LANGUAGE_CODEC);
		}

		@Override
		public ErrorWebRouteManager<ExchangeContext, ErrorWebRouter<ExchangeContext>> route() {
			return new ErrorWebRouteManagerFacade(GenericErrorWebInterceptedRouter.this.route());
		}

		@Override
		public Set<ErrorWebRoute<ExchangeContext>> getRoutes() {
			return GenericErrorWebInterceptedRouter.this.getRoutes();
		}

		@Override
		public void handle(ErrorExchange<ExchangeContext> exchange) throws HttpException {
			GenericErrorWebInterceptedRouter.this.getRouter().handle(exchange);
		}

		@Override
		public Mono<Void> defer(ErrorExchange<ExchangeContext> exchange) {
			return GenericErrorWebInterceptedRouter.this.getRouter().defer(exchange);
		}

		@Override
		public ErrorWebRouter<ExchangeContext> configureRoutes(ErrorWebRoutesConfigurer<? super ExchangeContext> configurer) {
			GenericErrorWebInterceptedRouter.this.configureRoutes(configurer);
			return this;
		}

		@Override
		public ErrorWebInterceptedRouter<ExchangeContext> configureInterceptors(ErrorWebInterceptorsConfigurer<? super ExchangeContext> configurer) {
			GenericErrorWebInterceptedRouter childInterceptedRouter = new GenericErrorWebInterceptedRouter(this);
			if(configurer != null) {
				GenericErrorWebInterceptableFacade facade = new GenericErrorWebInterceptableFacade(childInterceptedRouter);
				configurer.configure(facade);

				return facade.getInterceptedRouter();
			}
			return childInterceptedRouter;
		}

		@Override
		public ErrorWebInterceptedRouter<ExchangeContext> configureInterceptors(List<ErrorWebInterceptorsConfigurer<? super ExchangeContext>> configurers) {
			GenericErrorWebInterceptedRouter childInterceptedRouter = new GenericErrorWebInterceptedRouter(this);
			if(configurers != null && !configurers.isEmpty()) {
				GenericErrorWebInterceptableFacade facade = new GenericErrorWebInterceptableFacade(childInterceptedRouter);
				for(ErrorWebInterceptorsConfigurer<? super ExchangeContext> configurer : configurers) {
					configurer.configure(facade);
				}
				return facade.getInterceptedRouter();
			}
			return childInterceptedRouter;
		}

		private class ErrorWebRouteManagerFacade implements ErrorWebRouteManager<ExchangeContext, ErrorWebRouter<ExchangeContext>> {

			private final ErrorWebRouteManager<ExchangeContext, ErrorWebInterceptedRouter<ExchangeContext>> routeManager;

			public ErrorWebRouteManagerFacade(ErrorWebRouteManager<ExchangeContext, ErrorWebInterceptedRouter<ExchangeContext>> routeManager) {
				this.routeManager = routeManager;
			}

			@Override
			public ErrorWebRouter<ExchangeContext> handler(ExchangeHandler<? super ExchangeContext, ErrorWebExchange<ExchangeContext>> handler) {
				this.routeManager.handler(handler);
				return ErrorWebRouterFacade.this;
			}

			@Override
			public ErrorWebRouteManager<ExchangeContext, ErrorWebRouter<ExchangeContext>> error(Class<? extends Throwable> error) {
				this.routeManager.error(error);
				return this;
			}

			@Override
			public ErrorWebRouteManager<ExchangeContext, ErrorWebRouter<ExchangeContext>> path(String path, boolean matchTrailingSlash) throws IllegalArgumentException {
				this.routeManager.path(path, matchTrailingSlash);
				return this;
			}

			@Override
			public ErrorWebRouteManager<ExchangeContext, ErrorWebRouter<ExchangeContext>> produces(String mediaType) {
				this.routeManager.produces(mediaType);
				return this;
			}

			@Override
			public ErrorWebRouteManager<ExchangeContext, ErrorWebRouter<ExchangeContext>> language(String language) {
				this.routeManager.language(language);
				return this;
			}

			@Override
			public ErrorWebRouter<ExchangeContext> enable() {
				this.routeManager.enable();
				return ErrorWebRouterFacade.this;
			}

			@Override
			public ErrorWebRouter<ExchangeContext> disable() {
				this.routeManager.disable();
				return ErrorWebRouterFacade.this;
			}

			@Override
			public ErrorWebRouter<ExchangeContext> remove() {
				this.routeManager.remove();
				return ErrorWebRouterFacade.this;
			}

			@Override
			public Set<ErrorWebRoute<ExchangeContext>> findRoutes() {
				return this.routeManager.findRoutes();
			}
		}
	}
}
