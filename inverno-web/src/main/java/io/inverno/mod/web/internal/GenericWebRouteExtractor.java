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
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.http.server.ReactiveExchangeHandler;
import io.inverno.mod.http.server.ws.WebSocketExchangeHandler;
import io.inverno.mod.web.Web2SocketExchange;
import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.WebRoute;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * <p>
 * Generic {@link WebRouteExtractor} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class GenericWebRouteExtractor implements WebRouteExtractor<ExchangeContext, WebRoute<ExchangeContext>, GenericWebRouteExtractor>, WebSocketRouteExtractor<ExchangeContext, WebRoute<ExchangeContext>, GenericWebRouteExtractor> {

	private final GenericWebRouter router;
	private final boolean unwrapInterceptors;
	
	private GenericWebRouteExtractor parent;
	
	private Set<WebRoute<ExchangeContext>> routes;
	
	private String path;
	private URIPattern pathPattern;
	
	private Method method;
	
	private String consume;
	
	private String produce;
	
	private String language;
	
	private String subprotocol;
	
	private List<? extends ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>>> interceptors;
	private Consumer<List<? extends ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>>>> interceptorsUpdater;
	
	/**
	 * <p>
	 * Creates a generic web route extractor in the specified generic web router.
	 * </p>
	 * 
	 * @param router a generic web router
	 */
	public GenericWebRouteExtractor(GenericWebRouter router, boolean unwrapInterceptors) {
		this.router = router;
		this.unwrapInterceptors = unwrapInterceptors;
	}
	
	/**
	 * <p>
	 * Creates a generic web route extractor with the specified parent.
	 * </p>
	 * 
	 * @param parent a generic web route extractor
	 */
	private GenericWebRouteExtractor(GenericWebRouteExtractor parent) {
		this.parent = parent;
		this.router = parent.router;
		this.unwrapInterceptors = parent.unwrapInterceptors;
	}
	
	private GenericWebRouter getRouter() {
		return this.router;
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
	
	private Method getMethod() {
		if(this.method != null) {
			return this.method;
		}
		else if(parent != null) {
			return this.parent.getMethod();
		}
		return null;
	}
	
	private String getConsume() {
		if(this.consume != null) {
			return this.consume;
		}
		else if(parent != null) {
			return this.parent.getConsume();
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
	
	private void addRoute(WebRoute<ExchangeContext> route) {
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
	public Set<WebRoute<ExchangeContext>> getRoutes() {
		if(this.parent != null) {
			return this.parent.getRoutes();			
		}
		else {
			return Collections.unmodifiableSet(this.routes);
		}
	}
	
	@Override
	public GenericWebRouteExtractor path(String path) {
		GenericWebRouteExtractor childExtractor = new GenericWebRouteExtractor(this);
		childExtractor.path = path;
		return childExtractor;
	}

	@Override
	public GenericWebRouteExtractor pathPattern(URIPattern pathPattern) {
		GenericWebRouteExtractor childExtractor = new GenericWebRouteExtractor(this);
		childExtractor.pathPattern = pathPattern;
		return childExtractor;
	}
	
	@Override
	public GenericWebRouteExtractor method(Method method) {
		GenericWebRouteExtractor childExtractor = new GenericWebRouteExtractor(this);
		childExtractor.method = method;
		return childExtractor;
	}

	@Override
	public GenericWebRouteExtractor consumes(String mediaRange) {
		GenericWebRouteExtractor childExtractor = new GenericWebRouteExtractor(this);
		childExtractor.consume = mediaRange;
		return childExtractor;
	}

	@Override
	public GenericWebRouteExtractor produces(String mediaType) {
		GenericWebRouteExtractor childExtractor = new GenericWebRouteExtractor(this);
		childExtractor.produce = mediaType;
		return childExtractor;
	}

	@Override
	public GenericWebRouteExtractor language(String language) {
		GenericWebRouteExtractor childExtractor = new GenericWebRouteExtractor(this);
		childExtractor.language = language;
		return childExtractor;
	}

	@Override
	public GenericWebRouteExtractor subprotocol(String subprotocol) {
		GenericWebRouteExtractor childExtractor = new GenericWebRouteExtractor(this);
		childExtractor.subprotocol = subprotocol;
		return childExtractor;
	}

	@Override
	public GenericWebRouteExtractor interceptors(List<? extends ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>>> interceptors, Consumer<List<? extends ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>>>> interceptorsUpdater) {
		this.interceptors = interceptors != null ? interceptors : List.of();
		if(this.unwrapInterceptors) {
			this.interceptors = this.interceptors.stream()
			.map(interceptor -> {
				ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> current = interceptor;
				while(current instanceof ExchangeInterceptorWrapper) {
					current = ((ExchangeInterceptorWrapper<ExchangeContext, WebExchange<ExchangeContext>>) current).unwrap();
				}
				return current;
			})
			.collect(Collectors.toList());
		}
		this.interceptorsUpdater = interceptorsUpdater;
		return this;
	}
	
	@Override
	public void handler(ReactiveExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler, boolean disabled) {
		if(handler != null) {
			GenericWebRoute route = new GenericWebRoute(this.getRouter());
			route.setDisabled(disabled);
			
			String routePath = this.getPath();
			URIPattern routePathPattern = this.getPathPattern();
			Method routeMethod = this.getMethod();
			String routeConsume = this.getConsume();
			String routeProduce = this.getProduce();
			String routeLanguage = this.getLanguage();
			
			if(routePath != null) {
				route.setPath(routePath);
			}
			if(routePathPattern != null) {
				route.setPathPattern(routePathPattern);
			}
			if(routeMethod != null) {
				route.setMethod(routeMethod);
			}
			if(routeConsume != null) {
				route.setConsume(routeConsume);
			}
			if(routeProduce != null) {
				route.setProduce(routeProduce);
			}
			if(routeLanguage != null) {
				route.setLanguage(routeLanguage);
			}
			route.setInterceptors(this.interceptors, this.interceptorsUpdater);
			route.setHandler((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)handler);
			this.addRoute(route);
		}
	}

	@Override
	public void webSocketHandler(WebSocketExchangeHandler<ExchangeContext, Web2SocketExchange<ExchangeContext>> handler, boolean disabled) {
		if(handler != null) {
			GenericWebSocketRoute webSocketRoute = new GenericWebSocketRoute(this.getRouter());
			webSocketRoute.setDisabled(disabled);
			
			String routePath = this.getPath();
			URIPattern routePathPattern = this.getPathPattern();
			String routeLanguage = this.getLanguage();
			
			if(routePath != null) {
				webSocketRoute.setPath(routePath);
			}
			if(routePathPattern != null) {
				webSocketRoute.setPathPattern(routePathPattern);
			}
			if(routeLanguage != null) {
				webSocketRoute.setLanguage(routeLanguage);
			}
			webSocketRoute.setInterceptors(this.interceptors, this.interceptorsUpdater);
			webSocketRoute.setWebSocketHandler(handler);
			this.addRoute(webSocketRoute);
		}
	}
}
