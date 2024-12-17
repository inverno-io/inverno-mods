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
package io.inverno.mod.http.client;

import io.inverno.mod.http.base.ExchangeContext;

/**
 * <p>
 * An {@link Exchange} which is not bound to any {@link Endpoint}.
 * </p>
 *
 * <p>
 * An unbound exchange must be explicitly bound to an actual endpoint before it can be sent, {@link #bind(Endpoint)} must be invoked before publishers returned by {@link #response()} or
 * {@link #webSocket()} are subscribed.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public interface UnboundExchange<A extends ExchangeContext> extends Exchange<A> {

	/**
	 * <p>
	 * Specifies an interceptor to intercept the exchange before it is sent to the endpoint.
	 * </p>
	 *
	 * <p>
	 * When invoked multiple time this method chains the interceptors one after the other. Interceptors specified on the exchange are executed after the ones defined on the endpoint processing the
	 * exchange.
	 * </p>
	 *
	 * @param interceptor an exchange interceptor
	 *
	 * @return the unbounded exchange
	 */
	UnboundExchange<A> intercept(ExchangeInterceptor<? super A, ? extends InterceptedExchange<A>> interceptor);

	/**
	 * <p>
	 * Binds the exchange to the specified endpoint.
	 * </p>
	 *
	 * <p>
	 * An unbound exchange is created from the {@link HttpClient}, it must be bound to an actual endpoint in order to be sent.
	 * </p>
	 *
	 * <p>
	 * Note that an exchange is stateful and as such it can't be bound multiple times to different endpoints.
	 * </p>
	 *
	 * @param endpoint the exchange to bind
	 *
	 * @return the bound exchange
	 *
	 * @throws IllegalArgumentException if the specified endpoint hasn't been obtained from the {@link HttpClient} bean
	 * @throws IllegalStateException    if the exchange is already bound
	 */
	Exchange<A> bind(Endpoint<? super A> endpoint) throws IllegalArgumentException, IllegalStateException;

}
