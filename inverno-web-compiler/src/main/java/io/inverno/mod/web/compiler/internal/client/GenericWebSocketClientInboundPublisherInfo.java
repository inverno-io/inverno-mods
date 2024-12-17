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
import io.inverno.mod.web.compiler.spi.WebSocketBoundPublisherInfo;
import io.inverno.mod.web.compiler.spi.client.WebSocketClientInboundPublisherInfo;
import javax.lang.model.type.TypeMirror;

/**
 * <p>
 * Generic {@link WebSocketClientInboundPublisherInfo} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class GenericWebSocketClientInboundPublisherInfo extends AbstractWebClientRouteReturnInfo implements WebSocketClientInboundPublisherInfo {

	private final TypeMirror type;
	private final WebSocketBoundPublisherInfo.BoundKind boundKind;
	private final WebSocketBoundPublisherInfo.BoundReactiveKind boundReactiveKind;

	/**
	 * <p>
	 * Creates a generic WebSocket client inbound publisher info.
	 * </p>
	 *
	 * @param reporter          the reporter info
	 * @param type              the type
	 * @param boundKind         the bound kind
	 * @param boundReactiveKind the bound reactive kind
	 */
	public GenericWebSocketClientInboundPublisherInfo(ReporterInfo reporter, TypeMirror type, WebSocketBoundPublisherInfo.BoundKind boundKind, WebSocketBoundPublisherInfo.BoundReactiveKind boundReactiveKind) {
		super(reporter);
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
