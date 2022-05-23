/*
 * Copyright 2020 Jeremy KUHN
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

import io.inverno.mod.base.net.URIPattern;
import io.inverno.mod.http.server.ErrorExchangeHandler;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.http.server.ReactiveExchangeHandler;
import io.inverno.mod.web.ErrorWebExchange;
import io.inverno.mod.web.ErrorWebRoute;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * <p>
 * Generic {@link ErrorWebRouteExtractor} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class GenericErrorWebRouteExtractor implements ErrorWebRouteExtractor<ExchangeContext> {

	private final GenericErrorWebRouter router;
	private final boolean unwrapInterceptors;
	
	private GenericErrorWebRouteExtractor parent;
	
	private Set<ErrorWebRoute<ExchangeContext>> routes;
	
	private Class<? extends Throwable> error;

	private String path;
	private URIPattern pathPattern;
	
	private String produce;
	
	private String language;

	private List<? extends ExchangeInterceptor<ExchangeContext, ErrorWebExchange<ExchangeContext>>> interceptors;
	private Consumer<List<? extends ExchangeInterceptor<ExchangeContext, ErrorWebExchange<ExchangeContext>>>> interceptorsUpdater;
	
	/**
	 * <p>
	 * Creates a generic error web route extractor in the specified generic error
	 * web router.
	 * </p>
	 * 
	 * @param router a generic error web router
	 */
	public GenericErrorWebRouteExtractor(GenericErrorWebRouter router, boolean unwrapInterceptors) {
		this.router = router;
		this.unwrapInterceptors = unwrapInterceptors;
	}

	/**
	 * <p>
	 * Creates a generic error web route extractor with the specified parent.
	 * </p>
	 * 
	 * @param parent a generic error web route extractor
	 */
	private GenericErrorWebRouteExtractor(GenericErrorWebRouteExtractor parent) {
		this.parent = parent;
		this.router = parent.router;
		this.unwrapInterceptors = parent.unwrapInterceptors;
	}
	
	private GenericErrorWebRouter getRouter() {
		return this.router;
	}

	private Class<? extends Throwable> getError() {
		if(this.error != null) {
			return this.error;
		}
		else if(parent != null) {
			return this.parent.getError();
		}
		return null;
	}

	private String getPath() {
		if(this.path != null) {
			return this.path;
		}
		else if(parent != null) {
			return this.parent.getPath();
		}
		return null;
	}

	private URIPattern getPathPattern() {
		if(this.pathPattern != null) {
			return this.pathPattern;
		}
		else if(parent != null) {
			return this.parent.getPathPattern();
		}
		return null;
	}
	
	private String getProduce() {
		if(this.produce != null) {
			return this.produce;
		}
		else if(parent != null) {
			return this.parent.getProduce();
		}
		return null;
	}
	
	private String getLanguage() {
		if(this.language != null) {
			return this.language;
		}
		else if(parent != null) {
			return this.parent.getLanguage();
		}
		return null;
	}
	
	private void addRoute(ErrorWebRoute<ExchangeContext> route) {
		if(this.parent != null) {
			this.parent.addRoute(route);
		}
		else {
			if(this.routes == null) {
				this.routes = new HashSet<>();
			}
			this.routes.add(route);
		}
	}
	
	@Override
	public Set<ErrorWebRoute<ExchangeContext>> getRoutes() {
		if(this.parent != null) {
			return this.parent.getRoutes();			
		}
		else {
			return Collections.unmodifiableSet(this.routes);
		}
	}
	
	@Override
	public GenericErrorWebRouteExtractor error(Class<? extends Throwable> error) {
		GenericErrorWebRouteExtractor childExtractor = new GenericErrorWebRouteExtractor(this);
		childExtractor.error = error;
		return childExtractor;
	}

	@Override
	public GenericErrorWebRouteExtractor path(String path) {
		GenericErrorWebRouteExtractor childExtractor = new GenericErrorWebRouteExtractor(this);
		childExtractor.path = path;
		return childExtractor;
	}

	@Override
	public GenericErrorWebRouteExtractor pathPattern(URIPattern pathPattern) {
		GenericErrorWebRouteExtractor childExtractor = new GenericErrorWebRouteExtractor(this);
		childExtractor.pathPattern = pathPattern;
		return childExtractor;
	}
	
	@Override
	public GenericErrorWebRouteExtractor produces(String mediaType) {
		GenericErrorWebRouteExtractor childExtractor = new GenericErrorWebRouteExtractor(this);
		childExtractor.produce = mediaType;
		return childExtractor;
	}

	@Override
	public GenericErrorWebRouteExtractor language(String language) {
		GenericErrorWebRouteExtractor childExtractor = new GenericErrorWebRouteExtractor(this);
		childExtractor.language = language;
		return childExtractor;
	}

	@Override
	public GenericErrorWebRouteExtractor interceptors(List<? extends ExchangeInterceptor<ExchangeContext, ErrorWebExchange<ExchangeContext>>> exchangeInterceptors, Consumer<List<? extends ExchangeInterceptor<ExchangeContext, ErrorWebExchange<ExchangeContext>>>> interceptorsUpdater) {
		this.interceptors = exchangeInterceptors != null ? exchangeInterceptors : List.of();
		if(this.unwrapInterceptors) {
			this.interceptors = this.interceptors.stream()
			.map(interceptor -> {
				ExchangeInterceptor<ExchangeContext, ErrorWebExchange<ExchangeContext>> current = interceptor;
				while(current instanceof ExchangeInterceptorWrapper) {
					current = ((ExchangeInterceptorWrapper<ExchangeContext, ErrorWebExchange<ExchangeContext>>) current).unwrap();
				}
				return current;
			})
			.collect(Collectors.toList());
		}
		this.interceptorsUpdater = interceptorsUpdater;
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void handler(ReactiveExchangeHandler<ExchangeContext, ErrorWebExchange<ExchangeContext>> handler, boolean disabled) {
		if(handler != null) {
			GenericErrorWebRoute route = new GenericErrorWebRoute(this.getRouter());
			route.setDisabled(disabled);
			
			Class<? extends Throwable> routeError = this.getError();
			String routePath = this.getPath();
			URIPattern routePathPattern = this.getPathPattern();
			String routeProduce = this.getProduce();
			String routeLanguage = this.getLanguage();
			
			if(routeError != null) {
				route.setError(routeError);
			}
			if(routePath != null) {
				route.setPath(routePath);
			}
			if(routePathPattern != null) {
				route.setPathPattern(routePathPattern);
			}
			if(routeProduce != null) {
				route.setProduce(routeProduce);
			}
			if(routeLanguage != null) {
				route.setLanguage(routeLanguage);
			}
			route.setInterceptors(this.interceptors, this.interceptorsUpdater);
			route.setHandler((ErrorExchangeHandler<ExchangeContext, ErrorWebExchange<ExchangeContext>>)handler);
			this.addRoute(route);
		}
	}	
}
