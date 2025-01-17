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
import io.inverno.mod.web.server.annotation.WebRoutes;
import io.inverno.mod.web.compiler.spi.server.WebServerRouteQualifiedName;
import io.inverno.mod.web.compiler.spi.server.WebSocketServerOutboundPublisherInfo;
import io.inverno.mod.web.compiler.spi.server.WebSocketServerRouteInfo;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * Provided {@link WebSocketServerRouteInfo} implementation used to describes WebSocket routes specified in a {@link WebRoutes} annotation on a Web routes configurer or a Web router.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class ProvidedWebSocketServerRouteInfo extends ProvidedWebServerRouteInfo implements WebSocketServerRouteInfo {
	
	private final WebSocketMessage.Kind messageType;
	
	private final String[] subprotocols;

	/**
	 * <p>
	 * Creates a provided WebSocket server route info.
	 * </p>
	 * 
	 * @param name               the route qualified name
	 * @param reporter           the route reporter
	 * @param paths              the route paths
	 * @param matchTrailingSlash true to match trailing slash, false otherwise
	 * @param languages          the route produced languages
	 * @param subprotocols       the route WebSocket subprotocols
	 * @param messageType        the route WebSocket message type
	 */
	public ProvidedWebSocketServerRouteInfo(WebServerRouteQualifiedName name, ReporterInfo reporter, Set<String> paths, boolean matchTrailingSlash, Set<String> languages, Set<String> subprotocols, WebSocketMessage.Kind messageType) {
		super(name, reporter, paths, matchTrailingSlash, Set.of(Method.GET), Set.of(), Set.of(), languages);
		this.subprotocols = subprotocols.stream().sorted().toArray(String[]::new);
		this.messageType = messageType;
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
		return Optional.empty();
	}

	@Override
	public boolean isCloseOnComplete() {
		return true;
	}
}
