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
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.web.server.ErrorWebExchange;
import io.inverno.mod.web.server.ErrorWebRouteInterceptorManager;
import io.inverno.mod.web.server.WebServer;
import io.inverno.mod.web.server.internal.router.InternalErrorWebRouteInterceptorRouter;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Generic {@link ErrorWebRouteInterceptorManager} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the exchange context type
 * @param <B> the intercepted Web server type
 */
public class GenericErrorWebRouteInterceptorManager<A extends ExchangeContext, B extends WebServer.Intercepted<A> & Intercepting<A>> implements ErrorWebRouteInterceptorManager<A, WebServer.Intercepted<A>> {

	private final B server;
	private final InternalErrorWebRouteInterceptorRouter.InterceptorRouteManager<A> routeManager;

	/**
	 * <p>
	 * Creates a generic error Web route interceptor manager.
	 * </p>
	 *
	 * @param server       an intercepted server
	 * @param routeManager an internal error interceptor route manager
	 */
	public GenericErrorWebRouteInterceptorManager(B server, InternalErrorWebRouteInterceptorRouter.InterceptorRouteManager<A> routeManager) {
		this.server = server;
		this.routeManager = routeManager;
	}

	@Override
	public ErrorWebRouteInterceptorManager<A, WebServer.Intercepted<A>> error(Class<? extends Throwable> error) {
		this.routeManager.errorType(error);
		return this;
	}

	@Override
	public ErrorWebRouteInterceptorManager<A, WebServer.Intercepted<A>> path(String path, boolean matchTrailingSlash) {
		this.routeManager.resolvePath(path, matchTrailingSlash);
		return this;
	}

	@Override
	public ErrorWebRouteInterceptorManager<A, WebServer.Intercepted<A>> consume(String mediaRange) {
		this.routeManager.contentType(mediaRange);
		return this;
	}

	@Override
	public ErrorWebRouteInterceptorManager<A, WebServer.Intercepted<A>> produce(String mediaType) {
		this.routeManager.accept(mediaType);
		return this;
	}

	@Override
	public ErrorWebRouteInterceptorManager<A, WebServer.Intercepted<A>> language(String language) {
		this.routeManager.language(language);
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public WebServer.Intercepted<A> interceptor(ExchangeInterceptor<? super A, ErrorWebExchange<A>> interceptor) {
		Objects.requireNonNull(interceptor);
		this.server.setErrorWebRouteInterceptorRouter(this.routeManager.set((ExchangeInterceptor<A, ErrorWebExchange<A>>) interceptor));
		return this.server;
	}

	@Override
	@SuppressWarnings("unchecked")
	public WebServer.Intercepted<A> interceptors(List<ExchangeInterceptor<? super A, ErrorWebExchange<A>>> interceptors) {
		Objects.requireNonNull(interceptors);
		if(interceptors.isEmpty()) {
			throw new IllegalArgumentException("Empty interceptors");
		}
		InternalErrorWebRouteInterceptorRouter<A> interceptedRouter = null;
		for(ExchangeInterceptor<? super A, ErrorWebExchange<A>> interceptor : interceptors) {
			interceptedRouter = this.routeManager.set((ExchangeInterceptor<A, ErrorWebExchange<A>>) interceptor);
		}
		this.server.setErrorWebRouteInterceptorRouter(interceptedRouter);
		return this.server;
	}
}