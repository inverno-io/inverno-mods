/*
 * Copyright 2021 Jeremy Kuhn
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
package io.inverno.mod.web.server;

import io.inverno.mod.base.net.URIPattern;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ws.WebSocket;
import io.inverno.mod.web.server.ws.Web2SocketExchange;
import java.util.Optional;

/**
 * <p>
 * An exchange that extends HTTP server {@link Exchange} with features for the Web.
 * </p>
 *
 * <p>
 * It supports request body decoding based on the request content type as well as response body encoding based on the response content type.
 * </p>
 *
 * <p>
 * It gives access to path parameters when processed in a route defined with a {@link URIPattern} and exposes a context used to propagate contextual information throughout the processing of the
 * exchange.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @param <A> the type of the exchange context
 */
public interface WebExchange<A extends ExchangeContext> extends Exchange<A> {

	@Override
	WebRequest request();

	@Override
	WebResponse response();

	@Override
	Optional<? extends WebSocket<A, Web2SocketExchange<A>>> webSocket(String... subprotocols);
}
