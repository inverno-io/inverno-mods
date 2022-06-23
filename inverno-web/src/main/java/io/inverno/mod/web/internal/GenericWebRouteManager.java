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
package io.inverno.mod.web.internal;

import io.inverno.mod.base.net.URIPattern;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.WebRoute;
import io.inverno.mod.web.WebRouteManager;
import io.inverno.mod.web.WebRouter;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * <p>
 * Generic {@link WebRouteManager} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class GenericWebRouteManager extends AbstractWebManager<GenericWebRouteManager> implements WebRouteManager<ExchangeContext, WebRouter<ExchangeContext>> {

	private final GenericWebRouter router;

	private ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler;
	
	/**
	 * <p>
	 * Creates a generic web route manager.
	 * </p>
	 * 
	 * @param router a generic web router
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
	public Set<WebRoute<ExchangeContext>> findRoutes() {
		// TODO Implement filtering in the route extractor
		return this.router.getRoutes().stream().filter(route -> {
			// We want all routes that share the same criteria as the one defined in this route manager
			if(this.paths != null) {
				if(route.getPath() != null) {
					if(!this.paths.contains(route.getPath())) {
						return false;
					}
				}
				else if(route.getPathPattern() != null) {
					if(this.paths.stream().noneMatch(path -> route.getPathPattern().matcher(path).matches())) {
						return false;
					}
				}
				else {
					return false;
				}
			}
			if(this.pathPatterns != null) {
				if(route.getPath() != null) {
					if(this.pathPatterns.stream().noneMatch(pattern -> pattern.matcher(route.getPath()).matches())) {
						return false;
					}
				}
				else if(route.getPathPattern() != null) {
					if(this.pathPatterns.stream().noneMatch(pattern -> pattern.includes(route.getPathPattern()) != URIPattern.Inclusion.DISJOINT)) {
						return false;
					}
				}
				else {
					return false;
				}
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
	public WebRouter<ExchangeContext> handler(ExchangeHandler<? super ExchangeContext, WebExchange<ExchangeContext>> handler) {
		Objects.requireNonNull(handler);
		this.handler = handler;
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
			if(this.paths != null && !this.paths.isEmpty() || this.pathPatterns != null && !this.pathPatterns.isEmpty()) {
				if(this.paths != null) {
					for(String path : this.paths) {
						route.setPath(path);
						methodsCommitter.accept(route);
					}
				}
				if(this.pathPatterns != null) {
					for(URIPattern pathPattern : this.pathPatterns) {
						route.setPathPattern(pathPattern);
						methodsCommitter.accept(route);
					}
				}
			}
			else {
				methodsCommitter.accept(route);
			}
		};
		pathCommitter.accept(new GenericWebRoute(this.router));
	}
}
