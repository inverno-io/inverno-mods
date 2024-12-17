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
package io.inverno.mod.web.client.internal;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.base.net.URIs;
import io.inverno.mod.discovery.ServiceID;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.client.ExchangeInterceptor;
import io.inverno.mod.http.client.HttpClient;
import io.inverno.mod.web.base.DataConversionService;
import io.inverno.mod.web.client.InterceptedWebExchange;
import io.inverno.mod.web.client.WebClient;
import io.inverno.mod.web.client.WebExchange;
import io.inverno.mod.web.client.WebRouteInterceptor;
import io.inverno.mod.web.client.WebRouteInterceptorManager;
import io.inverno.mod.web.client.internal.discovery.WebDiscoveryService;
import io.inverno.mod.web.client.internal.router.InternalWebRouteInterceptorRouter;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link WebClient} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class GenericWebClient<A extends ExchangeContext> implements WebClient<A> {

	private final DataConversionService dataConversionService;
	private final ObjectConverter<String> parameterConverter;
	private final HttpClient httpClient;
	private final WebDiscoveryService discoveryService;
	private final Supplier<A> contextFactory;

	/**
	 * <p>
	 * Creates a generic Web client.
	 * </p>
	 *
	 * @param dataConversionService the data conversion service
	 * @param parameterConverter    the parameter converter
	 * @param httpClient            the HTTP client
	 * @param discoveryService      the Web discovery service
	 * @param contextFactory        the exchange context factory
	 */
	public GenericWebClient(DataConversionService dataConversionService, ObjectConverter<String> parameterConverter, HttpClient httpClient, WebDiscoveryService discoveryService, Supplier<A> contextFactory) {
		this.dataConversionService = dataConversionService;
		this.parameterConverter = parameterConverter;
		this.discoveryService = discoveryService;
		this.httpClient = httpClient;
		this.contextFactory = contextFactory;
	}

	@Override
	public WebRouteInterceptorManager<A, Intercepted<A>> intercept() {
		return new GenericWebClient<A>.GenericInterceptedWebClient().intercept();
	}

	@Override
	public Intercepted<A> configureInterceptors(Configurer<? super A> configurer) {
		return new GenericWebClient<A>.GenericInterceptedWebClient().configureInterceptors(configurer);
	}

	@Override
	public Intercepted<A> configureInterceptors(List<Configurer<? super A>> configurers) {
		return new GenericWebClient<A>.GenericInterceptedWebClient().configureInterceptors(configurers);
	}

	@Override
	public Mono<WebExchange<A>> exchange(Method method, URI uri) {
		// No interceptors
		return this.httpClient.exchange(method, ServiceID.getRequestTarget(uri), this.contextFactory.get())
			.map(exchange -> new GenericWebExchange<>(this.dataConversionService, this.discoveryService, exchange, uri));
	}

	@Override
	public WebClient.WebExchangeBuilder<A> exchange(String uri) throws IllegalArgumentException {
		return new GenericWebExchangeBuilder<>(this, this.parameterConverter, uri);
	}

	/**
	 * <p>
	 * Generic {@link WebClient.WebExchangeBuilder} implementation.
	 * </p>
	 *
	 * @param <A> the exchange context type
	 */
	public static class GenericWebExchangeBuilder<A extends ExchangeContext> implements WebClient.WebExchangeBuilder<A> {

		private final WebClient<A> webClient;
		private final ObjectConverter<String> parameterConverter;
		private final String baseURI;
		private final URIBuilder requestTargetBuilder;

		private final Map<String, String> uriParameterValues;
		private Method method;

		/**
		 * <p>
		 * Creates a generic Web exchange builder.
		 * </p>
		 *
		 * @param webClient the Web client
		 * @param parameterConverter the parameter converter
		 * @param uri the service URI
		 *
		 * @throws IllegalArgumentException if the specified URI is not a valid service URI
		 */
		public GenericWebExchangeBuilder(WebClient<A> webClient, ObjectConverter<String> parameterConverter, String uri) throws IllegalArgumentException {
			this.webClient = webClient;
			this.parameterConverter = parameterConverter;
			this.uriParameterValues = new HashMap<>();
			this.method = Method.GET;
			// opaque URI defines the request target in the fragment

			int colonIndex = uri.indexOf(':');
			if(colonIndex == -1) {
				throw new IllegalArgumentException("Missing scheme");
			}
			if(uri.length() <= colonIndex + 1) {
				throw new IllegalArgumentException("Missing scheme specific part");
			}

			String requestTarget;
			if(uri.charAt(colonIndex + 1) == '/') {
				// scheme://...
				if(uri.length() <= colonIndex + 2 || uri.charAt(colonIndex + 2) != '/') {
					throw new IllegalArgumentException("Missing authority");
				}

				// We must make sure we don't have any fragment
				int fragmentIndex = uri.indexOf('#');
				if(fragmentIndex != -1) {
					uri = uri.substring(0, fragmentIndex);
				}

				int pathIndex = uri.indexOf('/', colonIndex + 3);
				if(pathIndex == -1) {
					this.baseURI = uri;
					requestTarget = "/";

				}
				else {
					this.baseURI = uri.substring(0, pathIndex);
					requestTarget = uri.substring(pathIndex);
				}
			}
			else {
				// scheme:...
				int fragmentIndex = uri.indexOf('#');
				if(fragmentIndex == -1) {
					this.baseURI = uri;
					requestTarget = "/";
				}
				else {
					this.baseURI = uri.substring(0, fragmentIndex);
					requestTarget = fragmentIndex + 1 == uri.length() ? "/" : uri.substring(fragmentIndex + 1);
				}
			}
			this.requestTargetBuilder = URIs.uri(requestTarget, URIs.RequestTargetForm.PATH_QUERY, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED);
		}

		/**
		 * <p>
		 * Creates a generic Web exchange builder from the specified parent builder.
		 * </p>
		 *
		 * <p>
		 * This is used in {@link #clone()} to clone an Web exchange builder.
		 * </p>
		 *
		 * @param parent the parent Web exchange builder
		 */
		public GenericWebExchangeBuilder(GenericWebExchangeBuilder<A> parent) {
			this.webClient = parent.webClient;
			this.parameterConverter = parent.parameterConverter;
			this.uriParameterValues = new HashMap<>(parent.uriParameterValues);
			this.method = parent.method;
			this.baseURI = parent.baseURI;
			this.requestTargetBuilder = parent.requestTargetBuilder.clone();
		}

		@Override
		public WebExchangeBuilder<A> method(Method method) {
			this.method = method;
			return this;
		}

		@Override
		public WebExchangeBuilder<A> path(String path) {
			this.requestTargetBuilder.path(path);
			return this;
		}

		@Override
		public <T> WebExchangeBuilder<A> pathParameter(String name, T value) {
			this.uriParameterValues.put(name, this.parameterConverter.encode(value));
			return this;
		}

		@Override
		public <T> WebExchangeBuilder<A> pathParameter(String name, T value, Type type) {
			this.uriParameterValues.put(name, this.parameterConverter.encode(value, type));
			return this;
		}

		@Override
		public <T> WebClient.WebExchangeBuilder<A> queryParameter(String name, T value) {
			this.requestTargetBuilder.queryParameter(name, this.parameterConverter.encode(value));
			return this;
		}

		@Override
		public <T> WebExchangeBuilder<A> queryParameter(String name, T value, Type type) {
			this.requestTargetBuilder.queryParameter(name, this.parameterConverter.encode(value, type));
			return this;
		}

		@Override
		public Mono<WebExchange<A>> build() {
			return this.webClient.exchange(this.method, URI.create(this.baseURI + this.requestTargetBuilder.build(this.uriParameterValues)));
		}

		@SuppressWarnings("MethodDoesntCallSuperMethod")
		@Override
		public WebExchangeBuilder<A> clone() {
			return new GenericWebExchangeBuilder<>(this);
		}
	}

	/**
	 * <p>
	 * Generic {@link WebClient.Intercepted} implementation
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	public class GenericInterceptedWebClient implements WebClient.Intercepted<A> {

		private final WebClient<A> parent;
		private final InternalWebRouteInterceptorRouter<A> interceptorRouter;

		/**
		 * <p>
		 * Creates a generic intercepted Web client.
		 * </p>
		 */
		public GenericInterceptedWebClient() {
			this.parent = GenericWebClient.this;
			this.interceptorRouter = new InternalWebRouteInterceptorRouter<>();
		}

		/**
		 * <p>
		 * Creates a generic intercepted Web client with the specified ancestor.
		 * </p>
		 *
		 * @param parent the parent intercepted Web client
		 */
		private GenericInterceptedWebClient(GenericWebClient<A>.GenericInterceptedWebClient parent) {
			this.parent = parent;
			this.interceptorRouter = parent.interceptorRouter;
		}

		@Override
		public WebRouteInterceptorManager<A, WebClient.Intercepted<A>> intercept() {
			return new GenericWebRouteInterceptorManager<>(new GenericWebClient<A>.GenericInterceptedWebClient(this), this.interceptorRouter.route());
		}

		@Override
		@SuppressWarnings("unchecked")
		public WebClient.Intercepted<A> configureInterceptors(Configurer<? super A> configurer) {
			if(configurer != null) {
				return (WebClient.Intercepted<A>)((WebRouteInterceptor.Configurer<A>)configurer).configure(this);
			}
			return this;
		}

		@Override
		public WebClient.Intercepted<A> configureInterceptors(List<Configurer<? super A>> configurers) {
			WebClient.Intercepted<A> client = this;
			if(configurers != null && !configurers.isEmpty()) {
				for(WebRouteInterceptor.Configurer<? super A> configurer : configurers) {
					client = client.configureInterceptors(configurer);
				}
			}
			return client;
		}

		@Override
		public Mono<WebExchange<A>> exchange(Method method, URI uri) {
			return GenericWebClient.this.httpClient.exchange(method, ServiceID.getRequestTarget(uri), GenericWebClient.this.contextFactory.get())
				.map(exchange -> new GenericWebExchange<>(GenericWebClient.this.dataConversionService, GenericWebClient.this.discoveryService, exchange, uri, this::resolveInterceptor));
		}

		@Override
		public WebExchangeBuilder<A> exchange(String uri) throws IllegalArgumentException {
			return new GenericWebExchangeBuilder<>(this, GenericWebClient.this.parameterConverter, uri);
		}

		/**
		 * <p>
		 * Resolves the interceptor to apply to the specified Web exchange.
		 * </p>
		 *
		 * <p>
		 * This method invokes the internal interceptor router to resolve matching interceptors that are then aggregated in the returned interceptor.
		 * </p>
		 *
		 * @param webExchange a Web exchange
		 *
		 * @return an Web exchange interceptor
		 */
		private ExchangeInterceptor<A, InterceptedWebExchange<A>> resolveInterceptor(WebExchange<A> webExchange) {
			LinkedHashMap<WebClient.Intercepted<A>, List<ExchangeInterceptor<A, InterceptedWebExchange<A>>>> interceptorsByAncestorOrSelf = new LinkedHashMap<>();
			GenericWebClient<A>.GenericInterceptedWebClient current = GenericWebClient.GenericInterceptedWebClient.this;
			while(current.parent != GenericWebClient.this) {
				interceptorsByAncestorOrSelf.putFirst(current, new ArrayList<>());
				current = (GenericWebClient<A>.GenericInterceptedWebClient)current.parent;
			}

			for(WebRouteInterceptors<A> webRouteInterceptors : this.interceptorRouter.resolveAll(webExchange)) {
				webRouteInterceptors.getInterceptors().forEach((client, clientInterceptors) -> {
					interceptorsByAncestorOrSelf.computeIfPresent(client, (ign, interceptors) -> {
						interceptors.addAll(clientInterceptors);
						return interceptors;
					});
				});
			}
			return interceptorsByAncestorOrSelf.values().stream().flatMap(List::stream).reduce(ExchangeInterceptor::andThen).orElseGet(() -> Mono::just);
		}

		@Override
		public WebClient<A> unwrap() {
			return this.parent;
		}
	}
}
