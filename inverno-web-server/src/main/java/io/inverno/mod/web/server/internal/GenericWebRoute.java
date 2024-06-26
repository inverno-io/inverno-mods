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
package io.inverno.mod.web.server.internal;

import io.inverno.mod.base.net.URIPattern;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.http.server.ReactiveExchangeHandler;
import io.inverno.mod.web.server.WebExchange;
import io.inverno.mod.web.server.WebRoute;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * <p>
 * Generic {@link WebRoute} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class GenericWebRoute implements WebRoute<ExchangeContext> {
	
	private final AbstractWebRouter router;
	
	private boolean disabled;
	
	private String path;
	private URIPattern pathPattern;
	
	private Method method;
	
	private String consume;
	
	private String produce;
	
	private String language;
	
	private List<? extends ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>>> interceptors;
	private Consumer<List<? extends ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>>>> interceptorsUpdater;
	
	private ReactiveExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler;
	
	/**
	 * <p>
	 * Creates a generic web route in the specified generic web router.
	 * </p>
	 * 
	 * @param router a generic web router
	 */
	public GenericWebRoute(AbstractWebRouter router) {
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
	
	/**
	 * <p>
	 * Disables/Enables the web route.
	 * </p>
	 * 
	 * @param disabled true to disable the route, false otherwise
	 */
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
	
	/**
	 * <p>
	 * Sets the route static path as defined by {@link WebRoute#getPath()}.
	 * </p>
	 * 
	 * @param path a static path
	 */
	public void setPath(String path) {
		this.pathPattern = null;
		this.path = path;
	}

	@Override
	public URIPattern getPathPattern() {
		return this.pathPattern;
	}
	
	/**
	 * <p>
	 * Sets the route parameterized path as defined by {@link WebRoute#getPathPattern()}.
	 * </p>
	 * 
	 * @param pathPattern a path pattern
	 */
	public void setPathPattern(URIPattern pathPattern) {
		this.path = null;
		this.pathPattern = pathPattern;
	}

	@Override
	public Method getMethod() {
		return this.method;
	}
	
	/**
	 * <p>
	 * Sets the route HTTP method as defined by {@link WebRoute#getMethod()}.
	 * </p>
	 * 
	 * @param method a HTTP method
	 */
	public void setMethod(Method method) {
		this.method = method;
	}

	@Override
	public String getConsume() {
		return this.consume;
	}
	
	/**
	 * <p>
	 * Sets the route consumed media range as defined by {@link WebRoute#getConsume()}.
	 * </p>
	 * 
	 * @param mediaRange a media range
	 */
	public void setConsume(String mediaRange) {
		this.consume = mediaRange;
	}

	@Override
	public String getProduce() {
		return this.produce;
	}
	
	/**
	 * <p>
	 * Sets the route produced media type as defined by {@link WebRoute#getProduce()}.
	 * </p>
	 * 
	 * @param mediaType a media type
	 */
	public void setProduce(String mediaType) {
		this.produce = mediaType;
	}
	
	@Override
	public String getLanguage() {
		return this.language;
	}
	
	/**
	 * <p>
	 * Sets the route language as defined by {@link WebRoute#getLanguage()}
	 * </p>
	 * 
	 * @param language a language tag
	 */
	public void setLanguage(String language) {
		this.language = language;
	}
	
	@Override
	public List<? extends ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>>> getInterceptors() {
		return this.interceptors;
	}
	
	@Override
	public void setInterceptors(List<? extends ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>>> interceptors) {
		this.interceptors = interceptors != null ? Collections.unmodifiableList(interceptors) : List.of();
		if(this.interceptorsUpdater != null) {
			this.interceptorsUpdater.accept(this.interceptors);
		}
	}

	/**
	 * <p>
	 * Used by a {@link WebRouteExtractor} to populate route interceptors.
	 * </p>
	 *
	 * @param interceptors        the route interceptors
	 * @param interceptorsUpdater a consumer used to update route interceptors in the actual handler routing link.
	 */
	public void setInterceptors(List<? extends ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>>> interceptors, Consumer<List<? extends ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>>>> interceptorsUpdater) {
		this.interceptors = interceptors != null ? Collections.unmodifiableList(interceptors) : List.of();
		this.interceptorsUpdater = interceptorsUpdater;
	}
	
	@Override
	public ReactiveExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> getHandler() {
		return this.handler;
	}
	
	/**
	 * <p>
	 * Sets the route exchange handler as defined by {@link WebRoute#getHandler()}.
	 * </p>
	 * 
	 * @param handler an exchange handler
	 */
	public void setHandler(ReactiveExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler) {
		this.handler = handler;
	}

	@Override
	public String toString() {
		StringBuilder routeStringBuilder = new StringBuilder();
		
		routeStringBuilder.append("{");
		routeStringBuilder.append("\"method\":\"").append(this.method != null ? this.method : null).append("\",");
		routeStringBuilder.append("\"path\":\"").append(this.path != null ? this.path : this.pathPattern).append("\",");
		routeStringBuilder.append("\"consume\":\"").append(this.consume).append("\",");
		routeStringBuilder.append("\"produce\":\"").append(this.produce).append("\",");
		routeStringBuilder.append("\"language\":\"").append(this.language);
		routeStringBuilder.append("}");
		
		return routeStringBuilder.toString();
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
}
