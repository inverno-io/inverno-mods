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
package io.winterframework.mod.web.router.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.winterframework.mod.web.Method;
import io.winterframework.mod.web.router.WebExchange;
import io.winterframework.mod.web.router.WebExchangeHandler;
import io.winterframework.mod.web.router.WebRoute;
import io.winterframework.mod.web.router.WebRouteManager;
import io.winterframework.mod.web.router.WebRouter;
import io.winterframework.mod.web.server.ExchangeHandler;

/**
 * @author jkuhn
 *
 */
class GenericWebRouteManager implements WebRouteManager<WebExchange> {

	private GenericWebRouter router;
	
//	private GenericWebRoute route;
	
	private Set<String> paths;
	private WebRoute.PathPattern pathPattern;
	
	private Set<Method> methods;
	
	private Set<String> consumes;
	
	private Set<String> produces;
	
	private Set<String> languages;

	private ExchangeHandler<WebExchange> handler;
	
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
	public GenericWebRouteManager path(String path, boolean matchTrailingSlash) throws IllegalArgumentException {
		Objects.requireNonNull(path);
		if(!path.startsWith("/")) {
			throw new IllegalArgumentException("Path must be absolute");
		}
		
		List<String> pathParameterNames = new ArrayList<>();
		
		StringBuilder routePathPatternBuilder = new StringBuilder();
		Set<String> uniquePathParameterNames = new HashSet<>();
		
		byte[] pathBytes = path.getBytes();
		String pathParamName = null;
		boolean inPathParam = false;
		Integer startIndex = null;
		for(int i=0;i<pathBytes.length;i++) {
			byte nextByte = pathBytes[i];
			if(nextByte == '{' && !inPathParam) {
				if(startIndex != null) {
					pathParameterNames.add(null);
					routePathPatternBuilder.append('(');
					// TODO we could validate here that we have a valid URI segment
					routePathPatternBuilder.append(Pattern.quote(new String(pathBytes, startIndex, i - startIndex)));
					routePathPatternBuilder.append(')');
				}
				startIndex = i + 1;
				inPathParam = true;
			}
			else if(nextByte == '}' && inPathParam && pathBytes[i-1] != '\\') {
				// closing path param
				routePathPatternBuilder.append('(');
				if(pathParamName == null) {
					pathParamName = ':' + new String(pathBytes, startIndex, i - startIndex);
					if(!uniquePathParameterNames.add(pathParamName)) {
						throw new IllegalArgumentException("Duplicate path parameters: " + pathParamName);
					}
					pathParameterNames.add(pathParamName);
					routePathPatternBuilder.append(".+");
				}
				else {
					if(!uniquePathParameterNames.add(pathParamName)) {
						throw new IllegalArgumentException("Duplicate path parameters: " + pathParamName);
					}
					pathParameterNames.add(pathParamName);
					routePathPatternBuilder.append(new String(pathBytes, startIndex, i - startIndex));
				}
				routePathPatternBuilder.append(')');
				pathParamName = null;
				inPathParam = false;
				startIndex = null;
			}
			else if(nextByte == ':' && inPathParam && pathParamName == null) {
				pathParamName = ':' + new String(pathBytes, startIndex, i - startIndex);
				startIndex = i + 1;
			}
			else if(startIndex == null) {
				startIndex = i;
			}
		}
		if(startIndex != null) {
			if(inPathParam) {
				// error unfinished pattern
				throw new IllegalArgumentException("Invalid path: " + path);
			}
			else {
				if(pathParameterNames.size() > 0) {
					pathParameterNames.add(null);
				}
				routePathPatternBuilder.append('(');
				// TODO we could validate here that we have a valid URI segment
				routePathPatternBuilder.append(Pattern.quote(new String(pathBytes, startIndex, pathBytes.length - startIndex)));
				routePathPatternBuilder.append(')');
			}
		}
		
		if(pathParameterNames.size() > 0) {
			// We have a regex path
			if(matchTrailingSlash) {
				routePathPatternBuilder.append("/?");
			}
			
			routePathPatternBuilder.insert(0, "^");
			routePathPatternBuilder.append("$");
			
			this.paths = Set.of(path);
			this.pathPattern = new GenericWebRoute.GenericPathPattern(path, Pattern.compile(routePathPatternBuilder.toString()), pathParameterNames);
		}
		else {
			this.paths = new HashSet<>();
			this.paths.add(path);
			if(matchTrailingSlash) {
				if(path.endsWith("/")) {
					this.paths.add(path.substring(0, path.length() - 1));
				}
				else {
					this.paths.add(path + "/");
				}
			}
			else {
				this.paths = Set.of(path);
			}
			this.pathPattern = null;
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
	public GenericWebRouteManager consumes(String mediaType) {
		Objects.requireNonNull(mediaType);
		if(this.consumes == null) {
			this.consumes = new LinkedHashSet<>();
		}
		this.consumes.add(mediaType);
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
					route.setPathPattern(this.pathPattern);
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
