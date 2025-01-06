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
package io.inverno.mod.discovery.http.internal;

import io.inverno.mod.discovery.http.HttpServiceInstance;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.client.Endpoint;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.UnboundExchange;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A generic {@link HttpServiceInstance} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class GenericHttpServiceInstance implements HttpServiceInstance {

	private final Endpoint<?> endpoint;

	private final int weight;

	/**
	 * <p>
	 * Creates an HTTP service instance.
	 * </p>
	 *
	 * @param endpoint the HTTP client endpoint
	 */
	public GenericHttpServiceInstance(Endpoint<?> endpoint) {
		this(endpoint, 1);
	}

	/**
	 * <p>
	 * Creates an HTTP service instance with the specified weight.
	 * </p>
	 *
	 * @param endpoint the HTTP client endpoint
	 * @param weight   the service instance weight
	 */
	public GenericHttpServiceInstance(Endpoint<?> endpoint, int weight) {
		this.endpoint = endpoint;
		this.weight = weight;
		if(weight <= 0) {
			throw new IllegalArgumentException("weight must be a positive integer");
		}
	}

	@Override
	public int getWeight() {
		return this.weight;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends ExchangeContext> Exchange<T> bind(UnboundExchange<T> exchange) throws IllegalStateException {
		return exchange.bind((Endpoint<? super T>) this.endpoint);
	}

	@Override
	public long getActiveRequests() {
		return this.endpoint.getActiveRequests();
	}

	@Override
	public float getLoadFactor() {
		return this.endpoint.getLoadFactor();
	}

	@Override
	public Mono<Void> shutdown() {
		return this.endpoint.shutdown();
	}

	@Override
	public Mono<Void> shutdownGracefully() {
		return this.endpoint.shutdownGracefully();
	}
}