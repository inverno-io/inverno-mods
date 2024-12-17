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
package io.inverno.mod.web.server.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.ReactiveExchangeHandler;
import io.inverno.mod.web.server.ErrorWebExchange;

/**
 * <p>
 * Error Web route handler used as a resource in the {@link io.inverno.mod.web.server.internal.router.InternalErrorWebRouter}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see io.inverno.mod.web.server.internal.router.InternalErrorWebRouter
 *
 * @param <A> the exchange context type
 */
public class ErrorWebRouteHandler<A extends ExchangeContext> extends AbstractWebRouteHandler<A, ErrorWebExchange<A>> {

	/**
	 * <p>
	 * Creates an error Web route handler.
	 * </p>
	 *
	 * @param handler an error Web exchange handler
	 */
	public ErrorWebRouteHandler(ReactiveExchangeHandler<A, ErrorWebExchange<A>> handler) {
		super(handler);
	}
}
