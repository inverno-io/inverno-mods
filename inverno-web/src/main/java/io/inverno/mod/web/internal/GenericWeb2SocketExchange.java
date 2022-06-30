/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.web.internal;

import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ws.WebSocketException;
import io.inverno.mod.http.server.ws.WebSocketExchange;
import io.inverno.mod.web.Web2SocketExchange;
import io.inverno.mod.web.WebRequest;
import reactor.core.publisher.Mono;
		
/**
 * <p>
 * Generic {@link Web2SocketExchange} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericWeb2SocketExchange implements Web2SocketExchange<ExchangeContext> {

	private final WebSocketExchange<ExchangeContext> webSocketExchange;
	
	private final WebRequest request;
	
	private final DataConversionService dataConversionService;

	/**
	 * <p>
	 * Creates a generic Web2Socket exchange.
	 * </p>
	 *
	 * @param webSocketExchange     the original Web exchange
	 * @param request               the Web request
	 * @param dataConversionService the data conversion service
	 */
	public GenericWeb2SocketExchange(WebSocketExchange<ExchangeContext> webSocketExchange, WebRequest request, DataConversionService dataConversionService) {
		this.webSocketExchange = webSocketExchange;
		this.request = request;
		this.dataConversionService = dataConversionService;
	}

	@Override
	public WebRequest request() {
		return this.request;
	}

	@Override
	public Web2SocketExchange<ExchangeContext> finalizer(Mono<Void> finalizer) {
		this.webSocketExchange.finalizer(finalizer);
		return this;
	}

	@Override
	public Web2SocketExchange.Inbound inbound() {
		try {
			return this.dataConversionService.createWebSocketDecodedInbound(this.webSocketExchange.inbound(), this.webSocketExchange.getSubProtocol());
		}
		catch(NoConverterException e) {
			throw new WebSocketException("No converter found for sub protocol: " + this.webSocketExchange.getSubProtocol(), e);
		}
	}

	@Override
	public Web2SocketExchange.Outbound outbound() {
		try {
			return this.dataConversionService.createWebSocketEncodedOutbound(this.webSocketExchange.outbound(), this.webSocketExchange.getSubProtocol());
		}
		catch(NoConverterException e) {
			throw new WebSocketException("No converter found for sub protocol: " + this.webSocketExchange.getSubProtocol(), e);
		}
	}

	@Override
	public String getSubProtocol() {
		return this.webSocketExchange.getSubProtocol();
	}

	@Override
	public void close(short code, String reason) {
		this.webSocketExchange.close(code, reason);
	}
}
