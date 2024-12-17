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
package io.inverno.mod.discovery.http;

import io.inverno.mod.discovery.WeightedServiceInstance;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.client.Endpoint;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.UnboundExchange;

/**
 * <p>
 * An HTTP service instance using an HTTP client {@link Endpoint} to send HTTP requests to an HTTP server exposing a service.
 * </p>
 *
 * <p>
 * An exchange must be bound to a service instance, and therefore to the underlying endpoint, using {@link #bind(UnboundExchange)} to obtain an exchange that can be sent to the remote server by
 * subscribing to the response publisher.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public interface HttpServiceInstance extends WeightedServiceInstance {

	/**
	 * <p>
	 * Binds the exchange to the underlying HTTP {@link Endpoint} connecting to the service instance.
	 * </p>
	 *
	 * <p>
	 * An exchange is typically bound to a service instance right after a service instance has been resolved from a service using that same exchange (see
	 * {@link io.inverno.mod.discovery.Service#getInstance(Object)}). Binding multiple exchanges to a service instance bypassing the service impacts load balancing and must be avoided.
	 * </p>
	 *
	 * @param <T>      the exchange context type
	 * @param exchange an unbound exchange
	 *
	 * @return a bound exchange
	 *
	 * @throws IllegalStateException if the exchange is already bound
	 *
	 * @see UnboundExchange#bind(Endpoint)
	 */
	<T extends ExchangeContext> Exchange<T> bind(UnboundExchange<T> exchange) throws IllegalStateException;

	/**
	 * <p>
	 * Returns the number of active requests handled by this instance.
	 * </p>
	 *
	 * @return the number of active requests
	 *
	 * @see Endpoint#getActiveRequests()
	 */
	long getActiveRequests();

	/**
	 * <p>
	 * Returns the current load factor of the instance.
	 * </p>
	 *
	 * @return the load factor between 0 and 1
	 *
	 * @see Endpoint#getLoadFactor()
	 */
	float getLoadFactor();
}
