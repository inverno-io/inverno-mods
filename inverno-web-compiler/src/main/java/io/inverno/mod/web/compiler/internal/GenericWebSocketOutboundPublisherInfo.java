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
package io.inverno.mod.web.compiler.internal;

import io.inverno.mod.web.compiler.spi.WebSocketBoundPublisherInfo;
import io.inverno.mod.web.compiler.spi.WebSocketOutboundPublisherInfo;
import javax.lang.model.type.TypeMirror;

/**
 * <p>
 * Generic {@link WebSocketOutboundParameterInfo} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
class GenericWebSocketOutboundPublisherInfo implements WebSocketOutboundPublisherInfo {

	private final TypeMirror type;
	
	private final WebSocketOutboundPublisherInfo.BoundKind boundKind;
	
	private final WebSocketOutboundPublisherInfo.BoundReactiveKind boundReactiveKind;

	/**
	 * <p>
	 * Creates a generic Websocket outbound publisher info.
	 * </p>
	 *
	 * @param type              the actual WebSocket message type
	 * @param boundKind         the outbound kind
	 * @param boundReactiveKind the outbound reactive kind
	 */
	public GenericWebSocketOutboundPublisherInfo(TypeMirror type, WebSocketBoundPublisherInfo.BoundKind boundKind, WebSocketBoundPublisherInfo.BoundReactiveKind boundReactiveKind) {
		this.type = type;
		this.boundKind = boundKind;
		this.boundReactiveKind = boundReactiveKind;
	}
	
	@Override
	public TypeMirror getType() {
		return this.type;
	}

	@Override
	public BoundKind getBoundKind() {
		return this.boundKind;
	}
	
	@Override
	public BoundReactiveKind getBoundReactiveKind() {
		return this.boundReactiveKind;
	}
}
