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

import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.WebExchangeHandler;
import io.inverno.mod.web.WebInterceptedRouter;
import io.inverno.mod.web.WebInterceptorManager;
import io.inverno.mod.web.WebInterceptorsConfigurer;
import io.inverno.mod.web.WebRoute;
import io.inverno.mod.web.WebRouteManager;
import io.inverno.mod.web.WebRouter;
import io.inverno.mod.web.WebRouterConfigurer;
import io.inverno.mod.web.WebRoutesConfigurer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link WebInterceptedRouter} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 */
class GenericWebInterceptedRouter extends AbstractWebRouter implements WebInterceptedRouter<ExchangeContext> {

	private final GenericWebRouter router;
	
	private final WebRouterFacade routerFacade;
	
	private final List<WebRouteInterceptor<ExchangeContext>> routeInterceptors;
	
	/**
	 * <p>
	 * Creates a generic web intercepted router from a web router.
	 * </p>
	 * 
	 * @param router a web router
	 */
	public GenericWebInterceptedRouter(GenericWebRouter router) {
		this.routeInterceptors = new ArrayList<>();
		this.router = router;
		this.routerFacade = null;
	}
	
	/**
	 * <p>
	 * Creates a generic web intercepted router from a web intercepted router.
	 * </p>
	 * 
	 * @param interceptedRouter a web intercepted router
	 */
	private GenericWebInterceptedRouter(GenericWebInterceptedRouter interceptedRouter) {
		this.routeInterceptors = new ArrayList<>();
		this.routeInterceptors.addAll(interceptedRouter.routeInterceptors);
		this.router = interceptedRouter.router;
		this.routerFacade = interceptedRouter.routerFacade;
	}
	
	/**
	 * <p>
	 * Creates a generic web intercepted router from a web router facade.
	 * </p>
	 * 
	 * @param routerFacade a web router facade
	 */
	private GenericWebInterceptedRouter(WebRouterFacade routerFacade) {
		this.routeInterceptors = new ArrayList<>();
		this.routeInterceptors.addAll(routerFacade.interceptedRouter.routeInterceptors);
		this.router = routerFacade.interceptedRouter.router;
		this.routerFacade = routerFacade;
	}

	/**	
	 * <p>
	 * Adds the specified web route interceptor to the web intercepted router.
	 * </p>
	 * 
	 * @param routeInterceptor a web route interceptor
	 */
	void addRouteInterceptor(WebRouteInterceptor<ExchangeContext> routeInterceptor) {
		this.routeInterceptors.add(routeInterceptor);
	}
	
	@Override
	void setRoute(WebRoute<ExchangeContext> route) {
		route.setInterceptors(this.routeInterceptors.stream()
			.map(interceptor -> interceptor.matches(route))
			.filter(Objects::nonNull)
			.map(routeInterceptor -> routeInterceptor.getInterceptor())
			.collect(Collectors.toList())
		);
		this.router.setRoute(route);
	}

	@Override
	void enableRoute(WebRoute<ExchangeContext> route) {
		this.router.enableRoute(route);
	}

	@Override
	void disableRoute(WebRoute<ExchangeContext> route) {
		this.router.disableRoute(route);
	}

	@Override
	void removeRoute(WebRoute<ExchangeContext> route) {
		this.router.removeRoute(route);
	}

	@Override
	public WebInterceptorManager<ExchangeContext, WebInterceptedRouter<ExchangeContext>> intercept() {
		return new GenericWebInterceptorManager(new GenericWebInterceptedRouter(this), CONTENT_TYPE_CODEC , ACCEPT_LANGUAGE_CODEC);
	}

	@Override
	public WebRouteManager<ExchangeContext, WebInterceptedRouter<ExchangeContext>> route() {
		return new GenericWebInterceptedRouteManager(this);
	}

	@Override
	public Set<WebRoute<ExchangeContext>> getRoutes() {
		return this.router.getRoutes();
	}

