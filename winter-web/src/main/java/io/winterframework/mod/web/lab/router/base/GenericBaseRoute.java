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

import java.util.Collections;
import java.util.Set;

import io.winterframework.mod.web.Method;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.RequestHandler;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.lab.router.BaseContext;
import io.winterframework.mod.web.lab.router.BaseRoute;
import io.winterframework.mod.web.lab.router.PathPattern;

/**
 * @author jkuhn
 *
 */
public class GenericBaseRoute implements BaseRoute<RequestBody, BaseContext, ResponseBody> {

	private RoutingChain routingChain;
	
	protected String path;
	protected PathPattern pathPattern;
	
	protected Set<Method> methods;
	
	protected Set<String> produces;
	
	protected Set<String> consumes;
	
	protected Set<String> languages;

	protected boolean matchTrailingSlash;
	
	protected RequestHandler<RequestBody, BaseContext, ResponseBody> handler;
	
	/**
	 * 
	 */
	public GenericBaseRoute(RoutingChain routingChain) {
		this.routingChain = routingChain;
	}

	@Override
	public void enable() {
		this.routingChain.enableRoute(this);
	}
	
	@Override
	public void disable() {
		this.routingChain.disableRoute(this);
	}
	
	@Override
	public void remove() {
		this.routingChain.removeRoute(this);
	}
	
	@Override
	public RequestHandler<RequestBody, BaseContext, ResponseBody> getHandler() {
		return this.handler;
	}
	
	public void setHandler(RequestHandler<RequestBody, BaseContext, ResponseBody> handler) {
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
}
