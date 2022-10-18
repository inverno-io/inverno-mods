/*
 * Copyright 2022 Jeremy KUHN
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
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.server.ws.WebSocketExchangeHandler;
import io.inverno.mod.web.Web2SocketExchange;
import io.inverno.mod.web.WebRouteManager;
import io.inverno.mod.web.WebRouter;
import io.inverno.mod.web.WebSocketRoute;
import io.inverno.mod.web.WebSocketRouteManager;
import java.util.LinkedHashSet;
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
 * @since 1.5
 */
class GenericWebSocketRouteManager extends AbstractWebManager<GenericWebSocketRouteManager> implements WebSocketRouteManager<ExchangeContext, WebRouter<ExchangeContext>> {

	private final GenericWebRouter router;
	
	private Set<String> subprotocols;
	
	private WebSocketExchangeHandler<ExchangeContext, Web2SocketExchange<ExchangeContext>> handler;
	
	/**
	 * <p>
	 * Creates a generic WebSocket route manager.
	 * </p>
	 * 
	 * @param router a generic web router
	 */
	public GenericWebSocketRouteManager(GenericWebRouter router) {
		this.router = router;
	}
	
	@Override
	public GenericWebSocketRouteManager subprotocol(String subprotocol) {
		Objects.requireNonNull(subprotocol);
		if (this.subprotocols == null) {
			this.subprotocols = new LinkedHashSet<>();
		}
		this.subprotocols.add(subprotocol);
		return this;
	}

	@Override
	public GenericWebSocketRouteManager method(Method method) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public GenericWebSocketRouteManager consumes(String mediaRange) {
		throw new UnsupportedOperationException();
	}

	@Override
	public GenericWebSocketRouteManager produces(String mediaType) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public WebRouter<ExchangeContext> handler(WebSocketExchangeHandler<? super ExchangeContext, Web2SocketExchange<ExchangeContext>> handler) {
		Objects.requireNonNull(handler);
		this.handler = handler;
		this.commit();
		return this.router;
	}

	@Override
	public WebRouter<ExchangeContext> enable() {
		this.findRoutes().stream().forEach(route -> route.enable());
		return this.router;
	}

	@Override
	public WebRouter<ExchangeContext> disable() {
		this.findRoutes().stream().forEach(route -> route.disable());
		return this.router;
	}

	@Override
	public WebRouter<ExchangeContext> remove() {
		this.findRoutes().stream().forEach(route -> route.remove());
		return this.router;
	}

	@Override
	public Set<WebSocketRoute<ExchangeContext>> findRoutes() {
		// TODO Implement filtering in the route extractor
		return this.router.getRoutes().stream().filter(route -> {
				// We want all WebSocket routes that share the same criteria as the one defined in this WebSocket route manager

				if(!(route instanceof WebSocketRoute)) {
					return false;
				}
				WebSocketRoute<ExchangeContext> webSocketRoute = (WebSocketRoute<ExchangeContext>)route;

				if(this.paths != null) {
					if(webSocketRoute.getPath() != null) {
						if(!this.paths.contains(webSocketRoute.getPath())) {
							return false;
						}
					}
					else if(webSocketRoute.getPathPattern() != null) {
						if(this.paths.stream().noneMatch(path -> webSocketRoute.getPathPattern().matcher(path).matches())) {
							return false;
						}
					}
					else {
						return false;
					}
				}
				if(this.pathPatterns != null) {
					if(webSocketRoute.getPath() != null) {
						if(this.pathPatterns.stream().noneMatch(pattern -> pattern.matcher(webSocketRoute.getPath()).matches())) {
							return false;
						}
					}
					else if(webSocketRoute.getPathPattern() != null) {
						if(this.pathPatterns.stream().noneMatch(pattern -> pattern.includes(webSocketRoute.getPathPattern()) != URIPattern.Inclusion.DISJOINT)) {
							return false;
						}
					}
					else {
						return false;
					}
				}
				if(this.languages != null && !this.languages.isEmpty()) {
					if(webSocketRoute.getLanguage() == null || !this.languages.contains(webSocketRoute.getLanguage())) {
						return false;
					}
				}
				if(this.subprotocols != null && !this.subprotocols.isEmpty()) {
					if(webSocketRoute.getSubProtocol()== null || !this.subprotocols.contains(webSocketRoute.getSubProtocol())) {
						return false;
					}
				}
				return true;
			})
			.map(route -> (WebSocketRoute<ExchangeContext>)route)
			.collect(Collectors.toSet());
	}
	
	private void commit() {
		Consumer<GenericWebSocketRoute> subprotocolCommitter = route -> {
			if(this.subprotocols != null && !this.subprotocols.isEmpty()) {
				for(String subprotocol : this.subprotocols) {
					route.setSubProtocol(subprotocol);
					route.setWebSocketHandler(this.handler);
					this.router.setRoute(route);
				}
			}
			else {
				route.setWebSocketHandler(this.handler);
				this.router.setRoute(route);
			}
		};
		
		Consumer<GenericWebSocketRoute> languagesCommitter = route -> {
			if(this.languages != null && !this.languages.isEmpty()) {
				for(String language : this.languages) {
					route.setLanguage(language);
					subprotocolCommitter.accept(route);
				}
			}
			else {
				subprotocolCommitter.accept(route);
			}
		};
		
		Consumer<GenericWebSocketRoute> pathCommitter = route -> {
			if(this.paths != null && !this.paths.isEmpty() || this.pathPatterns != null && !this.pathPatterns.isEmpty()) {
				if(this.paths != null) {
					for(String path : this.paths) {
						route.setPath(path);
						languagesCommitter.accept(route);
					}
				}
				if(this.pathPatterns != null) {
					for(URIPattern pathPattern : this.pathPatterns) {
						route.setPathPattern(pathPattern);
						languagesCommitter.accept(route);
					}
				}
			}
			else {
				languagesCommitter.accept(route);
			}
		};
		pathCommitter.accept(new GenericWebSocketRoute(this.router));
	}
}
