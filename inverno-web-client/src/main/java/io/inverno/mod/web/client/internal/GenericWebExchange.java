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

import io.inverno.mod.discovery.ServiceID;
import io.inverno.mod.discovery.ServiceNotFoundException;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.ExchangeInterceptor;
import io.inverno.mod.http.client.UnboundExchange;
import io.inverno.mod.web.base.DataConversionService;
import io.inverno.mod.web.client.InterceptedWebExchange;
import io.inverno.mod.web.client.WebExchange;
import io.inverno.mod.web.client.WebRequest;
import io.inverno.mod.web.client.WebResponse;
import io.inverno.mod.web.client.internal.discovery.WebDiscoveryService;
import io.inverno.mod.web.client.internal.ws.GenericWeb2SocketExchange;
import io.inverno.mod.web.client.ws.Web2SocketExchange;
import java.net.URI;
import java.util.Optional;
import java.util.function.Function;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link WebExchange} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class GenericWebExchange<A extends ExchangeContext> implements WebExchange<A> {

	private static final Function<WebResponse, Mono<Void>> BASIC_ERROR_MAPPER = webResponse -> Mono.error(HttpException.fromStatus(webResponse.headers().getStatus()));

	private final DataConversionService dataConversionService;
	private final WebDiscoveryService discoveryService;
	private final UnboundExchange<A> unboundExchange;
	private final GenericWebRequest request;
	private final Function<WebExchange<A>, ExchangeInterceptor<A, InterceptedWebExchange<A>>> interceptorResolver;

	private boolean failOnErrorStatus = true;
	private Function<WebResponse, Mono<Void>> errorMapper = BASIC_ERROR_MAPPER;

	/**
	 * <p>
	 * Creates a generic Web exchange.
	 * </p>
	 *
	 * @param dataConversionService the data conversion service
	 * @param discoveryService      the Web discovery service
	 * @param unboundExchange       the originating unbound exchange
	 * @param uri                   the service URI
	 */
	public GenericWebExchange(DataConversionService dataConversionService, WebDiscoveryService discoveryService, UnboundExchange<A> unboundExchange, URI uri) {
		this(dataConversionService, discoveryService, unboundExchange, uri, null);
	}

	/**
	 * <p>
	 * Creates a generic Web exchange.
	 * </p>
	 *
	 * @param dataConversionService the data conversion service
	 * @param discoveryService      the Web discovery service
	 * @param unboundExchange       the originating unbound exchange
	 * @param uri                   the service URI
	 * @param interceptorResolver   the interceptor resolver
	 */
	public GenericWebExchange(DataConversionService dataConversionService, WebDiscoveryService discoveryService, UnboundExchange<A> unboundExchange, URI uri, Function<WebExchange<A>, ExchangeInterceptor<A, InterceptedWebExchange<A>>> interceptorResolver) {
		this.dataConversionService = dataConversionService;
		this.discoveryService = discoveryService;
		this.unboundExchange = unboundExchange;
		this.request = new GenericWebRequest(dataConversionService, uri, unboundExchange.request());
		this.interceptorResolver = interceptorResolver;
	}

	@Override
	public WebExchange<A> failOnErrorStatus(boolean failOnErrorStatus) {
		this.failOnErrorStatus = failOnErrorStatus;
		return this;
	}

	@Override
	public Mono<Web2SocketExchange<A>> webSocket(String subProtocol) {
		ServiceID serviceId = ServiceID.of(this.request.getUri());
		return this.discoveryService.resolve(serviceId)
			.switchIfEmpty(Mono.error(() -> new ServiceNotFoundException(serviceId)))
			.flatMap(service -> service.getInstance(unboundExchange))
			.switchIfEmpty(Mono.error(() -> new ServiceNotFoundException(serviceId, "No matching service instance could be found")))
			.map(serviceInstance -> {
				if (this.interceptorResolver != null) {
					this.unboundExchange.intercept(interceptedExchange -> {
						GenericInterceptedWebExchange<A> webInterceptedExchange = new GenericInterceptedWebExchange<>(GenericWebExchange.this.dataConversionService, GenericWebExchange.this.request.getUri(), interceptedExchange);
						return this.interceptorResolver.apply(this)
							.intercept(webInterceptedExchange)
							.map(ign -> interceptedExchange)
							.doOnSuccess(ign -> this.errorMapper = webInterceptedExchange.getErrorMapper() != null ? webInterceptedExchange.getErrorMapper() : BASIC_ERROR_MAPPER);
					});
				}
				return serviceInstance.bind(this.unboundExchange);
			})
			.flatMap(exchange -> exchange.webSocket(subProtocol))
			.map(wsExchange -> new GenericWeb2SocketExchange<>(this.dataConversionService, wsExchange));
	}

	@Override
	public HttpVersion getProtocol() {
		return this.unboundExchange.getProtocol();
	}

	@Override
	public A context() {
		return this.unboundExchange.context();
	}

	@Override
	public WebRequest request() {
		return this.request;
	}

	@Override
	public Mono<WebResponse> response() {
		ServiceID serviceId = ServiceID.of(this.request.getUri());
		return this.discoveryService.resolve(serviceId)
			.switchIfEmpty(Mono.error(() -> new ServiceNotFoundException(serviceId)))
			.flatMap(service -> service.getInstance(unboundExchange))
			.map(serviceInstance -> {
				if(this.interceptorResolver != null) {
					this.unboundExchange.intercept(interceptedExchange -> {
						GenericInterceptedWebExchange<A> webInterceptedExchange = new GenericInterceptedWebExchange<>(GenericWebExchange.this.dataConversionService, GenericWebExchange.this.request.getUri(), interceptedExchange);
						return this.interceptorResolver.apply(this)
							.intercept(webInterceptedExchange)
							.map(ign -> interceptedExchange)
							.doOnSuccess(ign -> this.errorMapper = webInterceptedExchange.getErrorMapper() != null ? webInterceptedExchange.getErrorMapper() : BASIC_ERROR_MAPPER);
					});
				}
				return serviceInstance.bind(this.unboundExchange);
			})
			.flatMap(Exchange::response)
			.flatMap(response -> {
				GenericWebResponse webResponse = new GenericWebResponse(this.dataConversionService, response);
				if(this.failOnErrorStatus && webResponse.headers().getStatus().getCode() >= 400) {
					return this.errorMapper.apply(webResponse).thenReturn(webResponse);
				}
				return Mono.just(webResponse);
			});
	}

	@Override
	public void reset(long code) {
		this.unboundExchange.reset(code);
	}

	@Override
	public Optional<Throwable> getCancelCause() {
		return this.unboundExchange.getCancelCause();
	}
}
