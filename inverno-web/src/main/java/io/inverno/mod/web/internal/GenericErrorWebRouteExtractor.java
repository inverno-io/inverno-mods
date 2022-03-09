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
class GenericErrorWebRouteExtractor implements ErrorWebRouteExtractor {

	private final GenericErrorWebRouter router;
	
	private GenericErrorWebRouteExtractor parent;
	
	private Set<ErrorWebRoute> routes;
	
	private Class<? extends Throwable> error;

	private String path;
	private URIPattern pathPattern;
	
	private String produce;
	
	private String language;

	private List<? extends ExchangeInterceptor<ExchangeContext, ErrorWebExchange<Throwable>>> interceptors;
	private Consumer<List<? extends ExchangeInterceptor<ExchangeContext, ErrorWebExchange<Throwable>>>> interceptorsUpdater;
	
	/**
	 * <p>
	 * Creates a generic error web route extractor in the specified generic error
	 * web router.
	 * </p>
	 * 
	 * @param router a generic error web router
	 */
	public GenericErrorWebRouteExtractor(GenericErrorWebRouter router) {
		this.router = router;
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
	
	private void addRoute(ErrorWebRoute route) {
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
	public Set<ErrorWebRoute> getRoutes() {
		if(this.parent != null) {
			return this.parent.getRoutes();			
		}
		else {
			return Collections.unmodifiableSet(this.routes);
		}
	}
	
	@Override
	public ErrorWebRouteExtractor error(Class<? extends Throwable> error) {
		GenericErrorWebRouteExtractor childExtractor = new GenericErrorWebRouteExtractor(this);
		childExtractor.error = error;
		return childExtractor;
	}

	@Override
	public ErrorWebRouteExtractor path(String path) {
		GenericErrorWebRouteExtractor childExtractor = new GenericErrorWebRouteExtractor(this);
		childExtractor.path = path;
		return childExtractor;
	}

	@Override
	public ErrorWebRouteExtractor pathPattern(URIPattern pathPattern) {
		GenericErrorWebRouteExtractor childExtractor = new GenericErrorWebRouteExtractor(this);
		childExtractor.pathPattern = pathPattern;
		return childExtractor;
	}
	
	@Override
	public ErrorWebRouteExtractor produces(String mediaType) {
		GenericErrorWebRouteExtractor childExtractor = new GenericErrorWebRouteExtractor(this);
		childExtractor.produce = mediaType;
		return childExtractor;
	}

	@Override
	public ErrorWebRouteExtractor language(String language) {
		GenericErrorWebRouteExtractor childExtractor = new GenericErrorWebRouteExtractor(this);
		childExtractor.language = language;
		return childExtractor;
	}

	@Override
	public ErrorWebRouteExtractor interceptors(List<? extends ExchangeInterceptor<ExchangeContext, ErrorWebExchange<Throwable>>> exchangeInterceptors, Consumer<List<? extends ExchangeInterceptor<ExchangeContext, ErrorWebExchange<Throwable>>>> updater) {
		this.interceptors = interceptors != null ? interceptors.stream()
				.map(interceptor -> {
					ExchangeInterceptor<ExchangeContext, ErrorWebExchange<Throwable>> current = interceptor;
					while(current instanceof ExchangeInterceptorWrapper) {
						current = ((ExchangeInterceptorWrapper<ExchangeContext, ErrorWebExchange<Throwable>>) current).unwrap();
					}
					return current;
				})
				.collect(Collectors.toList()) : List.of();
		this.interceptorsUpdater = interceptorsUpdater;
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void handler(ReactiveExchangeHandler<ExchangeContext, ErrorWebExchange<Throwable>> handler, boolean disabled) {
		if(handler != null) {
			GenericErrorWebRoute route = new GenericErrorWebRoute(this.getRouter());
			route.setDisabled(disabled);
	
			Class<? extends Throwable> error = this.getError();
			String produce = this.getProduce();
			String language = this.getLanguage();
			
			if(error != null) {
				route.setError(error);
			}
			if(produce != null) {
				route.setProduce(produce);
			}
			if(language != null) {
				route.setLanguage(language);
			}
			route.setHandler((ErrorExchangeHandler<Throwable, ErrorWebExchange<Throwable>>)handler);
			this.addRoute(route);
		}
	}	
}
