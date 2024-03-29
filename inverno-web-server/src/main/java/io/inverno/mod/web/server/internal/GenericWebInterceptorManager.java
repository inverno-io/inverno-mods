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
package io.inverno.mod.web.server.internal;

import io.inverno.mod.base.net.URIPattern;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.header.HeaderCodec;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.web.server.WebExchange;
import io.inverno.mod.web.server.WebInterceptedRouter;
import io.inverno.mod.web.server.WebInterceptorManager;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * <p>
 * Generic {@link WebInterceptorManager} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 */
class GenericWebInterceptorManager extends AbstractWebManager<GenericWebInterceptorManager> implements WebInterceptorManager<ExchangeContext, WebInterceptedRouter<ExchangeContext>> {

	private final GenericWebInterceptedRouter router;
	private final HeaderCodec<? extends Headers.ContentType> contentTypeCodec;
	private final HeaderCodec<? extends Headers.AcceptLanguage> acceptLanguageCodec;
	
	private ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> interceptor;

	/**
	 * <p>
	 * Creates a generic web interceptor manager.
	 * </p>
	 * 
	 * @param router a generic web intercepted router
	 * @param contentTypeCodec a content type header codec
	 * @param acceptLanguageCodec an accept language header codec
	 */
	public GenericWebInterceptorManager(GenericWebInterceptedRouter router, HeaderCodec<? extends Headers.ContentType> contentTypeCodec, HeaderCodec<? extends Headers.AcceptLanguage> acceptLanguageCodec) {
		this.router = router;
		this.contentTypeCodec = contentTypeCodec;
		this.acceptLanguageCodec = acceptLanguageCodec;
	}

	@Override
	public WebInterceptedRouter<ExchangeContext> interceptor(ExchangeInterceptor<? super ExchangeContext, WebExchange<ExchangeContext>> interceptor) {
		Objects.requireNonNull(interceptor);
		this.interceptor = interceptor;
		this.commit();

		return this.router;
	}

	@Override
	public WebInterceptedRouter<ExchangeContext> interceptors(List<ExchangeInterceptor<? super ExchangeContext, WebExchange<ExchangeContext>>> interceptors) {
		interceptors.forEach(this::interceptor);
		return this.router;
	}
	
	private void commit() {
		Consumer<GenericWebRouteInterceptor> languagesCommitter = routeInterceptor -> {
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
		
		Consumer<GenericWebRouteInterceptor> producesCommitter = routeInterceptor -> {
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
		
		Consumer<GenericWebRouteInterceptor> consumesCommitter = routeInterceptor -> {
			if(this.consumes != null && !this.consumes.isEmpty()) {
				for(String consume : this.consumes) {
					routeInterceptor.setConsume(consume);
					producesCommitter.accept(routeInterceptor);
				}
			}
			else {
				producesCommitter.accept(routeInterceptor);
			}
		};
		
		Consumer<GenericWebRouteInterceptor> methodsCommitter = routeInterceptor -> {
			if(this.methods != null && !this.methods.isEmpty()) {
				for(Method method : this.methods) {
					routeInterceptor.setMethod(method);
					consumesCommitter.accept(routeInterceptor);
				}
			}
			else {
				consumesCommitter.accept(routeInterceptor);
			}
		};
		
		Consumer<GenericWebRouteInterceptor> pathCommitter = routeInterceptor -> {
			if(this.paths != null && !this.paths.isEmpty() || this.pathPatterns != null && !this.pathPatterns.isEmpty()) {
				if(this.paths != null) {
					for(String path : this.paths) {
						routeInterceptor.setPath(path);
						methodsCommitter.accept(routeInterceptor);
					}
				}
				if(this.pathPatterns != null) {
					for(URIPattern pathPattern : this.pathPatterns) {
						routeInterceptor.setPathPattern(pathPattern);
						methodsCommitter.accept(routeInterceptor);
					}
				}
			}
			else {
				methodsCommitter.accept(routeInterceptor);
			}
		};
		pathCommitter.accept(new GenericWebRouteInterceptor(this.contentTypeCodec, this.acceptLanguageCodec));
	}
}
