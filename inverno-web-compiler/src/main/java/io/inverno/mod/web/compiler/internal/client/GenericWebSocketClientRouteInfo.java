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
package io.inverno.mod.web.compiler.internal.client;

import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.ws.WebSocketMessage;
import io.inverno.mod.web.compiler.internal.AbstractWebParameterInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientRouteQualifiedName;
import io.inverno.mod.web.compiler.spi.client.WebClientRouteReturnInfo;
import io.inverno.mod.web.compiler.spi.client.WebSocketClientRouteInfo;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.ExecutableType;

/**
 * <p>
 * Generic {@link WebSocketClientRouteInfo} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class GenericWebSocketClientRouteInfo extends GenericWebClientRouteInfo implements WebSocketClientRouteInfo {

	private final String subprotocol;
	private final WebSocketMessage.Kind messageType;
	private final boolean closeOnComplete;

	/**
	 * <p>
	 * Creates a generic WebSocket client route info.
	 * </p>
	 *
	 * @param element         the route element
	 * @param routeType       the route type
	 * @param name            the route qualified name
	 * @param reporter        the reporter info
	 * @param path            the path
	 * @param languages       the accepted languages
	 * @param subprotocol     the subprotocol
	 * @param messageType     the message type
	 * @param parameters      the route parameters
	 * @param routeReturn     the route return info
	 * @param closeOnComplete the closeOnComplete flag
	 */
	public GenericWebSocketClientRouteInfo(
			ExecutableElement element,
			ExecutableType routeType,
			WebClientRouteQualifiedName name,
			ReporterInfo reporter,
			String path,
			Set<String> languages,
			String subprotocol,
			WebSocketMessage.Kind messageType,
			List<? extends AbstractWebParameterInfo> parameters,
			WebClientRouteReturnInfo routeReturn,
			boolean closeOnComplete
		) {
		super(element, routeType, name, reporter, path, Method.GET, Set.of(), null, languages, parameters, routeReturn);

		this.subprotocol = subprotocol;
		this.messageType = messageType;
		this.closeOnComplete = closeOnComplete;
	}

	@Override
	public WebSocketMessage.Kind getMessageType() {
		return this.messageType;
	}

	@Override
	public String getSubprotocol() {
		return this.subprotocol;
	}

	@Override
	public boolean isCloseOnComplete() {
		return this.closeOnComplete;
	}
}
