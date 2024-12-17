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

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.client.ExchangeInterceptor;
import io.inverno.mod.web.client.InterceptedWebExchange;
import io.inverno.mod.web.client.WebClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Web route interceptors used a resource in the {@link io.inverno.mod.web.client.internal.router.InternalWebRouteInterceptorRouter} to store the interceptors specified in a route.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the exchange context type
 */
public class WebRouteInterceptors<A extends ExchangeContext> {

	private final Map<WebClient.Intercepted<A>, List<ExchangeInterceptor<A, InterceptedWebExchange<A>>>> exchangeInterceptors;

	/**
	 * <p>
	 * Creates Web route interceptors.
	 * </p>
	 */
	public WebRouteInterceptors() {
		this.exchangeInterceptors = new HashMap<>();
	}

	/**
	 * <p>
	 * Creates Web route interceptors.
	 * </p>
	 *
	 * @param originatingClient   the originating intercepted Web client
	 * @param exchangeInterceptor a Web Exchange interceptor
	 */
	public WebRouteInterceptors(WebClient.Intercepted<A> originatingClient, ExchangeInterceptor<A, InterceptedWebExchange<A>> exchangeInterceptor) {
		this.exchangeInterceptors = new HashMap<>();
		this.add(originatingClient, exchangeInterceptor);
	}

	/**
	 * <p>
	 * Adds the specified Web exchange interceptor associated to the specified intercepted Web client.
	 * </p>
	 *
	 * @param originatingClient   the originating intercepted Web client
	 * @param exchangeInterceptor a Web Exchange interceptor
	 */
	public final void add(WebClient.Intercepted<A> originatingClient, ExchangeInterceptor<A, InterceptedWebExchange<A>> exchangeInterceptor) {
		this.exchangeInterceptors.computeIfAbsent(originatingClient, ign -> new ArrayList<>()).add(exchangeInterceptor);
	}

	/**
	 * <p>
	 * Adds all the specified Web exchange interceptors associated to the specified intercepted Web clients.
	 * </p>
	 *
	 * @param exchangeInterceptors a map associating intercepted Web clients with Web exchange interceptors
	 */
	public final void addAll(Map<WebClient.Intercepted<A>, List<ExchangeInterceptor<A, InterceptedWebExchange<A>>>> exchangeInterceptors) {
		exchangeInterceptors.forEach((originatingClient, interceptors) -> {
			this.exchangeInterceptors.computeIfAbsent(originatingClient, ign -> new ArrayList<>()).addAll(interceptors);
		});
	}

	/**
	 * <p>
	 * Returns the Web exchange interceptors.
	 * </p>
	 *
	 * @return a map associating intercepted Web clients with Web exchange interceptors
	 */
	public Map<WebClient.Intercepted<A>, List<ExchangeInterceptor<A, InterceptedWebExchange<A>>>> getInterceptors() {
		return exchangeInterceptors;
	}
}
