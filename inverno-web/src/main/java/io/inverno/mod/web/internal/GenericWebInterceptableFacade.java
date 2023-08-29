/*
 * Copyright 2021 Jeremy KUHN
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

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.WebInterceptable;
import io.inverno.mod.web.WebInterceptedRouter;
import io.inverno.mod.web.WebInterceptorManager;
import io.inverno.mod.web.WebInterceptorsConfigurer;
import java.util.List;

/**
 * <p>
 * A {@link WebInterceptable} facade for web intercepted routers.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 */
class GenericWebInterceptableFacade implements WebInterceptable<ExchangeContext, GenericWebInterceptableFacade> {

	private WebInterceptedRouter<ExchangeContext> interceptedRouter;
	
	public GenericWebInterceptableFacade(WebInterceptedRouter<ExchangeContext> initialRouter) {
		this.interceptedRouter = initialRouter;
	}

	public WebInterceptedRouter<ExchangeContext> getInterceptedRouter() {
		return interceptedRouter;
	}
	
	@Override
	public WebInterceptorManager<ExchangeContext, GenericWebInterceptableFacade> intercept() {
		return new WebInterceptorManagerFacade(this.interceptedRouter.intercept());
	}

	@Override
	public GenericWebInterceptableFacade configureInterceptors(WebInterceptorsConfigurer<? super ExchangeContext> configurer) {
		this.interceptedRouter = this.interceptedRouter.configureInterceptors(configurer);
		return this;
	}

	@Override
	public GenericWebInterceptableFacade configureInterceptors(List<WebInterceptorsConfigurer<? super ExchangeContext>> configurers) {
		this.interceptedRouter = this.interceptedRouter.configureInterceptors(configurers);
		return this;
	}

	/**
	 * <p>
	 * A Web interceptor manager that delegates to an underlying interceptor manager while keeping track of the resulting Web intercepted router.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.3
	 */
	private class WebInterceptorManagerFacade implements WebInterceptorManager<ExchangeContext, GenericWebInterceptableFacade> {

		private final WebInterceptorManager<ExchangeContext, WebInterceptedRouter<ExchangeContext>> interceptorManager;

		public WebInterceptorManagerFacade(WebInterceptorManager<ExchangeContext, WebInterceptedRouter<ExchangeContext>> interceptorManager) {
			this.interceptorManager = interceptorManager;
		}

		@Override
		public GenericWebInterceptableFacade interceptor(ExchangeInterceptor<? super ExchangeContext, WebExchange<ExchangeContext>> interceptor) {
			GenericWebInterceptableFacade.this.interceptedRouter = this.interceptorManager.interceptor(interceptor);
			return GenericWebInterceptableFacade.this;
		}

		@Override
		public GenericWebInterceptableFacade interceptors(List<ExchangeInterceptor<? super ExchangeContext, WebExchange<ExchangeContext>>> interceptors) {
			interceptors.forEach(this::interceptor);
			return GenericWebInterceptableFacade.this;
		}
		
		@Override
		public WebInterceptorManager<ExchangeContext, GenericWebInterceptableFacade> path(String path, boolean matchTrailingSlash) throws IllegalArgumentException {
			this.interceptorManager.path(path, matchTrailingSlash);
			return this;
		}

		@Override
		public WebInterceptorManager<ExchangeContext, GenericWebInterceptableFacade> method(Method method) {
			this.interceptorManager.method(method);
			return this;
		}

		@Override
		public WebInterceptorManager<ExchangeContext, GenericWebInterceptableFacade> consumes(String mediaRange) {
			this.interceptorManager.consumes(mediaRange);
			return this;
		}

		@Override
		public WebInterceptorManager<ExchangeContext, GenericWebInterceptableFacade> produces(String mediaRange) {
			this.interceptorManager.produces(mediaRange);
			return this;
		}

		@Override
		public WebInterceptorManager<ExchangeContext, GenericWebInterceptableFacade> language(String languageRange) {
			this.interceptorManager.language(languageRange);
			return this;
		}
	}
}
