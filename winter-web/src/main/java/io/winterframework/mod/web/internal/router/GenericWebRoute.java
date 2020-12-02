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

import java.util.List;
import java.util.Objects;
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
	
	private boolean disabled;
	
	private String path;
	private WebRoute.PathPattern pathPattern;
	
	private Method method;
	
	private String produce;
	
	private String consume;
	
	private String language;
	
	private ExchangeHandler<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>> handler;
	
	public GenericWebRoute(GenericWebRouter router) {
		this.router = router;
	}

	@Override
	public void enable() {
		this.router.enableRoute(this);
		this.disabled = false;
	}
	
	@Override
	public void disable() {
		this.router.disableRoute(this);
		this.disabled = true;
	}
	
	@Override
	public boolean isDisabled() {
		return this.disabled;
	}
	
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
	
	@Override
	public void remove() {
		this.router.removeRoute(this);
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
	public Method getMethod() {
		return this.method;
	}
	
	public void setMethod(Method method) {
		this.method = method;
	}

	@Override
	public String getConsume() {
		return this.consume;
	}
	
	public void setConsume(String consume) {
		this.consume = consume;
	}

	@Override
	public String getProduce() {
		return this.produce;
	}
	
	public void setProduce(String produce) {
		this.produce = produce;
	}
	
	@Override
	public String getLanguage() {
		return this.language;
	}
	
	public void setLanguage(String language) {
		this.language = language;
	}
	
	@Override
	public ExchangeHandler<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>> getHandler() {
		return this.handler;
	}
	
	public void setHandler(ExchangeHandler<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>> handler) {
		this.handler = handler;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((consume == null) ? 0 : consume.hashCode());
		result = prime * result + ((language == null) ? 0 : language.hashCode());
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((pathPattern == null) ? 0 : pathPattern.hashCode());
		result = prime * result + ((produce == null) ? 0 : produce.hashCode());
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
		if (consume == null) {
			if (other.consume != null)
				return false;
		} else if (!consume.equals(other.consume))
			return false;
		if (language == null) {
			if (other.language != null)
				return false;
		} else if (!language.equals(other.language))
			return false;
		if (method == null) {
			if (other.method != null)
				return false;
		} else if (!method.equals(other.method))
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
		if (produce == null) {
			if (other.produce != null)
				return false;
		} else if (!produce.equals(other.produce))
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
