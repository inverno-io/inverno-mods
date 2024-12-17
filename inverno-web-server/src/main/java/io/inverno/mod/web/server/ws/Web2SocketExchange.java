/*
 * Copyright 2022 Jeremy Kuhn
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
package io.inverno.mod.web.server.ws;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.ws.WebSocketExchange;
import io.inverno.mod.web.base.ws.BaseWeb2SocketExchange;
import io.inverno.mod.web.server.WebRequest;

/**
 * <p>
 * A WebSocket exchange that extends the HTTP server {@link WebSocketExchange} with features for the Web.
 * </p>
 *
 * <p>
 * It supports inbound and outbound message decoding and encoding based on the negotiated subProtocol which is interpreted as a compact application media types (see
 * {@link io.inverno.mod.base.resource.MediaTypes#normalizeApplicationMediaType(java.lang.String)}).
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 *
 * @param <A> the exchange context type
 */
public interface Web2SocketExchange<A extends ExchangeContext> extends WebSocketExchange<A>, BaseWeb2SocketExchange<A> {

	@Override
	WebRequest request();
}
