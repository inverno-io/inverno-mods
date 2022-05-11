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
 * Base Exchange context interface.
 * </p>
 *
 * <p>
 * An exchange context is attached to an exchange to expose contextual data and operation during the lifetime of an exchange. It is created by the HTTP server with the {@link Exchange} using
 * {@link ServerController#createContext()} at the earliest possible moment basically when a request is received.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 *
 * @see Exchange
 * @see ServerController#createContext()
 */
public interface ExchangeContext {
	
	/**
	 * <p>
	 * Initializes the exchange context.
	 * </p>
	 * 
	 * <p>
	 * This method is invoked by the HTTP server right after creation.
	 * </p>
	 */
	default void init() {
		
	}
}
