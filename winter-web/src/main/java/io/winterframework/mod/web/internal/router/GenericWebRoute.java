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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import io.winterframework.mod.web.Method;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.RequestHandler;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.router.WebContext;
import io.winterframework.mod.web.router.WebRoute;

/**
 * @author jkuhn
 *
 */
class GenericWebRoute implements WebRoute<RequestBody, ResponseBody, WebContext> {

	private GenericWebRouter router;
	
	protected String path;
	protected PathPattern pathPattern;
	
	protected Set<Method> methods;
	
	protected Set<String> produces;
	
	protected Set<String> consumes;
	
	protected Set<String> languages;

	protected boolean matchTrailingSlash;
	
	protected RequestHandler<RequestBody, ResponseBody, WebContext> handler;
	
	public GenericWebRoute(GenericWebRouter router) {
		this.router = router;
	}

	@Override
	public void enable() {
		this.router.enableRoute(this);
	}
	
	@Override
	public void disable() {
		this.router.disableRoute(this);
	}
	
	@Override
	public void remove() {
		this.router.removeRoute(this);
	}
	
	@Override
	public RequestHandler<RequestBody, ResponseBody, WebContext> getHandler() {
		return this.handler;
	}
	
	public void setHandler(RequestHandler<RequestBody, ResponseBody, WebContext> handler) {
		this.handler = handler;
	}

	@Override
	public String getPath() {
		return this.path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public PathPattern getPathPattern() {
		return this.pathPattern;
	}
	
	public void setPathPattern(PathPattern pathPattern) {
		this.pathPattern = pathPattern;
	}

	@Override
	public boolean isMatchTrailingSlash() {
		return this.matchTrailingSlash;
	}

	public void setMatchTrailingSlash(boolean matchTrailingSlash) {
		this.matchTrailingSlash = matchTrailingSlash;
	}
	
	@Override
	public Set<Method> getMethods() {
		return this.methods != null ? Collections.unmodifiableSet(this.methods) : Set.of();
	}
	
	public void setMethods(Set<Method> methods) {
		this.methods = methods;
	}

	@Override
	public Set<String> getConsumes() {
		return this.consumes != null ? Collections.unmodifiableSet(this.consumes) : Set.of();
	}
	
	public void setConsumes(Set<String> consumes) {
		this.consumes = consumes;
	}

	@Override
	public Set<String> getProduces() {
		return this.produces != null ? Collections.unmodifiableSet(this.produces) : Set.of();
	}
	
	public void setProduces(Set<String> produces) {
		this.produces = produces;
	}
	
	@Override
	public Set<String> getLanguages() {
		return this.languages != null ? Collections.unmodifiableSet(this.languages) : Set.of();
	}
	
	public void setLanguages(Set<String> languages) {
		this.languages = languages;
	}
	
	public static class PathPattern implements WebRoute.PathPattern {

		private Pattern pathPattern;
		
		private List<String> pathParameterNames;
		
		public PathPattern(Pattern pathPattern, List<String> pathParameterNames) {
			Objects.requireNonNull(pathPattern);
			Objects.requireNonNull(pathParameterNames);
			this.pathPattern = pathPattern;
			this.pathParameterNames = pathParameterNames;
		}

		@Override
		public Pattern getPattern() {
			return this.pathPattern;
		}

		@Override
		public List<String> getPathParameterNames() {
			return this.pathParameterNames;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((pathPattern == null) ? 0 : pathPattern.pattern().hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PathPattern other = (PathPattern) obj;
			if (pathPattern == null) {
				if (other.pathPattern != null)
					return false;
			} else if (!pathPattern.pattern().equals(other.pathPattern.pattern()))
				return false;
			return true;
		}
	}
}
