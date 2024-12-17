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
import io.inverno.mod.web.server.ErrorWebRouteInterceptor;
import io.inverno.mod.web.server.ErrorWebRouteInterceptorManager;
import io.inverno.mod.web.server.WebServer;
import java.util.List;

/**
 * <p>
 * Scopes Web server instances when configuring error Web route interceptors.
 * </p>
 *
 * <p>
 * {@link WebServer} implements {@link ErrorWebRouteInterceptor} but the Web server instance can't be exposed directly when configuring error Web route interceptor using
 * {@link WebServer#configureErrorInterceptors(Configurer)}  for instance as one could then do a cast leading to undesirable side effects.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the exchange context type
 */
class ErrorWebRouteInterceptorFacade<A extends ExchangeContext> implements ErrorWebRouteInterceptor<A> {

	private final WebServer.Intercepted<A> server;

	/**
	 * <p>
	 * Creates an error Web route interceptor facade wrapping the specified intercepted Web server.
	 * </p>
	 *
	 * @param server an intercepted Web server
	 */
	public ErrorWebRouteInterceptorFacade(WebServer.Intercepted<A> server) {
		this.server = server;
	}

	@Override
	public ErrorWebRouteInterceptorManager<A, ? extends ErrorWebRouteInterceptor<A>> interceptError() {
		return new ErrorWebRouteInterceptorManagerFacade<>(this.server.interceptError());
	}

	@Override
	@SuppressWarnings("unchecked")
	public ErrorWebRouteInterceptor<A> configureErrorInterceptors(ErrorWebRouteInterceptor.Configurer<? super A> configurer) {
		if(configurer != null) {
			return ((ErrorWebRouteInterceptor.Configurer<A>)configurer).configure(new ErrorWebRouteInterceptorFacade<>(this.server));
		}
		return this;
	}

	@Override
	public ErrorWebRouteInterceptor<A> configureErrorInterceptors(List<Configurer<? super A>> configurers) {
		ErrorWebRouteInterceptor<A> webRouteInterceptor = this;
		if(configurers != null && !configurers.isEmpty()) {
			for(ErrorWebRouteInterceptor.Configurer<? super A> configurer : configurers) {
				webRouteInterceptor = this.configureErrorInterceptors(configurer);
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
	 * Error Web route interceptor manager returning {@link ErrorWebRouteInterceptorFacade} instead of a Web server instance when setting interceptors.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 */
	private static class ErrorWebRouteInterceptorManagerFacade<A extends ExchangeContext> implements ErrorWebRouteInterceptorManager<A, ErrorWebRouteInterceptor<A>> {

		private final ErrorWebRouteInterceptorManager<A, WebServer.Intercepted<A>> interceptorManager;

		/**
		 * <p>
		 * Creates an error Web route interceptor manager facade.
		 * </p>
		 *
		 * @param interceptorManager an interceptor manager
		 */
		public ErrorWebRouteInterceptorManagerFacade(ErrorWebRouteInterceptorManager<A, WebServer.Intercepted<A>> interceptorManager) {
			this.interceptorManager = interceptorManager;
		}

		@Override
		public ErrorWebRouteInterceptorManager<A, ErrorWebRouteInterceptor<A>> error(Class<? extends Throwable> error) {
			this.interceptorManager.error(error);
			return this;
		}

		@Override
		public ErrorWebRouteInterceptorManager<A, ErrorWebRouteInterceptor<A>> path(String path, boolean matchTrailingSlash) {
			this.interceptorManager.path(path, matchTrailingSlash);
			return this;
		}

		@Override
		public ErrorWebRouteInterceptorManager<A, ErrorWebRouteInterceptor<A>> consume(String mediaRange) {
			this.interceptorManager.consume(mediaRange);
			return this;
		}

		@Override
		public ErrorWebRouteInterceptorManager<A, ErrorWebRouteInterceptor<A>> produce(String mediaType) {
			this.interceptorManager.produce(mediaType);
			return this;
		}

		@Override
		public ErrorWebRouteInterceptorManager<A, ErrorWebRouteInterceptor<A>> language(String language) {
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
		 * @return an error Web route interceptor facade
		 */
		@Override
		public ErrorWebRouteInterceptor<A> interceptor(ExchangeInterceptor<? super A, ErrorWebExchange<A>> interceptor) {
			return new ErrorWebRouteInterceptorFacade<>(this.interceptorManager.interceptor(interceptor));
		}

		/**
		 * {@inheritDoc}
		 *
		 * <p>
		 * This method wraps the resulting intercepted Web server into a facade.
		 * </p>
		 *
		 * @return an error Web route interceptor facade
		 */
		@Override
		public ErrorWebRouteInterceptor<A> interceptors(List<ExchangeInterceptor<? super A, ErrorWebExchange<A>>> interceptors) {
			return new ErrorWebRouteInterceptorFacade<>(this.interceptorManager.interceptors(interceptors));
		}
	}
}
