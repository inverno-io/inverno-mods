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

import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.web.*;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * Generic {@link ErrorWebInterceptedRouter} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
class GenericErrorWebInterceptedRouter extends AbstractErrorWebRouter implements ErrorWebInterceptedRouter {

	private final GenericErrorWebRouter router;

	private final ErrorWebRouterFacade routerFacade;

	private final List<ErrorWebRouteInterceptor> routeInterceptors;

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
	void addRouteInterceptor(ErrorWebRouteInterceptor routeInterceptor) {
		this.routeInterceptors.add(routeInterceptor);
	}

	@Override
	void setRoute(ErrorWebRoute route) {
		route.setInterceptors(this.routeInterceptors.stream()
			.map(interceptor -> interceptor.matches(route))
			.filter(Objects::nonNull)
			.map(routeInterceptor -> routeInterceptor.getInterceptor())
			.collect(Collectors.toList())
		);
		this.router.setRoute(route);
	}

	@Override
	void enableRoute(ErrorWebRoute route) {
		this.router.enableRoute(route);
	}

	@Override
	void disableRoute(ErrorWebRoute route) {
		this.router.disableRoute(route);
	}

	@Override
	void removeRoute(ErrorWebRoute route) {
		this.router.removeRoute(route);
	}

	@Override
	public ErrorWebInterceptorManager<ErrorWebInterceptedRouter> intercept() {
		return new GenericErrorWebInterceptorManager(new GenericErrorWebInterceptedRouter(this), CONTENT_TYPE_CODEC, ACCEPT_LANGUAGE_CODEC);
	}

	@Override
	public ErrorWebRouteManager<ErrorWebInterceptedRouter> route() {
		return new GenericErrorWebInterceptedRouteManager(this);
	}

	@Override
	public Set<ErrorWebRoute> getRoutes() {
		return this.router.getRoutes();
	}

	@Override
	public List<? extends ExchangeInterceptor<ExchangeContext, ErrorWebExchange<Throwable>>> getInterceptors() {
		return this.routeInterceptors.stream().map(routeInterceptor -> routeInterceptor.getInterceptor()).collect(Collectors.toList());
	}

