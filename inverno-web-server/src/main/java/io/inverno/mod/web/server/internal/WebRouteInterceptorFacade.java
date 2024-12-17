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
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.web.server.WebExchange;
import io.inverno.mod.web.server.WebRouteInterceptor;
import io.inverno.mod.web.server.WebRouteInterceptorManager;
import io.inverno.mod.web.server.WebServer;
import java.util.List;

/**
 * <p>
 * Scopes Web server instances when configuring route interceptors.
 * </p>
 *
 * <p>
 * {@link WebServer} implements {@link WebRouteInterceptor} but the Web server instance can't be exposed directly when configuring Web route interceptor using
 * {@link WebServer#configureInterceptors(Configurer)} for instance as one could then do a cast leading to undesirable side effects.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the exchange context type
 */
class WebRouteInterceptorFacade<A extends ExchangeContext> implements WebRouteInterceptor<A> {

	private final WebServer.Intercepted<A> server;

	/**
	 * <p>
	 * Creates a Web route interceptor facade wrapping the specified intercepted Web server.
	 * </p>
	 *
	 * @param server an intercepted Web server
	 */
	public WebRouteInterceptorFacade(WebServer.Intercepted<A> server) {
		this.server = server;
	}

	@Override
	public WebRouteInterceptorManager<A, ? extends WebRouteInterceptor<A>> intercept() {
		return new WebRouteInterceptorManagerFacade<>(this.server.intercept());
	}

	@Override
	@SuppressWarnings("unchecked")
	public WebRouteInterceptor<A> configureInterceptors(Configurer<? super A> configurer) {
		if(configurer != null) {
			return ((Configurer<A>)configurer).configure(new WebRouteInterceptorFacade<>(this.server));
		}
		return this;
	}

	@Override
	public WebRouteInterceptor<A> configureInterceptors(List<Configurer<? super A>> configurers) {
		WebRouteInterceptor<A> webRouteInterceptor = this;
		if(configurers != null && !configurers.isEmpty()) {
			for(WebRouteInterceptor.Configurer<? super A> configurer : configurers) {
				webRouteInterceptor = this.configureInterceptors(configurer);
			}
		}
		return webRouteInterceptor;
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

	/**
	 * <p>
	 * Web route interceptor manager returning {@link WebRouteInterceptorFacade} instead of a Web server instance when setting interceptors.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 */
	private static class WebRouteInterceptorManagerFacade<A extends ExchangeContext> implements WebRouteInterceptorManager<A, WebRouteInterceptor<A>> {

		private final WebRouteInterceptorManager<A, WebServer.Intercepted<A>> interceptorManager;

		/**
		 * <p>
		 * Creates a Web route interceptor manager facade.
		 * </p>
		 *
		 * @param interceptorManager an interceptor manager
		 */
		public WebRouteInterceptorManagerFacade(WebRouteInterceptorManager<A, WebServer.Intercepted<A>> interceptorManager) {
			this.interceptorManager = interceptorManager;
		}

		@Override
		public WebRouteInterceptorManager<A, WebRouteInterceptor<A>> path(String path, boolean matchTrailingSlash) {
			this.interceptorManager.path(path, matchTrailingSlash);
			return this;
		}

		@Override
		public WebRouteInterceptorManager<A, WebRouteInterceptor<A>> method(Method method) {
			this.interceptorManager.method(method);
			return this;
		}

		@Override
		public WebRouteInterceptorManager<A, WebRouteInterceptor<A>> consume(String mediaRange) {
			this.interceptorManager.consume(mediaRange);
			return this;
		}

		@Override
		public WebRouteInterceptorManager<A, WebRouteInterceptor<A>> produce(String mediaType) {
			this.interceptorManager.produce(mediaType);
			return this;
		}

		@Override
		public WebRouteInterceptorManager<A, WebRouteInterceptor<A>> language(String language) {
			this.interceptorManager.language(language);
			return this;
		}

		/**
		 * {@inheritDoc}
		 *
		 * <p>
		 * This method wraps the resulting intercepted Web server into a facade.
		 * </p>
		 *
		 * @return a Web route interceptor facade
		 */
		@Override
		public WebRouteInterceptor<A> interceptor(ExchangeInterceptor<? super A, WebExchange<A>> interceptor) {
			return new WebRouteInterceptorFacade<>(this.interceptorManager.interceptor(interceptor));
		}

		/**
		 * {@inheritDoc}
		 *
		 * <p>
		 * This method wraps the resulting intercepted Web server into a facade.
		 * </p>
		 *
		 * @return a Web route interceptor facade
		 */
		@Override
		public WebRouteInterceptor<A> interceptors(List<ExchangeInterceptor<? super A, WebExchange<A>>> interceptors) {
			return new WebRouteInterceptorFacade<>(this.interceptorManager.interceptors(interceptors));
		}
	}
}
