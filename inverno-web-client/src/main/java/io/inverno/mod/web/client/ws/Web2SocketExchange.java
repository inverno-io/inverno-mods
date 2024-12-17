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
package io.inverno.mod.web.client.ws;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.client.ws.WebSocketExchange;
import io.inverno.mod.web.base.ws.BaseWeb2SocketExchange;

/**
 * <p>
 * A WebSocket exchange.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the exchange context type
 */
public interface Web2SocketExchange<A extends ExchangeContext> extends WebSocketExchange<A>, BaseWeb2SocketExchange<A> {

	/**
	 * <p>
	 * A WebSocket exchange configurer.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 */
	interface Configurer<A extends ExchangeContext> {

		/**
		 * <p>
		 * Configures the specified WebSocket exchange.
		 * </p>
		 *
		 * @param exchange a WebSocket exchange
		 */
		void configure(Web2SocketExchange<? extends A> exchange);
	}
}
