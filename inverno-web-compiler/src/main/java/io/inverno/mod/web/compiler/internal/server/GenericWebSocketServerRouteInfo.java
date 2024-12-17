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
package io.inverno.mod.web.compiler.internal.server;

import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.ws.WebSocketMessage;
import io.inverno.mod.web.compiler.internal.AbstractWebParameterInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerRouteQualifiedName;
import io.inverno.mod.web.compiler.spi.server.WebSocketServerOutboundPublisherInfo;
import io.inverno.mod.web.compiler.spi.server.WebSocketServerRouteInfo;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.ExecutableType;

/**
 * <p>
 * Generic {@link WebSocketServerRouteInfo} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericWebSocketServerRouteInfo extends GenericWebServerRouteInfo implements WebSocketServerRouteInfo {
	
	private final WebSocketMessage.Kind messageType;
	
	private final String[] subprotocols;
	
	private final WebSocketServerOutboundPublisherInfo outboundPublisher;

	private final boolean closeOnComplete;

	/**
	 * <p>
	 * Creates a generic WebSocket server route info.
	 * </p>
	 *
	 * @param element            the executable element of the route
	 * @param routeType          the executable type of the route as member of the Web controller interface
	 * @param name               the route qualified name
	 * @param reporter           the route reporter
	 * @param paths              the route paths
	 * @param matchTrailingSlash true to match trailing slash, false otherwise
	 * @param languages          the route produced languages
	 * @param subprotocols       the route WebSocket subprotocols
	 * @param messageType        the route WebSocket message type
	 * @param parameters         the route parameter info
	 * @param outboundPublisher  the route WebSocket outbound publisher
	 * @param closeOnComplete    indicate whether to close the WebSocket when outbound completes
	 */
	public GenericWebSocketServerRouteInfo(
			ExecutableElement element,
			ExecutableType routeType,
			WebServerRouteQualifiedName name,
			ReporterInfo reporter, 
			Set<String> paths, 
			boolean matchTrailingSlash, 
			Set<String> languages, 
			Set<String> subprotocols,
			WebSocketMessage.Kind messageType,
			List<? extends AbstractWebParameterInfo> parameters,
			WebSocketServerOutboundPublisherInfo outboundPublisher,
			boolean closeOnComplete) {
		super(element, routeType, name, reporter, paths, matchTrailingSlash, Set.of(Method.GET), Set.of(), Set.of(), languages, parameters, null);
		this.messageType = messageType;
		this.subprotocols = subprotocols.stream().sorted().toArray(String[]::new);
		this.outboundPublisher = outboundPublisher;
		this.closeOnComplete = closeOnComplete;
	}

	@Override
	public WebSocketMessage.Kind getMessageType() {
		return this.messageType;
	}
	
	@Override
	public String[] getSubprotocols() {
		return this.subprotocols;
	}

	@Override
	public Optional<WebSocketServerOutboundPublisherInfo> getOutboundPublisher() {
		return Optional.ofNullable(this.outboundPublisher);
	}

	@Override
	public boolean isCloseOnComplete() {
		return this.closeOnComplete;
	}
}
