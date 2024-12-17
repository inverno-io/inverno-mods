/*
 * Copyright 2020 Jeremy Kuhn
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
import io.inverno.mod.http.base.router.AbstractRoute;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.http.server.ReactiveExchangeHandler;
import io.inverno.mod.web.server.ErrorWebExchange;
import io.inverno.mod.web.server.ErrorWebRoute;
import io.inverno.mod.web.server.ErrorWebRouteManager;
import io.inverno.mod.web.server.WebServer;
import io.inverno.mod.web.server.internal.router.InternalErrorWebRouter;
import java.util.ArrayList;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * <p>
 * Generic {@link ErrorWebRouteManager} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @param <A> the exchange context type
 * @param <B> the Web server type
 */
public class GenericErrorWebRouteManager<A extends ExchangeContext, B extends WebServer<A>> implements ErrorWebRouteManager<A, B> {

	private final B server;
	private final InternalErrorWebRouter.RouteManager<A> routeManager;

	/**
	 * <p>
	 * Creates a generic error Web route manager.
	 * </p>
	 *
	 * @param server       a Web server
	 * @param routeManager an internal error route manager
	 */
	public GenericErrorWebRouteManager(B server, InternalErrorWebRouter.RouteManager<A> routeManager) {
		this.server = server;
		this.routeManager = routeManager;
	}

	@Override
	public ErrorWebRouteManager<A, B> error(Class<? extends Throwable> error) {
		this.routeManager.errorType(error);
		return this;
	}

	@Override
	public ErrorWebRouteManager<A, B> path(String path, boolean matchTrailingSlash) {
		this.routeManager.resolvePath(path, matchTrailingSlash);
		return this;
	}

	@Override
	public ErrorWebRouteManager<A, B> consume(String mediaRange) {
		this.routeManager.contentType(mediaRange);
		return this;
	}

	@Override
	public ErrorWebRouteManager<A, B> produce(String mediaType) {
		this.routeManager.accept(mediaType);
		return this;
	}

	@Override
	public ErrorWebRouteManager<A, B> language(String language) {
		this.routeManager.language(language);
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public B handler(ExchangeHandler<? super A, ErrorWebExchange<A>> handler) {
		Consumer<InternalErrorWebRouter.Route<A>> routeConfigurer;
		if(this.server instanceof Intercepting) {
			routeConfigurer = route -> route.get().setInterceptors(new ArrayList<>(((Intercepting<A>)this.server).getErrorWebRouteInterceptorRouter().resolveAll(route)));
		}
		else {
			routeConfigurer = route -> {};
		}
		this.routeManager.set(() -> new ErrorWebRouteHandler<>((ReactiveExchangeHandler<A, ErrorWebExchange<A>>)handler), routeConfigurer);
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
	public Set<ErrorWebRoute<A>> findRoutes() {
		return this.routeManager.findRoutes().stream()
			.map(GenericErrorWebRoute::new)
			.collect(Collectors.toUnmodifiableSet());
	}
}