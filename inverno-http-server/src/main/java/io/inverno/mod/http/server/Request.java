/*
 * Copyright 2020 Jeremy KUHN
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

import io.inverno.mod.http.base.BaseRequest;
import java.util.Optional;

/**
 * <p>
 * Represents a client request in a server exchange.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Exchange
 */
public interface Request extends BaseRequest {
	
	/**
	 * <p>
	 * Returns the request body used to consume request payload.
	 * </p>
	 *
	 * @return an optional returning the request body or an empty optional if the request has no payload
	 */
	Optional<? extends RequestBody> body();
}
