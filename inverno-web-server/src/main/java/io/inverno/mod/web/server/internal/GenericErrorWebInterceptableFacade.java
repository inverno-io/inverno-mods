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
package io.inverno.mod.web.server.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.web.server.ErrorWebExchange;
import io.inverno.mod.web.server.ErrorWebInterceptable;
import io.inverno.mod.web.server.ErrorWebInterceptedRouter;
import io.inverno.mod.web.server.ErrorWebInterceptorManager;
import io.inverno.mod.web.server.ErrorWebInterceptorsConfigurer;
import java.util.List;

/**
 * <p>
 * An {@link ErrorWebInterceptable} facade for error web intercepted routers.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
class GenericErrorWebInterceptableFacade implements ErrorWebInterceptable<ExchangeContext, GenericErrorWebInterceptableFacade> {

	private ErrorWebInterceptedRouter<ExchangeContext> interceptedRouter;

	public GenericErrorWebInterceptableFacade(ErrorWebInterceptedRouter<ExchangeContext> initialRouter) {
		this.interceptedRouter = initialRouter;
	}

	public ErrorWebInterceptedRouter<ExchangeContext> getInterceptedRouter() {
		return interceptedRouter;
	}

	@Override
	public ErrorWebInterceptorManager<ExchangeContext, GenericErrorWebInterceptableFacade> intercept() {
		return new ErrorWebInterceptorManagerFacade(this.interceptedRouter.intercept());
	}

	@Override
	public GenericErrorWebInterceptableFacade configureInterceptors(ErrorWebInterceptorsConfigurer<? super ExchangeContext> configurer) {
		this.interceptedRouter = this.interceptedRouter.configureInterceptors(configurer);
		return this;
	}

	@Override
	public GenericErrorWebInterceptableFacade configureInterceptors(List<ErrorWebInterceptorsConfigurer<? super ExchangeContext>> configurers) {
		this.interceptedRouter = this.interceptedRouter.configureInterceptors(configurers);
		return this;
	}

	/**
	 * <p>
	 * An Error  Web interceptor manager that delegates to an underlying interceptor manager while keeping track of the
	 * resulting Error Web intercepted router.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	private class ErrorWebInterceptorManagerFacade implements ErrorWebInterceptorManager<ExchangeContext, GenericErrorWebInterceptableFacade> {

		private final ErrorWebInterceptorManager<ExchangeContext, ErrorWebInterceptedRouter<ExchangeContext>> interceptorManager;

		public ErrorWebInterceptorManagerFacade(ErrorWebInterceptorManager<ExchangeContext, ErrorWebInterceptedRouter<ExchangeContext>> interceptorManager) {
			this.interceptorManager = interceptorManager;
		}

		@Override
		public GenericErrorWebInterceptableFacade interceptor(ExchangeInterceptor<? super ExchangeContext, ErrorWebExchange<ExchangeContext>> interceptor) {
			GenericErrorWebInterceptableFacade.this.interceptedRouter = this.interceptorManager.interceptor(interceptor);
			return GenericErrorWebInterceptableFacade.this;
		}

		@Override
		public GenericErrorWebInterceptableFacade interceptors(List<ExchangeInterceptor<? super ExchangeContext, ErrorWebExchange<ExchangeContext>>> interceptors) {
			interceptors.forEach(this::interceptor);
			return GenericErrorWebInterceptableFacade.this;
		}

		@Override
		public ErrorWebInterceptorManager<ExchangeContext, GenericErrorWebInterceptableFacade> error(Class<? extends Throwable> error) {
			this.interceptorManager.error(error);
			return this;
		}
		
		@Override
		public ErrorWebInterceptorManager<ExchangeContext, GenericErrorWebInterceptableFacade> path(String path, boolean matchTrailingSlash) throws IllegalArgumentException {
			this.interceptorManager.path(path, matchTrailingSlash);
			return this;
		}

		@Override
		public ErrorWebInterceptorManager<ExchangeContext, GenericErrorWebInterceptableFacade> produces(String mediaRange) {
			this.interceptorManager.produces(mediaRange);
			return this;
		}

		@Override
		public ErrorWebInterceptorManager<ExchangeContext, GenericErrorWebInterceptableFacade> language(String languageRange) {
			this.interceptorManager.language(languageRange);
			return this;
		}
	}
}
