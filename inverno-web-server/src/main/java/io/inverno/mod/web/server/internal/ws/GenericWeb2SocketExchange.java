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
package io.inverno.mod.web.server.internal.ws;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.ws.WebSocketExchange;
import io.inverno.mod.web.base.ws.BaseWeb2SocketExchange;
import io.inverno.mod.web.server.WebRequest;
import io.inverno.mod.web.server.internal.ServerDataConversionService;
import io.inverno.mod.web.server.ws.Web2SocketExchange;

/**
 * <p>
 * Generic {@link Web2SocketExchange} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericWeb2SocketExchange<A extends ExchangeContext> implements Web2SocketExchange<A> {

	private final ServerDataConversionService dataConversionService;
	private final WebRequest request;
	private final WebSocketExchange<A> webSocketExchange;

	/**
	 * <p>
	 * Creates a generic Web2Socket exchange.
	 * </p>
	 *
	 * @param dataConversionService the data conversion service
	 * @param request               the originating Web request
	 * @param webSocketExchange     the originating WebSocket exchange
	 */
	public GenericWeb2SocketExchange(ServerDataConversionService dataConversionService, WebRequest request, WebSocketExchange<A> webSocketExchange) {
		this.dataConversionService = dataConversionService;
		this.request = request;
		this.webSocketExchange = webSocketExchange;
	}

	@Override
	public WebRequest request() {
		return this.request;
	}

	@Override
	public A context() {
		return this.webSocketExchange.context();
	}

	@Override
	public String getSubprotocol() {
		return this.webSocketExchange.getSubprotocol();
	}

	@Override
	public BaseWeb2SocketExchange.Inbound inbound() {
		return this.dataConversionService.createWebSocketDecodingInbound(this.webSocketExchange.inbound(), this.webSocketExchange.getSubprotocol());
	}

	@Override
	public BaseWeb2SocketExchange.Outbound outbound() {
		return this.dataConversionService.createWebSocketEncodingOutbound(this.webSocketExchange.outbound(), this.webSocketExchange.getSubprotocol());
	}

	@Override
	public void close(short code, String reason) {
		this.webSocketExchange.close(code, reason);
	}
}
