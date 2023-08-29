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
package io.inverno.mod.web;

import io.inverno.mod.base.net.URIPattern;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ws.WebSocket;
import java.util.Optional;

/**
 * <p>
 * An exchange that extends the HTTP server {@link Exchange} with features for the Web.
 * </p>
 *
 * <p>
 * It supports request body decoding based on the request content type as well as response body encoding based on the response content type.
 * </p>
 *
 * <p>
 * It also gives access to path parameters when processed in a route defined with a {@link URIPattern}.
 * </p>
 *
 * <p>
 * It also exposes a context which can be used to propagate information in a chain of exchange handlers. The {@link WebRouter} uses {@link WebServerControllerConfigurer#createContext()} to create the
 * context attached to the Web exchange.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see WebRoute
 * @see WebRouteManager
 * @see WebRouter
 *
 * @param <A> the type of the exchange context
 */
public interface WebExchange<A extends ExchangeContext> extends Exchange<A> {

	@Override
	WebRequest request();
	
	@Override
	WebResponse response();
	
	@Override
	public Optional<? extends WebSocket<A, Web2SocketExchange<A>>> webSocket(String... subProtocols);
}
