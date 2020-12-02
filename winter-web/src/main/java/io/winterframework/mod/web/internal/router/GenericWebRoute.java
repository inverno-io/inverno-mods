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

import io.winterframework.mod.web.ExchangeHandler;
import io.winterframework.mod.web.Method;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.router.WebExchange;
import io.winterframework.mod.web.router.WebRoute;

/**
 * @author jkuhn
 *
 */
class GenericWebRoute implements WebRoute<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>> {

	private GenericWebRouter router;
	
	protected String path;
	protected WebRoute.PathPattern pathPattern;
	
	protected Set<Method> methods;
	
	protected Set<String> produces;
	
	protected Set<String> consumes;
	
	protected Set<String> languages;

	protected boolean matchTrailingSlash;
	
	protected ExchangeHandler<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>> handler;
	
	public GenericWebRoute(GenericWebRouter router) {
		this.router = router;
	}

	@Override
	public void enable() {
		// TODO
	}
	
	@Override
	public void disable() {
		// TODO
	}
	
	@Override
	public void remove() {
		this.router.removeRoute(this);
	}
	
	public void setHandler(ExchangeHandler<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>> handler) {
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
	public WebRoute.PathPattern getPathPattern() {
		return this.pathPattern;
	}
	
	public void setPathPattern(WebRoute.PathPattern pathPattern) {
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
	
	@Override
	public ExchangeHandler<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>> getHandler() {
		return this.handler;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((consumes == null) ? 0 : consumes.hashCode());
		result = prime * result + ((languages == null) ? 0 : languages.hashCode());
		result = prime * result + ((methods == null) ? 0 : methods.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((pathPattern == null) ? 0 : pathPattern.hashCode());
		result = prime * result + ((produces == null) ? 0 : produces.hashCode());
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
		GenericWebRoute other = (GenericWebRoute) obj;
		if (consumes == null) {
			if (other.consumes != null)
				return false;
		} else if (!consumes.equals(other.consumes))
			return false;
		if (languages == null) {
			if (other.languages != null)
				return false;
		} else if (!languages.equals(other.languages))
			return false;
		if (methods == null) {
			if (other.methods != null)
				return false;
		} else if (!methods.equals(other.methods))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (pathPattern == null) {
			if (other.pathPattern != null)
				return false;
		} else if (!pathPattern.equals(other.pathPattern))
			return false;
		if (produces == null) {
			if (other.produces != null)
				return false;
		} else if (!produces.equals(other.produces))
			return false;
		return true;
	}

	public static class GenericPathPattern implements WebRoute.PathPattern {

		private String path;
		
		private Pattern pathPattern;
		
		private List<String> pathParameterNames;
		
		public GenericPathPattern(String path, Pattern pathPattern, List<String> pathParameterNames) {
			Objects.requireNonNull(pathPattern);
			Objects.requireNonNull(pathParameterNames);
			this.path = path;
			this.pathPattern = pathPattern;
			this.pathParameterNames = pathParameterNames;
		}
		
		@Override
		public String getPath() {
			return this.path;
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
			GenericPathPattern other = (GenericPathPattern) obj;
			if (pathPattern == null) {
				if (other.pathPattern != null)
					return false;
			} else if (!pathPattern.pattern().equals(other.pathPattern.pattern()))
				return false;
			return true;
		}
	}
}
