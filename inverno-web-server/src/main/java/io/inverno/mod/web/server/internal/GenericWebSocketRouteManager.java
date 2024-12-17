/*
 * Copyright 2022 Jeremy Kuhn
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

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.router.AbstractRoute;
import io.inverno.mod.http.server.ws.WebSocketExchangeHandler;
import io.inverno.mod.web.server.WebServer;
import io.inverno.mod.web.server.WebSocketRoute;
import io.inverno.mod.web.server.WebSocketRouteManager;
import io.inverno.mod.web.server.internal.router.InternalWebRouter;
import io.inverno.mod.web.server.ws.Web2SocketExchange;
import java.util.ArrayList;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * <p>
 * Generic {@link WebSocketRouteManager} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericWebSocketRouteManager<A extends ExchangeContext, B extends WebServer<A>> implements WebSocketRouteManager<A, B> {

	private final B server;
	private final InternalWebRouter.RouteManager<A> routeManager;

	/**
	 * <p>
	 * Generic WebSocket route manager.
	 * </p>
	 *
	 * @param server       a Web server
	 * @param routeManager an internal route manager
	 */
	public GenericWebSocketRouteManager(B server, InternalWebRouter.RouteManager<A> routeManager) {
		this.server = server;
		this.routeManager = routeManager.method(Method.GET);
	}

	@Override
	public WebSocketRouteManager<A, B> path(String path, boolean matchTrailingSlash) {
		this.routeManager.resolvePath(path, matchTrailingSlash);
		return this;
	}

	@Override
	public WebSocketRouteManager<A, B> language(String language) {
		this.routeManager.language(language);
		return this;
	}

	@Override
	public WebSocketRouteManager<A, B> subprotocol(String subprotocol) {
		this.routeManager.subprotocol(subprotocol);
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public B handler(WebSocketExchangeHandler<? super A, Web2SocketExchange<A>> handler) {
		Consumer<InternalWebRouter.Route<A>> routeConfigurer;
		if(this.server instanceof Intercepting) {
			routeConfigurer = route -> route.get().setInterceptors(new ArrayList<>(((Intercepting<A>)this.server).getWebRouteInterceptorRouter().resolveAll(route)));
		}
		else {
			routeConfigurer = route -> {};
		}
		this.routeManager.set(() -> new WebRouteHandler<>((WebSocketExchangeHandler<A, Web2SocketExchange<A>>)handler), routeConfigurer);
		return this.server;
	}

	@Override
	public B enable() {
		this.routeManager.findRoutes().forEach(AbstractRoute::enable);
		return this.server;
	}

	@Override
	public B disable() {
		this.routeManager.findRoutes().forEach(AbstractRoute::disable);
		return this.server;
	}

	@Override
	public B remove() {
		this.routeManager.findRoutes().forEach(AbstractRoute::remove);
		return this.server;
	}

	@Override
	public Set<WebSocketRoute<A>> findRoutes() {
		return this.routeManager.findRoutes().stream()
			.filter(route -> route.get().getWebSocketHandler() != null)
			.map(GenericWebSocketRoute::new)
			.collect(Collectors.toUnmodifiableSet());
	}
}