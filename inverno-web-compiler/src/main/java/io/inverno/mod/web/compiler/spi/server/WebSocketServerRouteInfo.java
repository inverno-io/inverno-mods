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
package io.inverno.mod.web.compiler.spi.server;

import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.ws.WebSocketMessage;
import java.util.Optional;

/**
 * <p>
 * Describes a WebSocket route.
 * </p>
 * 
 * <p>
 * A WebSocket route is a {@link WebServerRouteInfo} with hardcoded {@code GET} method, no {@code consumes}, no {@code produces} and no {@code responseBody}. It specifies a WebSocket message type, a list of
 * supported subprotocols and an optional outbound publisher.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface WebSocketServerRouteInfo extends WebServerRouteInfo {

	@Override
	default Method[] getMethods() {
		return new Method[] { Method.GET };
	}

	@Override
	default String[] getConsumes() {
		return new String[0];
	}

	@Override
	default String[] getProduces() {
		return new String[0];
	}

	@Override
	default WebServerResponseBodyInfo getResponseBody() {
		return null;
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
	 * Returns the list of WebSocket subprotocols supported in the route.
	 * </p>
	 * 
	 * @return an array of WebSocket subprotocols
	 */
	String[] getSubprotocols();
	
	/**
	 * <p>
	 * Returns the outbound publisher specified in the route if any.
	 * </p>
	 * 
	 * @return an optional returning an outbound publisher info or an empty optional if the route was not defined with an outbound publisher
	 */
	Optional<WebSocketServerOutboundPublisherInfo> getOutboundPublisher();

	/**
	 * <p>
	 * Determines whether the WebSocket should be closed when the outbound publisher completes.
	 * </p>
	 *
	 * @return true to close on outbound complete, false otherwise
	 */
	boolean isCloseOnComplete();
}
