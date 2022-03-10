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

import io.inverno.mod.web.*;

import java.util.List;

/**
 * <p>
 * An {@link ErrorWebInterceptable} facade for error web intercepted routers.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
class GenericErrorWebInterceptableFacade implements ErrorWebInterceptable<GenericErrorWebInterceptableFacade> {

	private ErrorWebInterceptedRouter interceptedRouter;

	public GenericErrorWebInterceptableFacade(ErrorWebInterceptedRouter initialRouter) {
		this.interceptedRouter = initialRouter;
	}

	public ErrorWebInterceptedRouter getInterceptedRouter() {
		return interceptedRouter;
	}

	@Override
	public ErrorWebInterceptorManager<GenericErrorWebInterceptableFacade> intercept() {
		return new ErrorWebInterceptorManagerFacade(this.interceptedRouter.intercept());
	}

	@Override
	public GenericErrorWebInterceptableFacade configureInterceptors(ErrorWebInterceptorsConfigurer configurer) {
		this.interceptedRouter = this.interceptedRouter.configureInterceptors(configurer);
		return this;
	}

	@Override
	public GenericErrorWebInterceptableFacade configureInterceptors(List<ErrorWebInterceptorsConfigurer> configurers) {
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
	private class ErrorWebInterceptorManagerFacade implements ErrorWebInterceptorManager<GenericErrorWebInterceptableFacade> {

		private final ErrorWebInterceptorManager<ErrorWebInterceptedRouter> interceptorManager;

		public ErrorWebInterceptorManagerFacade(ErrorWebInterceptorManager<ErrorWebInterceptedRouter> interceptorManager) {
			this.interceptorManager = interceptorManager;
		}

		@Override
		public GenericErrorWebInterceptableFacade interceptor(ErrorWebExchangeInterceptor<? extends Throwable> interceptor) {
			GenericErrorWebInterceptableFacade.this.interceptedRouter = this.interceptorManager.interceptor(interceptor);
			return GenericErrorWebInterceptableFacade.this;
		}

		@Override
		public GenericErrorWebInterceptableFacade interceptors(List<ErrorWebExchangeInterceptor<? extends Throwable>> interceptors) {
			interceptors.forEach(this::interceptor);
			return GenericErrorWebInterceptableFacade.this;
		}

		@Override
		public ErrorWebInterceptorManager<GenericErrorWebInterceptableFacade> error(Class<? extends Throwable> error) {
			this.interceptorManager.error(error);
			return this;
		}

		@Override
		public ErrorWebInterceptorManager<GenericErrorWebInterceptableFacade> path(String path, boolean matchTrailingSlash) throws IllegalArgumentException {
			this.interceptorManager.path(path, matchTrailingSlash);
			return this;
		}

		@Override
		public ErrorWebInterceptorManager<GenericErrorWebInterceptableFacade> produces(String mediaRange) {
			this.interceptorManager.produces(mediaRange);
			return this;
		}

		@Override
		public ErrorWebInterceptorManager<GenericErrorWebInterceptableFacade> language(String languageRange) {
			this.interceptorManager.language(languageRange);
			return this;
		}
	}
}
