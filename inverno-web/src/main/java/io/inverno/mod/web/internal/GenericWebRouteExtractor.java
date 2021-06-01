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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.inverno.mod.base.net.URIPattern;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.WebRoute;

/**
 * <p>
 * Generic {@link WebRouteExtractor} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class GenericWebRouteExtractor implements WebRouteExtractor<WebExchange, WebRoute<WebExchange>, GenericWebRouteExtractor> {

	private final GenericWebRouter router;
	
	private GenericWebRouteExtractor parent;
	
	private Set<WebRoute<WebExchange>> routes;
	
	private String path;
	
	private URIPattern pathPattern;
	
	private Method method;
	
	private String consume;
	
	private String produce;
	
	private String language;
	
	/**
	 * <p>
	 * Creates a generic web route extractor in the specified generic web router.
	 * </p>
	 * 
	 * @param router a generic web router
	 */
	public GenericWebRouteExtractor(GenericWebRouter router) {
		this.router = router;
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
	
	private void addRoute(WebRoute<WebExchange> route) {
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
	public Set<WebRoute<WebExchange>> getRoutes() {
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
	public GenericWebRouteExtractor method(Method method) {
		GenericWebRouteExtractor childExtractor = new GenericWebRouteExtractor(this);
		childExtractor.method = method;
		return childExtractor;
	}

	@Override
	public void handler(ExchangeHandler<WebExchange> handler, boolean disabled) {
		if(handler != null) {
			GenericWebRoute route = new GenericWebRoute(this.getRouter());
			route.setDisabled(disabled);
			
			String path = this.getPath();
			URIPattern pathPattern = this.getPathPattern();
			Method method = this.getMethod();
			String consume = this.getConsume();
			String produce = this.getProduce();
			String language = this.getLanguage();
			
			if(path != null) {
				route.setPath(path);
			}
			if(pathPattern != null) {
				route.setPathPattern(pathPattern);
			}
			if(method != null) {
				route.setMethod(method);
			}
			if(consume != null) {
				route.setConsume(consume);
			}
			if(produce != null) {
				route.setProduce(produce);
			}
			if(language != null) {
				route.setLanguage(language);
			}
			route.setHandler(handler);
			this.addRoute(route);
		}
	}
}
