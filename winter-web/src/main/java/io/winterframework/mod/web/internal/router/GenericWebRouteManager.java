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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.winterframework.mod.web.ExchangeHandler;
import io.winterframework.mod.web.Method;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.router.WebExchange;
import io.winterframework.mod.web.router.WebExchangeHandler;
import io.winterframework.mod.web.router.WebRoute;
import io.winterframework.mod.web.router.WebRouteManager;
import io.winterframework.mod.web.router.WebRouter;

/**
 * @author jkuhn
 *
 */
class GenericWebRouteManager implements WebRouteManager<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>> {

	private GenericWebRouter router;
	
	private GenericWebRoute route;
	
	public GenericWebRouteManager(GenericWebRouter router) {
		this.router = router;
		this.route = new GenericWebRoute(this.router);
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
	public Set<WebRoute<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>>> findRoutes() {
		// TODO Implement filtering in the route extractor
		return this.router.getRoutes().stream().filter(route -> {
			// We want all routes that share the same criteria as the one defined in this route manager
			if(this.route.path != null && !this.route.path.equals(route.getPath())) {
				return false;
			}
			if(this.route.methods != null && !this.route.methods.isEmpty()) {
				if(route.getMethods() == null || route.getMethods().isEmpty() || !this.route.methods.containsAll(route.getMethods())) {
					return false;
				}
			}
			if(this.route.consumes != null && !this.route.consumes.isEmpty()) {
				if(route.getConsumes() == null || route.getConsumes().isEmpty() || !this.route.consumes.containsAll(route.getConsumes())) {
					return false;
				}
			}
			if(this.route.produces != null && !this.route.produces.isEmpty()) {
				if(route.getProduces() == null || route.getProduces().isEmpty() || !this.route.produces.containsAll(route.getProduces())) {
					return false;
				}
			}
			if(this.route.languages != null && !this.route.languages.isEmpty()) {
				if(route.getLanguages() == null || route.getLanguages().isEmpty() || !this.route.languages.containsAll(route.getLanguages())) {
					return false;
				}
			}
			return true;
		}).collect(Collectors.toSet());
	}

	@Override
	public GenericWebRouteManager path(String path) throws IllegalArgumentException {
		return this.path(path, false);
	}
	
	@Override
	public GenericWebRouteManager path(String path, boolean matchTrailingSlash) throws IllegalArgumentException {
		Objects.requireNonNull(path);
		if(!path.startsWith("/")) {
			throw new IllegalArgumentException("Path must be absolute");
		}
		this.route.matchTrailingSlash = matchTrailingSlash;
		this.route.path = path;
		
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
					// TODO we could validate here that we have a valid valid URI segment
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
			if(this.route.matchTrailingSlash) {
				routePathPatternBuilder.append("/?");
			}
			
			routePathPatternBuilder.insert(0, "^");
			routePathPatternBuilder.append("$");
			
			this.route.pathPattern = new GenericWebRoute.GenericPathPattern(path, Pattern.compile(routePathPatternBuilder.toString()), pathParameterNames);
		}
		else {
			this.route.pathPattern = null;
		}
		return this;
	}
	
	@Override
	public GenericWebRouteManager method(Method method) {
		Objects.requireNonNull(method);
		if(this.route.methods == null) {
			this.route.methods = new HashSet<>();
		}
		this.route.methods.add(method);
		return this;
	}
	
	@Override
	public GenericWebRouteManager consumes(String mediaType) {
		Objects.requireNonNull(mediaType);
		if(this.route.consumes == null) {
			this.route.consumes = new LinkedHashSet<>();
		}
		this.route.consumes.add(mediaType);
		return this;
	}
	
	@Override
	public GenericWebRouteManager produces(String mediaType) {
		Objects.requireNonNull(mediaType);
		if(this.route.produces == null) {
			this.route.produces = new LinkedHashSet<>();
		}
		this.route.produces.add(mediaType);
		return this;
	}
	
	@Override
	public GenericWebRouteManager language(String language) {
		Objects.requireNonNull(language);
		if(this.route.languages == null) {
			this.route.languages = new LinkedHashSet<>();
		}
		this.route.languages.add(language);
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public WebRouter<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>> handler(ExchangeHandler<? super RequestBody, ? super ResponseBody, ? super WebExchange<RequestBody, ResponseBody>> handler) {
		Objects.requireNonNull(handler);
		// This will work since we consider lower bounded types
		this.route.handler = (ExchangeHandler<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>>) handler;
		this.router.addRoute(this.route);
		return this.router;
	}

	@SuppressWarnings("unchecked")
	@Override
	public WebRouter<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>> handler(WebExchangeHandler<? super RequestBody, ? super ResponseBody> handler) {
		Objects.requireNonNull(handler);
		// This will work since we consider lower bounded types
		this.route.handler = (WebExchangeHandler<RequestBody, ResponseBody>) handler;
		this.router.addRoute(this.route);
		return this.router;
	}
}