	/**
	 * <p>
	 * Scans all routes from the wrapped router and apply the interceptors. If an interceptor already exists in a route,
	 * we just move it to the top of the list, that might not be the most appropriate behavior but at least it's
	 * consistent, we'll see in practice where it goes and maybe provide ways to control this.
	 * </p>
	 */
	@Override
	public ErrorWebInterceptedRouter applyInterceptors() {
		this.router.getRoutes().stream().forEach(route -> {
			LinkedList<ExchangeInterceptor<ExchangeContext, ErrorWebExchange<Throwable>>> interceptors = new LinkedList<>(route.getInterceptors());
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
	public ErrorWebRouter getRouter() {
		return (this.router != null ? this.router : this.routerFacade);
	}

	@Override
	public ErrorWebInterceptedRouter configure(ErrorWebRouterConfigurer configurer) {
		if(configurer != null) {
			new ErrorWebRouterFacade().configure(configurer);
		}
		return this;
	}

	@Override
	public ErrorWebInterceptedRouter configureInterceptors(ErrorWebInterceptorsConfigurer configurer) {
		if(configurer != null) {
			GenericErrorWebInterceptableFacade facade = new GenericErrorWebInterceptableFacade(this);
			configurer.configure(facade);

			return facade.getInterceptedRouter();
		}
		return this;
	}

	@Override
	public ErrorWebInterceptedRouter configureInterceptors(List<ErrorWebInterceptorsConfigurer> configurers) {
		if(configurers != null && !configurers.isEmpty()) {
			GenericErrorWebInterceptableFacade facade = new GenericErrorWebInterceptableFacade(this);
			for(ErrorWebInterceptorsConfigurer configurer : configurers) {
				configurer.configure(facade);
			}
			return facade.getInterceptedRouter();
		}
		return this;
	}

	@Override
	public ErrorWebInterceptedRouter configureRoutes(ErrorWebRoutesConfigurer configurer) {
		GenericErrorWebRoutableFacade<ErrorWebInterceptedRouter> facade = new GenericErrorWebRoutableFacade<>(this);
		configurer.configure(facade);
		return this;
	}

	private class ErrorWebRouterFacade implements ErrorWebRouter {

		private final GenericErrorWebInterceptedRouter interceptedRouter;

		public ErrorWebRouterFacade() {
			this.interceptedRouter = GenericErrorWebInterceptedRouter.this;
		}

		@Override
		public ErrorWebInterceptorManager<ErrorWebInterceptedRouter> intercept() {
			return new GenericErrorWebInterceptorManager(new GenericErrorWebInterceptedRouter(this), CONTENT_TYPE_CODEC, ACCEPT_LANGUAGE_CODEC);
		}

		@Override
		public ErrorWebRouteManager<ErrorWebRouter> route() {
			return new ErrorWebRouteManagerFacade(GenericErrorWebInterceptedRouter.this.route());
		}

		@Override
		public Set<ErrorWebRoute> getRoutes() {
			return GenericErrorWebInterceptedRouter.this.getRoutes();
		}

		@Override
		public void handle(ErrorExchange<Throwable> exchange) throws HttpException {
			GenericErrorWebInterceptedRouter.this.getRouter().handle(exchange);
		}

		@Override
		public Mono<Void> defer(ErrorExchange<Throwable> exchange) {
			return GenericErrorWebInterceptedRouter.this.getRouter().defer(exchange);
		}

		@Override
		public ErrorWebRouter configureRoutes(ErrorWebRoutesConfigurer configurer) {
			GenericErrorWebInterceptedRouter.this.configureRoutes(configurer);
			return this;
		}

		@Override
		public ErrorWebInterceptedRouter configureInterceptors(ErrorWebInterceptorsConfigurer configurer) {
			GenericErrorWebInterceptedRouter interceptedRouter = new GenericErrorWebInterceptedRouter(this);
			if(configurer != null) {
				GenericErrorWebInterceptableFacade facade = new GenericErrorWebInterceptableFacade(interceptedRouter);
				configurer.configure(facade);

				return facade.getInterceptedRouter();
			}
			return interceptedRouter;
		}

		@Override
		public ErrorWebInterceptedRouter configureInterceptors(List<ErrorWebInterceptorsConfigurer> configurers) {
			GenericErrorWebInterceptedRouter interceptedRouter = new GenericErrorWebInterceptedRouter(this);
			if(configurers != null && !configurers.isEmpty()) {
				GenericErrorWebInterceptableFacade facade = new GenericErrorWebInterceptableFacade(interceptedRouter);
				for(ErrorWebInterceptorsConfigurer configurer : configurers) {
					configurer.configure(facade);
				}
				return facade.getInterceptedRouter();
			}
			return interceptedRouter;
		}

		private class ErrorWebRouteManagerFacade implements ErrorWebRouteManager<ErrorWebRouter> {

			private final ErrorWebRouteManager<ErrorWebInterceptedRouter> routeManager;

			public ErrorWebRouteManagerFacade(ErrorWebRouteManager<ErrorWebInterceptedRouter> routeManager) {
				this.routeManager = routeManager;
			}

			@Override
			public ErrorWebRouter handler(ErrorWebExchangeHandler<? extends Throwable> handler) {
				this.routeManager.handler(handler);
				return ErrorWebRouterFacade.this;
			}

			@Override
			public ErrorWebRouteManager<ErrorWebRouter> error(Class<? extends Throwable> error) {
				this.routeManager.error(error);
				return this;
			}

			@Override
			public ErrorWebRouteManager<ErrorWebRouter> path(String path, boolean matchTrailingSlash) throws IllegalArgumentException {
				this.routeManager.path(path, matchTrailingSlash);
				return this;
			}

			@Override
			public ErrorWebRouteManager<ErrorWebRouter> produces(String mediaType) {
				this.routeManager.produces(mediaType);
				return this;
			}

			@Override
			public ErrorWebRouteManager<ErrorWebRouter> language(String language) {
				this.routeManager.language(language);
				return this;
			}

			@Override
			public ErrorWebRouter enable() {
				this.routeManager.enable();
				return ErrorWebRouterFacade.this;
			}

			@Override
			public ErrorWebRouter disable() {
				this.routeManager.disable();
				return ErrorWebRouterFacade.this;
			}

			@Override
			public ErrorWebRouter remove() {
				this.routeManager.remove();
				return ErrorWebRouterFacade.this;
			}

			@Override
			public Set<ErrorWebRoute> findRoutes() {
				return this.routeManager.findRoutes();
			}
		}
	}
}
