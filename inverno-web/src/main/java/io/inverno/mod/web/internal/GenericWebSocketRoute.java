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

import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ReactiveExchangeHandler;
import io.inverno.mod.http.server.ws.WebSocketExchangeHandler;
import io.inverno.mod.web.Web2SocketExchange;
import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.WebSocketRoute;
import java.util.Objects;

/**
 * <p>
 * Generic {@link WebSocketRoute} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
class GenericWebSocketRoute extends GenericWebRoute implements WebSocketRoute<ExchangeContext> {

	private String subProtocol;
	
	private WebSocketExchangeHandler<ExchangeContext, Web2SocketExchange<ExchangeContext>> webSocketHandler;
	
	/**
	 * <p>
	 * Creates a generic WebSocket route in the specified generic web router.
	 * </p>
	 * 
	 * @param router a generic web router
	 */
	public GenericWebSocketRoute(AbstractWebRouter router) {
		super(router);
	}

	@Override
	public Method getMethod() {
		return Method.GET;
	}

	@Override
	public String getConsume() {
		return null;
	}

	@Override
	public String getProduce() {
		return null;
	}

	@Override
	public String getSubProtocol() {
		return this.subProtocol;
	}

	public void setSubProtocol(String subProtocol) {
		this.subProtocol = subProtocol;
	}

	@Override
	public ReactiveExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> getHandler() {
		return null;
	}
	
	@Override
	public WebSocketExchangeHandler<ExchangeContext, Web2SocketExchange<ExchangeContext>> getWebSocketHandler() {
		return this.webSocketHandler;
	}

	public void setWebSocketHandler(WebSocketExchangeHandler<ExchangeContext, Web2SocketExchange<ExchangeContext>> webSocketHandler) {
		this.webSocketHandler = webSocketHandler;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(subProtocol);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		GenericWebSocketRoute other = (GenericWebSocketRoute) obj;
		return Objects.equals(subProtocol, other.subProtocol);
	}
}
