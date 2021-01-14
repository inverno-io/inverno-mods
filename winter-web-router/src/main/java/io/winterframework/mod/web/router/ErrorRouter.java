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
package io.winterframework.mod.web.router;

import io.winterframework.mod.web.WebException;
import io.winterframework.mod.web.server.ErrorExchange;

/**
 * @author jkuhn
 *
 */
public interface ErrorRouter extends AbstractRouter<ErrorExchange<Throwable>, ErrorRouter, ErrorRouteManager, ErrorRoute, ErrorExchange<Throwable>> {
	
	@Override
	default void handle(ErrorExchange<Throwable> exchange) throws WebException {
		if(exchange.response().isHeadersWritten()) {
			throw new IllegalStateException("Headers already written", exchange.getError());
		}
	}
}
