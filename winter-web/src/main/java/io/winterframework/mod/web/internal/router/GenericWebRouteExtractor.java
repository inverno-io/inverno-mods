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
package io.winterframework.mod.web.internal.router;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.winterframework.mod.web.ExchangeHandler;
import io.winterframework.mod.web.Method;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.router.PathAwareRoute.PathPattern;
import io.winterframework.mod.web.router.WebExchange;
import io.winterframework.mod.web.router.WebRoute;

/**
 * @author jkuhn
 *
 */
public class GenericWebRouteExtractor implements WebRouteExtractor<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>, WebRoute<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>>, GenericWebRouteExtractor> {

	private GenericWebRouter router;
	
	private GenericWebRouteExtractor parent;
	
	private Set<WebRoute<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>>> routes;
	
	private String path;
	
	private PathPattern pathPattern;
	
	private Method method;
	
	private String consumes;
	
	private String produces;
	
	private String language;
	
	public GenericWebRouteExtractor(GenericWebRouter router) {
		this.router = router;
	}
	
	private GenericWebRouteExtractor(GenericWebRouteExtractor parent) {
		this.parent = parent;
	}
	
	private GenericWebRouter getRouter() {
		if(this.parent != null) {
			return this.parent.getRouter();
		}
		else {
			return this.router;
		}
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
	
	private PathPattern getPathPattern() {
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
	
	private String getConsumes() {
		if(this.consumes != null) {
			return this.consumes;
		}
		else if(parent != null) {
			return this.parent.getConsumes();
		}
		return null;
	}
	
	private String getProduces() {
		if(this.produces != null) {
			return this.produces;
		}
		else if(parent != null) {
			return this.parent.getProduces();
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
	
	private void addRoute(WebRoute<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>> route) {
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
	public Set<WebRoute<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>>> getRoutes() {
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
	public GenericWebRouteExtractor pathPattern(PathPattern pathPattern) {
		GenericWebRouteExtractor childExtractor = new GenericWebRouteExtractor(this);
		childExtractor.pathPattern = pathPattern;
		return childExtractor;
	}

	@Override
	public GenericWebRouteExtractor consumes(String mediaType) {
		GenericWebRouteExtractor childExtractor = new GenericWebRouteExtractor(this);
		childExtractor.consumes = mediaType;
		return childExtractor;
	}

	@Override
	public GenericWebRouteExtractor produces(String mediaType) {
		GenericWebRouteExtractor childExtractor = new GenericWebRouteExtractor(this);
		childExtractor.produces = mediaType;
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
	public void handler(ExchangeHandler<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>> handler) {
		if(handler != null) {
			GenericWebRoute route = new GenericWebRoute(this.getRouter());
			
			String path = this.getPath();
			PathPattern pathPattern = this.getPathPattern();
			Method method = this.getMethod();
			String consumes = this.getConsumes();
			String produces = this.getProduces();
			String language = this.getLanguage();
			
			if(path != null) {
				route.setPath(path);
			}
			if(pathPattern != null) {
				route.setPath(pathPattern.getPath());
				route.setPathPattern(pathPattern);
			}
			if(method != null) {
				route.setMethods(Set.of(method));
			}
			if(consumes != null) {
				route.setConsumes(Set.of(consumes));
			}
			if(produces != null) {
				route.setProduces(Set.of(produces));
			}
			if(language != null) {
				route.setLanguages(Set.of(language));
			}
			route.setHandler(handler);
			this.addRoute(route);
		}
	}
}
