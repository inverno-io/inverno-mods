/*
 * Copyright 2024 Jeremy Kuhn
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
import io.inverno.mod.web.server.ErrorWebRouteInterceptor;
import io.inverno.mod.web.server.ErrorWebRouteInterceptorManager;
import io.inverno.mod.web.server.ErrorWebRouter;
import io.inverno.mod.web.server.ErrorWebRoute;
import io.inverno.mod.web.server.ErrorWebRouteManager;
import io.inverno.mod.web.server.WebRouteInterceptor;
import io.inverno.mod.web.server.WebRouteInterceptorManager;
import io.inverno.mod.web.server.WebRouter;
import io.inverno.mod.web.server.WebRoute;
import io.inverno.mod.web.server.WebRouteManager;
import io.inverno.mod.web.server.WebServer;
import io.inverno.mod.web.server.WebSocketRoute;
import io.inverno.mod.web.server.WebSocketRouteManager;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * Scopes intercepted Web server instances when configuring Web servers.
 * </p>
 *
 * <p>
 * {@link WebServer.Intercepted} instances can't be exposed directly when configuring an intercepted Web server using {@link WebServer#configure(Configurer)} for instance as one could then do a cast
 * leading to undesirable side effects.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
class WebServerFacade<A extends ExchangeContext> implements WebServer<A> {

	private final WebServer.Intercepted<A> server;

	/**
	 * <p>
	 * Creates a Web router facade wrapping the specified Web server.
	 * </p>
	 *
	 * @param interceptedServer an intercepted Web server
	 */
	public WebServerFacade(Intercepted<A> interceptedServer) {
		this.server = interceptedServer;
	}

	@Override
	public Set<WebRoute<A>> getRoutes() {
		return this.server.getRoutes();
	}

	@Override
	public Set<WebSocketRoute<A>> getWebSocketRoutes() {
		return this.server.getWebSocketRoutes();
	}

	@Override
	public WebRouteInterceptorManager<A, Intercepted<A>> intercept() {
		return this.server.intercept();
	}

	@Override
	public WebRouteManager<A, ? extends WebServer<A>> route() {
		return this.server.route();
	}

	@Override
	public WebSocketRouteManager<A, ? extends WebServer<A>> webSocketRoute() {
		return this.server.webSocketRoute();
	}

	@Override
	public Intercepted<A> configureInterceptors(WebRouteInterceptor.Configurer<? super A> configurer) {
		return this.server.configureInterceptors(configurer);
	}

	@Override
	public Intercepted<A> configureInterceptors(List<WebRouteInterceptor.Configurer<? super A>> configurers) {
		return this.server.configureInterceptors(configurers);
	}

	@Override
	public WebServer<A> configureRoutes(WebRouter.Configurer<? super A> configurer) {
		return this.server.configureRoutes(configurer);
	}

	@Override
	public WebServer<A> configureRoutes(List<WebRouter.Configurer<? super A>> configurers) {
		return this.server.configureRoutes(configurers);
	}

	@Override
	public Set<ErrorWebRoute<A>> getErrorRoutes() {
		return this.server.getErrorRoutes();
	}

	@Override
	public ErrorWebRouteInterceptorManager<A, Intercepted<A>> interceptError() {
		return this.server.interceptError();
	}

	@Override
	public ErrorWebRouteManager<A, ? extends WebServer<A>> routeError() {
		return this.server.routeError();
	}

	@Override
	public Intercepted<A> configureErrorInterceptors(ErrorWebRouteInterceptor.Configurer<? super A> configurer) {
		return this.server.configureErrorInterceptors(configurer);
	}

	@Override
	public Intercepted<A> configureErrorInterceptors(List<ErrorWebRouteInterceptor.Configurer<? super A>> configurers) {
		return this.server.configureErrorInterceptors(configurers);
	}

	@Override
	public WebServer<A> configureErrorRoutes(ErrorWebRouter.Configurer<? super A> configurer) {
		return this.server.configureErrorRoutes(configurer);
	}

	@Override
	public WebServer<A> configureErrorRoutes(List<ErrorWebRouter.Configurer<? super A>> configurers) {
		return this.server.configureErrorRoutes(configurers);
	}

	@Override
	public WebServer<A> configure(Configurer<? super A> configurer) {
		return this.server.configure(configurer);
	}

	@Override
	public WebServer<A> configure(List<Configurer<? super A>> configurers) {
		return this.server.configure(configurers);
	}

	/**
	 * <p>
	 * Returns the wrapped intercepted Web server.
	 * </p>
	 *
	 * @return an intercepted Web server
	 */
	public WebServer.Intercepted<A> unwrap() {
		return this.server;
	}
}