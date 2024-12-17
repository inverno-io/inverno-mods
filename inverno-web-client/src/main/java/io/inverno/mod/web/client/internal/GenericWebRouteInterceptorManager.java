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

import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.base.net.URIs;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.client.ExchangeInterceptor;
import io.inverno.mod.web.client.InterceptedWebExchange;
import io.inverno.mod.web.client.WebClient;
import io.inverno.mod.web.client.WebRouteInterceptorManager;
import io.inverno.mod.web.client.internal.router.InternalWebRouteInterceptorRouter;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * <p>
 * Generic {@link WebRouteInterceptorManager} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class GenericWebRouteInterceptorManager<A extends ExchangeContext> implements WebRouteInterceptorManager<A, WebClient.Intercepted<A>> {

	private final WebClient.Intercepted<A> client;
	private final InternalWebRouteInterceptorRouter.RouteManager<A> routeManager;

	/**
	 * <p>
	 * Creates a generic Web route interceptor manager.
	 * </p>
	 *
	 * @param client       an intercepted Web client
	 * @param routeManager an internal interceptor route manager
	 */
	public GenericWebRouteInterceptorManager(WebClient.Intercepted<A> client, InternalWebRouteInterceptorRouter.RouteManager<A> routeManager) {
		this.client = client;
		this.routeManager = routeManager;
	}

	@Override
	public WebRouteInterceptorManager<A, WebClient.Intercepted<A>> uri(Consumer<UriConfigurator> uriConfigurer) {
		GenericUriConfigurator uriConfigurator = new GenericUriConfigurator();
		uriConfigurer.accept(uriConfigurator);
		uriConfigurator.apply(this.routeManager);
		return this;
	}

	@Override
	public WebRouteInterceptorManager<A, WebClient.Intercepted<A>> method(Method method) {
		this.routeManager.method(method);
		return this;
	}

	@Override
	public WebRouteInterceptorManager<A, WebClient.Intercepted<A>> consume(String mediaRange) {
		this.routeManager.accept(mediaRange);
		return this;
	}

	@Override
	public WebRouteInterceptorManager<A, WebClient.Intercepted<A>> produce(String mediaRange) {
		this.routeManager.contentType(mediaRange);
		return this;
	}

	@Override
	public WebRouteInterceptorManager<A, WebClient.Intercepted<A>> language(String languageRange) {
		this.routeManager.language(languageRange);
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public WebClient.Intercepted<A> interceptor(ExchangeInterceptor<? super A, InterceptedWebExchange<A>> interceptor) {
		Objects.requireNonNull(interceptor);
		this.routeManager.set(() -> new WebRouteInterceptors<>(this.client, (ExchangeInterceptor<A, InterceptedWebExchange<A>>)interceptor));
		return this.client;
	}

	@Override
	@SuppressWarnings("unchecked")
	public WebClient.Intercepted<A> interceptors(List<? extends ExchangeInterceptor<? super A, InterceptedWebExchange<A>>> interceptors) {
		Objects.requireNonNull(interceptors);
		if(interceptors.isEmpty()) {
			throw new IllegalArgumentException("Empty interceptors");
		}
		for(ExchangeInterceptor<? super A, InterceptedWebExchange<A>> interceptor : interceptors) {
			this.routeManager.set(() -> new WebRouteInterceptors<>(this.client, (ExchangeInterceptor<A, InterceptedWebExchange<A>>)interceptor));
		}
		return this.client;
	}

	private static class GenericUriConfigurator implements WebRouteInterceptorManager.UriConfigurator {

		private final URIBuilder uriBuilder;

		private boolean matchTrailingSlash;

		public GenericUriConfigurator() {
			this.uriBuilder = URIs.uri(URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);
		}

		@Override
		public UriConfigurator scheme(String scheme) {
			this.uriBuilder.scheme(scheme);
			return this;
		}

		@Override
		public UriConfigurator authority(String authority) {
			this.uriBuilder.authority(authority);
			return this;
		}

		@Override
		public UriConfigurator userInfo(String userInfo) {
			this.uriBuilder.userInfo(userInfo);
			return this;
		}

		@Override
		public UriConfigurator host(String host) {
			this.uriBuilder.host(host);
			return this;
		}

		@Override
		public UriConfigurator port(String port) {
			this.uriBuilder.port(port);
			return this;
		}

		@Override
		public UriConfigurator path(String path, boolean matchTrailingSlash) {
			this.uriBuilder.path(path);
			this.matchTrailingSlash = matchTrailingSlash;
			return this;
		}

		private void apply(InternalWebRouteInterceptorRouter.RouteManager<?> routeManager) {
			List<String> pathParameterNames = this.uriBuilder.getParameterNames();
			if (pathParameterNames.isEmpty()) {
				// Static URI
				String rawURI = this.uriBuilder.buildRawString();
				routeManager.uri(rawURI);
				if (this.matchTrailingSlash) {
					if (rawURI.endsWith("/")) {
						routeManager.uri(rawURI.substring(0, rawURI.length() - 1));
					}
					else {
						routeManager.uri(rawURI + "/");
					}
				}
			}
			else {
				// URI pattern
				routeManager.uriPattern(this.uriBuilder.buildPattern(this.matchTrailingSlash));
			}
		}
	}
}
