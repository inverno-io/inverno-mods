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
package io.winterframework.mod.web.internal;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.winterframework.mod.base.net.URIBuilder;
import io.winterframework.mod.base.net.URIPattern;
import io.winterframework.mod.base.net.URIs;
import io.winterframework.mod.http.base.Method;
import io.winterframework.mod.http.server.ExchangeHandler;
import io.winterframework.mod.web.WebExchange;
import io.winterframework.mod.web.WebExchangeHandler;
import io.winterframework.mod.web.WebRoute;
import io.winterframework.mod.web.WebRouteManager;
import io.winterframework.mod.web.WebRouter;

/**
 * <p>
 * Generic {@link WebRouteManager} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class GenericWebRouteManager implements WebRouteManager<WebExchange> {

	private final GenericWebRouter router;
	
	private Set<String> paths;
	private Set<URIPattern> pathPatterns;
	
	private Set<Method> methods;
	
	private Set<String> consumes;
	
	private Set<String> produces;
	
	private Set<String> languages;

	private ExchangeHandler<WebExchange> handler;
	
	/**
	 * <p>
	 * Creates a generic web route manager.
	 * </p>
	 * 
	 * @param router the generic web router
	 */
	public GenericWebRouteManager(GenericWebRouter router) {
		this.router = router;
	}
	
	@Override
	public GenericWebRouter enable() {
		this.findRoutes().stream().forEach(route -> route.enable());
		return this.router;
	}

	@Override
	public GenericWebRouter disable() {
		this.findRoutes().stream().forEach(route -> route.disable());
		return this.router;
	}

	@Override
	public GenericWebRouter remove() {
		this.findRoutes().stream().forEach(route -> route.remove());
		return this.router;
	}

	@Override
	public Set<WebRoute<WebExchange>> findRoutes() {
		// TODO Implement filtering in the route extractor
		return this.router.getRoutes().stream().filter(route -> {
			// We want all routes that share the same criteria as the one defined in this route manager
			if(this.paths != null && !this.paths.contains(route.getPath())) {
				return false;
			}
			if(this.methods != null && !this.methods.isEmpty()) {
				if(route.getMethod() == null || !this.methods.contains(route.getMethod())) {
					return false;
				}
			}
			if(this.consumes != null && !this.consumes.isEmpty()) {
				if(route.getConsume() == null || !this.consumes.contains(route.getConsume())) {
					return false;
				}
			}
			if(this.produces != null && !this.produces.isEmpty()) {
				if(route.getProduce() == null || !this.produces.contains(route.getProduce())) {
					return false;
				}
			}
			if(this.languages != null && !this.languages.isEmpty()) {
				if(route.getLanguage() == null || !this.languages.contains(route.getLanguage())) {
					return false;
				}
			}
			return true;
		}).collect(Collectors.toSet());
	}

	@Override
	public WebRouteManager<WebExchange> path(String path, boolean matchTrailingSlash) throws IllegalArgumentException {
		Objects.requireNonNull(path);
		if(!path.startsWith("/")) {
			throw new IllegalArgumentException("Path must be absolute");
		}
		
		URIBuilder pathBuilder = URIs.uri(path, false, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED);
		
		String rawPath = pathBuilder.buildRawPath();
		List<String> pathParameterNames = pathBuilder.getParameterNames();
		if(pathParameterNames.isEmpty()) {
			// Static path
			if(this.paths == null) {
				this.paths = new HashSet<>();
			}
			this.paths.add(rawPath);
			if(matchTrailingSlash) {
				if(rawPath.endsWith("/")) {
					this.paths.add(rawPath.substring(0, rawPath.length() - 1));
				}
				else {
					this.paths.add(rawPath + "/");
				}
			}
		}
		else {
			// PathPattern
			if(this.pathPatterns == null) {
				this.pathPatterns = new HashSet<>();
			}
			this.pathPatterns.add(pathBuilder.buildPathPattern(matchTrailingSlash));
		}
		return this;
	}

	@Override
	public GenericWebRouteManager method(Method method) {
		Objects.requireNonNull(method);
		if(this.methods == null) {
			this.methods = new HashSet<>();
		}
		this.methods.add(method);
		return this;
	}
	
	@Override
	public GenericWebRouteManager consumes(String mediaRange) {
		Objects.requireNonNull(mediaRange);
		if(this.consumes == null) {
			this.consumes = new LinkedHashSet<>();
		}
		this.consumes.add(mediaRange);
		return this;
	}
	
	@Override
	public GenericWebRouteManager produces(String mediaType) {
		Objects.requireNonNull(mediaType);
		if(this.produces == null) {
			this.produces = new LinkedHashSet<>();
		}
		this.produces.add(mediaType);
		return this;
	}
	
	@Override
	public GenericWebRouteManager language(String language) {
		Objects.requireNonNull(language);
		if(this.languages == null) {
			this.languages = new LinkedHashSet<>();
		}
		this.languages.add(language);
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public WebRouter<WebExchange> handler(ExchangeHandler<? super WebExchange> handler) {
		Objects.requireNonNull(handler);
		// This will work since we consider lower bounded types
		this.handler = (ExchangeHandler<WebExchange>) handler;
		this.commit();
		return this.router;
	}

	@SuppressWarnings("unchecked")
	@Override
	public WebRouter<WebExchange> handler(WebExchangeHandler<? super WebExchange> handler) {
		Objects.requireNonNull(handler);
		// This will work since we consider lower bounded types
		this.handler = (ExchangeHandler<WebExchange>) handler;
		this.commit();
		return this.router;
	}
	
	private void commit() {
		Consumer<GenericWebRoute> languagesCommitter = route -> {
			if(this.languages != null && !this.languages.isEmpty()) {
				for(String language : this.languages) {
					route.setLanguage(language);
					route.setHandler(this.handler);
					this.router.setRoute(route);
				}
			}
			else {
				route.setHandler(this.handler);
				this.router.setRoute(route);
			}
		};
		
		Consumer<GenericWebRoute> producesCommitter = route -> {
			if(this.produces != null && !this.produces.isEmpty()) {
				for(String produce : this.produces) {
					route.setProduce(produce);
					languagesCommitter.accept(route);
				}
			}
			else {
				languagesCommitter.accept(route);
			}
		};
		
		Consumer<GenericWebRoute> consumesCommitter = route -> {
			if(this.consumes != null && !this.consumes.isEmpty()) {
				for(String consume : this.consumes) {
					route.setConsume(consume);
					producesCommitter.accept(route);
				}
			}
			else {
				producesCommitter.accept(route);
			}
		};
		
		Consumer<GenericWebRoute> methodsCommitter = route -> {
			if(this.methods != null && !this.methods.isEmpty()) {
				for(Method method : this.methods) {
					route.setMethod(method);
					consumesCommitter.accept(route);
				}
			}
			else {
				consumesCommitter.accept(route);
			}
		};
		
		Consumer<GenericWebRoute> pathCommitter = route -> {
			if(this.paths != null && !this.paths.isEmpty()) {
				for(String path : this.paths) {
					route.setPath(path);
					methodsCommitter.accept(route);
				}
			}
			else if(this.pathPatterns != null && !this.pathPatterns.isEmpty()) {
				for(URIPattern pathPattern : this.pathPatterns) {
					route.setPathPattern(pathPattern);
					methodsCommitter.accept(route);
				}
			}
			else {
				methodsCommitter.accept(route);
			}
		};
		pathCommitter.accept(new GenericWebRoute(this.router));
	}
}