	@Override
	public List<? extends ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>>> getInterceptors() {
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
	public WebInterceptedRouter<ExchangeContext> applyInterceptors() {
		this.router.getRoutes().stream().forEach(route -> {
			LinkedList<ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>>> interceptors = new LinkedList<>(route.getInterceptors());
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
	public WebRouter<ExchangeContext> getRouter() {
		return (this.router != null ? this.router : this.routerFacade);
	}

	@Override
	public WebInterceptedRouter<ExchangeContext> configure(WebRouterConfigurer<? super ExchangeContext> configurer) {
		if(configurer != null) {
			new WebRouterFacade().configure(configurer);
		}
		return this;
	}
	
	@Override
	public WebInterceptedRouter<ExchangeContext> configureInterceptors(WebInterceptorsConfigurer<? super ExchangeContext> configurer) {
		if(configurer != null) {
			GenericWebInterceptableFacade facade = new GenericWebInterceptableFacade(this);
			configurer.accept(facade);
			
			return facade.getInterceptedRouter();
		}
		return this;
	}

	@Override
	public WebInterceptedRouter<ExchangeContext> configureInterceptors(List<WebInterceptorsConfigurer<? super ExchangeContext>> configurers) {
		if(configurers != null && !configurers.isEmpty()) {
			GenericWebInterceptableFacade facade = new GenericWebInterceptableFacade(this);
			for(WebInterceptorsConfigurer<? super ExchangeContext> configurer : configurers) {
				configurer.accept(facade);
			}
			return facade.getInterceptedRouter();
		}
		return this;
	}

	@Override
	public WebInterceptedRouter<ExchangeContext> configureRoutes(WebRoutesConfigurer<? super ExchangeContext> configurer) {
		GenericWebRoutableFacade<WebInterceptedRouter<ExchangeContext>> facade = new GenericWebRoutableFacade<>(this);
		configurer.accept(facade);
		return this;
	}
	
	private class WebRouterFacade implements WebRouter<ExchangeContext> {
		
		private final GenericWebInterceptedRouter interceptedRouter;

		public WebRouterFacade() {
			this.interceptedRouter = GenericWebInterceptedRouter.this;
		}
		
		@Override
		public WebInterceptorManager<ExchangeContext, WebInterceptedRouter<ExchangeContext>> intercept() {
			return new GenericWebInterceptorManager(new GenericWebInterceptedRouter(this), CONTENT_TYPE_CODEC , ACCEPT_LANGUAGE_CODEC);
		}
		
		@Override
		public WebRouteManager<ExchangeContext, WebRouter<ExchangeContext>> route() {
			return new WebRouteManagerFacade(GenericWebInterceptedRouter.this.route());
		}

		@Override
		public Set<WebRoute<ExchangeContext>> getRoutes() {
			return GenericWebInterceptedRouter.this.getRoutes();
		}

		@Override
		public void handle(Exchange<ExchangeContext> exchange) throws HttpException {
			GenericWebInterceptedRouter.this.getRouter().handle(exchange);
		}

		@Override
		public Mono<Void> defer(Exchange<ExchangeContext> exchange) {
			return GenericWebInterceptedRouter.this.getRouter().defer(exchange);
		}

		@Override
		public WebRouter<ExchangeContext> configureRoutes(WebRoutesConfigurer<? super ExchangeContext> configurer) {
			GenericWebInterceptedRouter.this.configureRoutes(configurer);
			return this;
		}

		@Override
		public WebInterceptedRouter<ExchangeContext> configureInterceptors(WebInterceptorsConfigurer<? super ExchangeContext> configurer) {
			GenericWebInterceptedRouter interceptedRouter = new GenericWebInterceptedRouter(this);
			if(configurer != null) {
				GenericWebInterceptableFacade facade = new GenericWebInterceptableFacade(interceptedRouter);
				configurer.accept(facade);

				return facade.getInterceptedRouter();
			}
			return interceptedRouter;
		}

		@Override
		public WebInterceptedRouter<ExchangeContext> configureInterceptors(List<WebInterceptorsConfigurer<? super ExchangeContext>> configurers) {
			GenericWebInterceptedRouter interceptedRouter = new GenericWebInterceptedRouter(this);
			if(configurers != null && !configurers.isEmpty()) {
				GenericWebInterceptableFacade facade = new GenericWebInterceptableFacade(interceptedRouter);
				for(WebInterceptorsConfigurer<? super ExchangeContext> configurer : configurers) {
					configurer.accept(facade);
				}
				return facade.getInterceptedRouter();
			}
			return interceptedRouter;
		}

		private class WebRouteManagerFacade implements WebRouteManager<ExchangeContext, WebRouter<ExchangeContext>> {

			private final WebRouteManager<ExchangeContext, WebInterceptedRouter<ExchangeContext>> routeManager;

			public WebRouteManagerFacade(WebRouteManager<ExchangeContext, WebInterceptedRouter<ExchangeContext>> routeManager) {
				this.routeManager = routeManager;
			}
			
			@Override
			public WebRouter<ExchangeContext> handler(WebExchangeHandler<? super ExchangeContext> handler) {
				this.routeManager.handler(handler);
				return WebRouterFacade.this;
			}

			@Override
			public WebRouteManager<ExchangeContext, WebRouter<ExchangeContext>> path(String path, boolean matchTrailingSlash) throws IllegalArgumentException {
				this.routeManager.path(path, matchTrailingSlash);
				return this;
			}

			@Override
			public WebRouteManager<ExchangeContext, WebRouter<ExchangeContext>> method(Method method) {
				this.routeManager.method(method);
				return this;
			}

			@Override
			public WebRouteManager<ExchangeContext, WebRouter<ExchangeContext>> consumes(String mediaRange) {
				this.routeManager.consumes(mediaRange);
				return this;
			}

			@Override
			public WebRouteManager<ExchangeContext, WebRouter<ExchangeContext>> produces(String mediaType) {
				this.routeManager.produces(mediaType);
				return this;
			}

			@Override
			public WebRouteManager<ExchangeContext, WebRouter<ExchangeContext>> language(String language) {
				this.routeManager.language(language);
				return this;
			}

			@Override
			public WebRouter<ExchangeContext> enable() {
				this.routeManager.enable();
				return WebRouterFacade.this;
			}

			@Override
			public WebRouter<ExchangeContext> disable() {
				this.routeManager.disable();
				return WebRouterFacade.this;
			}

			@Override
			public WebRouter<ExchangeContext> remove() {
				this.routeManager.remove();
				return WebRouterFacade.this;
			}

			@Override
			public Set<WebRoute<ExchangeContext>> findRoutes() {
				return this.routeManager.findRoutes();
			}
		}
	}
}
