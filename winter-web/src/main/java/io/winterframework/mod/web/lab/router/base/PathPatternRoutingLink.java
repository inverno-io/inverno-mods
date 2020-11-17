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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

import io.winterframework.mod.web.Request;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.RequestHandler;
import io.winterframework.mod.web.Response;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.lab.router.BaseContext;
import io.winterframework.mod.web.lab.router.BaseRoute;
import io.winterframework.mod.web.lab.router.PathPattern;

/**
 * @author jkuhn
 *
 */
public class PathPatternRoutingLink extends RoutingLink<PathPatternRoutingLink> {

	private Map<PathPattern, RoutingLink<?>> handlers;
	
	public PathPatternRoutingLink() {
		super(PathPatternRoutingLink::new);
		this.handlers = new HashMap<>();
	}

	@Override
	public PathPatternRoutingLink addRoute(BaseRoute<RequestBody, BaseContext, ResponseBody> route) {
		// TODO Auto-generated method stub
		if(route.getPathPattern() != null) {
			PathPattern pathPattern = route.getPathPattern();
			if(this.handlers.containsKey(pathPattern)) {
				this.handlers.get(pathPattern).addRoute(route);
			}
			else {
				this.handlers.put(pathPattern, this.nextLink.createNextLink().addRoute(route));
			}
		}
		else {
			this.nextLink.addRoute(route);
		}
		return this;
	}

	@Override
	public void handle(Request<RequestBody, BaseContext> request, Response<ResponseBody> response) {
		String normalizedPath;
		try {
			normalizedPath = new URI(request.headers().getPath()).normalize().getPath().toString();
		} 
		catch (URISyntaxException e) {
			throw new RoutingException(500, e);
		}
		
		// TODO If we have more than one, we need to prioritize them or we need to fail depending on a routing strategy
		// - we can choose to trust the Winter compiler to detect conflicts and have a defaulting behavior at runtime
		// - we can choose to make this behavior configurable
		this.handlers.entrySet().stream()
			.map(e -> this.matches(normalizedPath, e.getKey(), e.getValue()))
			.filter(Objects::nonNull)
			.sorted(Comparator.reverseOrder())
			.findFirst()
			.ifPresentOrElse(
				match -> {
					((GenericBaseContext)request.context()).setPathParameters(match.getPathParameters());
					match.getHandler().handle(request, response);
				},
				() -> this.nextLink.handle(request, response)
			);
	}
	
	private PathPatternMatch matches(String path, PathPattern pathPattern, RequestHandler<RequestBody, BaseContext, ResponseBody> handler) {
		Matcher matcher = pathPattern.getPattern().matcher(path);
		if(matcher.matches()) {
			return new PathPatternMatch(matcher, pathPattern, handler);
		}
		return null;
	}
	
	private static class PathPatternMatch implements Comparable<PathPatternMatch> {
		
		private Matcher matcher;
		
		private PathPattern pathPattern;
		
		private RequestHandler<RequestBody, BaseContext, ResponseBody> handler;
		
		public PathPatternMatch(Matcher matcher, PathPattern pathPattern, RequestHandler<RequestBody, BaseContext, ResponseBody> handler) {
			this.matcher = matcher;
			this.pathPattern = pathPattern;
			this.handler = handler;
		}
		
		public Map<String, String> getPathParameters() {
			Map<String, String> pathParameters = new HashMap<>();
			int matcherIndex = 0;
			for(String pathParameterName : this.pathPattern.getPathParameterNames()) {
				if(pathParameterName != null && !pathParameterName.equals(":"))
				pathParameters.put(pathParameterName.substring(1), this.matcher.group(++matcherIndex));
			}
			return pathParameters;
		}
		
		public RequestHandler<RequestBody, BaseContext, ResponseBody> getHandler() {
			return this.handler;
		}

		@Override
		public int compareTo(PathPatternMatch other) {
			// /toto/tata/titi/{}... > /toto/tata/{}...
			// /toto/tata/titi/{}... > /toto/tata/{}
			// /toto/{}/tutu > /toto/{}/{}
			// /toto/tutu > /toto/{:.*}
			// /toto/{}/tata > /toto/{:.*}
			// /toto/{}/{} > /toto/{:.*} 
			
			int groupIndex = 1;
			while (groupIndex < Math.min(this.matcher.groupCount(), other.matcher.groupCount())) {
				String thisGroup = this.matcher.group(groupIndex);
				boolean thisRegex = this.pathPattern.getPathParameterNames().get(groupIndex - 1) != null;
				
				String otherGroup = other.matcher.group(groupIndex);
				boolean otherRegex = other.pathPattern.getPathParameterNames().get(groupIndex - 1) != null;

				if(thisGroup.length() < otherGroup.length()) {
					if(!otherRegex) {
						return -1;
					}
					else {
						return 1;
					}
				}
				else if(thisGroup.length() > otherGroup.length()) {
					if(!thisRegex) {
						return 1;
					}
					else {
						return -1;
					}
				}
				else {
					if(!thisRegex && otherRegex) {
						return 1;
					}
					else if(thisRegex && !otherRegex) {
						return -1;
					}
				}
				groupIndex++;
			}
			
			if(this.matcher.groupCount() < other.matcher.groupCount()) {
				return 1;
			}
			else if(this.matcher.groupCount() > other.matcher.groupCount()) {
				return -1;
			}
			return 0;
		}
	}
}
