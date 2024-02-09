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

import io.inverno.mod.base.net.URIPattern;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.header.HeaderCodec;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.web.server.ErrorWebExchange;
import io.inverno.mod.web.server.ErrorWebInterceptedRouter;
import io.inverno.mod.web.server.ErrorWebInterceptorManager;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * <p>
 * Generic {@link ErrorWebInterceptorManager} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
class GenericErrorWebInterceptorManager extends AbstractErrorWebManager<GenericErrorWebInterceptorManager> implements ErrorWebInterceptorManager<ExchangeContext, ErrorWebInterceptedRouter<ExchangeContext>> {

	private final GenericErrorWebInterceptedRouter router;
	private final HeaderCodec<? extends Headers.ContentType> contentTypeCodec;
	private final HeaderCodec<? extends Headers.AcceptLanguage> acceptLanguageCodec;

	private ExchangeInterceptor<ExchangeContext, ErrorWebExchange<ExchangeContext>> interceptor;

	/**
	 * <p>
	 * Creates a generic error web interceptor manager.
	 * </p>
	 *
	 * @param router a generic error web intercepted router
	 * @param contentTypeCodec a content type header codec
	 * @param acceptLanguageCodec an accept language header codec
	 */
	public GenericErrorWebInterceptorManager(GenericErrorWebInterceptedRouter router, HeaderCodec<? extends Headers.ContentType> contentTypeCodec, HeaderCodec<? extends Headers.AcceptLanguage> acceptLanguageCodec) {
		this.router = router;
		this.contentTypeCodec = contentTypeCodec;
		this.acceptLanguageCodec = acceptLanguageCodec;
	}

	@Override
	public ErrorWebInterceptedRouter<ExchangeContext> interceptor(ExchangeInterceptor<? super ExchangeContext, ErrorWebExchange<ExchangeContext>> interceptor) {
		Objects.requireNonNull(interceptor);
		this.interceptor = interceptor;
		this.commit();
		return this.router;
	}

	@Override
	public ErrorWebInterceptedRouter<ExchangeContext> interceptors(List<ExchangeInterceptor<? super ExchangeContext, ErrorWebExchange<ExchangeContext>>> interceptors) {
		interceptors.forEach(this::interceptor);
		return this.router;
	}

	private void commit() {
		Consumer<GenericErrorWebRouteInterceptor> languagesCommitter = routeInterceptor -> {
			if(this.languages != null && !this.languages.isEmpty()) {
				for(String language : this.languages) {
					routeInterceptor.setLanguage(language);
					routeInterceptor.setInterceptor(this.interceptor);
					this.router.addRouteInterceptor(routeInterceptor.clone());
				}
			}
			else {
				routeInterceptor.setInterceptor(this.interceptor);
				this.router.addRouteInterceptor(routeInterceptor.clone());
			}
		};

		Consumer<GenericErrorWebRouteInterceptor> producesCommitter = routeInterceptor -> {
			if(this.produces != null && !this.produces.isEmpty()) {
				for(String produce : this.produces) {
					routeInterceptor.setProduce(produce);
					languagesCommitter.accept(routeInterceptor);
				}
			}
			else {
				languagesCommitter.accept(routeInterceptor);
			}
		};

		Consumer<GenericErrorWebRouteInterceptor> pathCommitter = routeInterceptor -> {
			if(this.paths != null && !this.paths.isEmpty() || this.pathPatterns != null && !this.pathPatterns.isEmpty()) {
				if(this.paths != null) {
					for(String path : this.paths) {
						routeInterceptor.setPath(path);
						producesCommitter.accept(routeInterceptor);
					}
				}
				if(this.pathPatterns != null) {
					for(URIPattern pathPattern : this.pathPatterns) {
						routeInterceptor.setPathPattern(pathPattern);
						producesCommitter.accept(routeInterceptor);
					}
				}
			}
			else {
				producesCommitter.accept(routeInterceptor);
			}
		};

		Consumer<GenericErrorWebRouteInterceptor> errorsCommitter = routeInterceptor -> {
			if(this.errors != null && !this.errors.isEmpty()) {
				for(Class<? extends Throwable> error : this.errors) {
					routeInterceptor.setError(error);
					pathCommitter.accept(routeInterceptor);
				}
			}
			else {
				pathCommitter.accept(routeInterceptor);
			}
		};

		errorsCommitter.accept(new GenericErrorWebRouteInterceptor(this.contentTypeCodec, this.acceptLanguageCodec));
	}
}
