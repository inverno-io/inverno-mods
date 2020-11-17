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
package io.winterframework.mod.web.lab.router.base;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import io.winterframework.mod.web.Method;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.RequestHandler;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.lab.router.BaseContext;
import io.winterframework.mod.web.lab.router.BaseRoute;
import io.winterframework.mod.web.lab.router.BaseRouteManager;
import io.winterframework.mod.web.lab.router.BaseRouter;

/**
 * @author jkuhn
 *
 */
public class GenericBaseRouteManager implements BaseRouteManager<RequestBody, BaseContext, ResponseBody> {

	private GenericBaseRouter router;
	
	private GenericBaseRoute route;
	
	GenericBaseRouteManager(GenericBaseRouter router) {
		this.router = router;
		this.route = new GenericBaseRoute(this.router);
	}
	
	@Override
	public GenericBaseRouter enable() {
		this.route.enable();
		return this.router;
	}

	@Override
	public GenericBaseRouter disable() {
		this.route.disable();
		return this.router;
	}

	@Override
	public GenericBaseRouter remove() {
		this.route.remove();
		return this.router;
	}

	@Override
	public List<BaseRoute<RequestBody, BaseContext, ResponseBody>> findRoutes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GenericBaseRouteManager path(String path) throws IllegalArgumentException {
		return this.path(path, false);
	}
	
	@Override
	public GenericBaseRouteManager path(String path, boolean matchTrailingSlash) throws IllegalArgumentException {
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
			
			this.route.pathPattern = new GenericPathPattern(Pattern.compile(routePathPatternBuilder.toString()), pathParameterNames);
		}
		else {
			this.route.pathPattern = null;
		}
		return this;
	}
	
	@Override
	public GenericBaseRouteManager method(Method method) {
		if(this.route.methods == null) {
			this.route.methods = new HashSet<>();
		}
		this.route.methods.add(method);
		return this;
	}
	
	@Override
	public GenericBaseRouteManager consumes(String mediaType) {
		if(this.route.consumes == null) {
			this.route.consumes = new LinkedHashSet<>();
		}
		this.route.consumes.add(mediaType);
		return this;
	}
	
	@Override
	public GenericBaseRouteManager produces(String mediaType) {
		if(this.route.produces == null) {
			this.route.produces = new LinkedHashSet<>();
		}
		this.route.produces.add(mediaType);
		return this;
	}
	
	@Override
	public GenericBaseRouteManager language(String language) {
		if(this.route.languages == null) {
			this.route.languages = new LinkedHashSet<>();
		}
		this.route.languages.add(language);
		return this;
	}

	@Override
	public BaseRouter<RequestBody, BaseContext, ResponseBody> handler(RequestHandler<RequestBody, BaseContext, ResponseBody> handler) {
		Objects.requireNonNull(handler);
		this.route.handler = handler;
		this.router.addRoute(this.route);
		return this.router;
	}
}
