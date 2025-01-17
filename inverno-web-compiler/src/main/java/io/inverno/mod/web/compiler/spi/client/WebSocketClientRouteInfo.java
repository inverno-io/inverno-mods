/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.web.compiler.spi.client;

import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.ws.WebSocketMessage;
import java.util.Optional;

/**
 * <p>
 * Describes a WebSocket client stub route.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public interface WebSocketClientRouteInfo extends WebClientRouteInfo {

	@Override
	default Method getMethod() {
		return Method.GET;
	}

	@Override
	default Optional<String> getProduce() {
		return Optional.empty();
	}

	@Override
	default String[] getConsumes() {
		return new String[0];
	}

	/**
	 * <p>
	 * Returns the WebSocket message type specified in the route.
	 * </p>
	 *
	 * <p>
	 * This is only relevant when the route is defined with inbound and outbound publishers.
	 * </p>
	 *
	 * @return the kind of WebSocket message
	 */
	WebSocketMessage.Kind getMessageType();

	/**
	 * <p>
	 * Returns the WebSocket subprotocol to negotiate in the route.
	 * </p>
	 *
	 * @return a subprotocol or null
	 */
	String getSubprotocol();

	/**
	 * <p>
	 * Determines whether the WebSocket should be closed when the outbound publisher completes.
	 * </p>
	 *
	 * @return true to close on outbound complete, false otherwise
	 */
	boolean isCloseOnComplete();
}
