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
package io.inverno.mod.http.server;

/**
 * <p>
 * A root exchange handler must be provided to the server to handle exchanges.
 * </p>
 * 
 * <p>
 * The HTTP server relies on a root exchange handler to actually process a
 * client request and provide a response to the client. It will create an
 * {@link ErrorExchange} and invoke the {@link ErrorExchangeHandler} in case an
 * error is thrown during that process.
 * </p>
 * 
 * <p>
 * The HTTP server shall only rely on the {@link #defer(Exchange)} method in
 * order to remain reactive, the root exchange handler only extends
 * {@link ExchangeHandler} to facilitate the definition of the handler using
 * lambda.
 * </p>
 * 
 * <p>
 * It differs from a regular {@link ExchangeHandler} by the definition of the
 * {@link #createContext()} method which is used by the server to create the
 * exchange context associated to an {@link Exchange}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.33
 * 
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the handler
 */
public interface RootExchangeHandler<A extends ExchangeContext, B extends Exchange<A>> extends ExchangeHandler<A, B> {
	
	/**
	 * <p>
	 * Creates an exchange context eventually attached to an exchange.
	 * </p>
	 * 
	 * <p>
	 * This method returns null by default.
	 * </p>
	 * 
	 * @return a new context instance or null
	 */
	default A createContext() {
		return null;
	}
}
