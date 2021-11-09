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
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.WebInterceptedRouteManager;
import io.inverno.mod.web.WebInterceptedRouter;
import io.inverno.mod.web.WebInterceptorManager;
import io.inverno.mod.web.WebRoute;
import io.inverno.mod.web.WebRouteManager;
import io.inverno.mod.web.WebRouter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
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
	
	private final List<WebRouteInterceptor<ExchangeContext>> routeInterceptors;
	
	/**
	 * <p>
	 * Creates a generic web intercepted router.
	 * </p>
	 * 
	 * @param router an abstract web router
	 */
	public GenericWebInterceptedRouter(AbstractWebRouter router) {
		this.routeInterceptors = new ArrayList<>();
		
		// We recopy here to prevent side effects, it should be documented
		if(router instanceof GenericWebInterceptedRouter) {
			this.routeInterceptors.addAll(((GenericWebInterceptedRouter) router).routeInterceptors);
			this.router = ((GenericWebInterceptedRouter) router).router;
		}
		else {
			// we might have a ClassCastException here if we are sloppy, just let it crash
			this.router = (GenericWebRouter)router;
		}
	}
	
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
	public WebInterceptedRouteManager<ExchangeContext> route() {
		return new GenericWebInterceptedRouteManager(this);
	}

	@Override
	public WebInterceptedRouter<ExchangeContext> route(Consumer<WebRouteManager<ExchangeContext>> routeConfigurer) {
		routeConfigurer.accept(this.route());
		return this;
	}
	
	@Override
	public WebInterceptorManager<ExchangeContext> interceptRoute() {
		return new GenericWebInterceptorManager(new GenericWebInterceptedRouter(this), this.contentTypeCodec, this.acceptLanguageCodec);
	}

	@Override
	public Set<WebRoute<ExchangeContext>> getRoutes() {
		return this.router.getRoutes();
	}

	@Override
	public Mono<Void> defer(Exchange<ExchangeContext> exchange) {
		return this.router.defer(exchange);
	}
	
	@Override
	public void handle(Exchange<ExchangeContext> exchange) throws HttpException {
		this.router.handle(exchange);
	}

	@Override
	public List<? extends ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>>> getInterceptors() {
		return this.routeInterceptors.stream().map(routeInterceptor -> routeInterceptor.getInterceptor()).collect(Collectors.toList());
	}

	/**
	 * <p>
	 * Scans all routes from the wrapped router and apply the interceptors. If an interceptor already exists in a route, we just move it to the top of the list, that might not be the most appropriate
	 * behavior but at least it's consistent, we'll see in practice where it goes and maybe provide ways to control this.
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
	public WebRouter<ExchangeContext> clearInterceptors() {
		return this.router;
	}
}
