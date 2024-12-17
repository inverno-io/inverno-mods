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
import io.inverno.mod.web.compiler.spi.WebSocketBoundPublisherInfo;
import io.inverno.mod.web.compiler.spi.server.WebSocketServerOutboundPublisherInfo;
import javax.lang.model.type.TypeMirror;

/**
 * <p>
 * Generic {@link WebSocketServerOutboundPublisherInfo} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericWebSocketServerOutboundPublisherInfo implements WebSocketServerOutboundPublisherInfo {

	private final ReporterInfo reporter;

	private final TypeMirror type;
	
	private final WebSocketServerOutboundPublisherInfo.BoundKind boundKind;
	
	private final WebSocketServerOutboundPublisherInfo.BoundReactiveKind boundReactiveKind;

	/**
	 * <p>
	 * Creates a generic WebSocket server outbound publisher info.
	 * </p>
	 *
	 * @param reporter          the reporter
	 * @param type              the actual WebSocket message type
	 * @param boundKind         the outbound kind
	 * @param boundReactiveKind the outbound reactive kind
	 */
	public GenericWebSocketServerOutboundPublisherInfo(ReporterInfo reporter, TypeMirror type, WebSocketBoundPublisherInfo.BoundKind boundKind, WebSocketBoundPublisherInfo.BoundReactiveKind boundReactiveKind) {
		this.reporter = reporter;
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

	@Override
	public boolean hasError() {
		return this.reporter.hasError();
	}

	@Override
	public boolean hasWarning() {
		return this.reporter.hasWarning();
	}

	@Override
	public void error(String message) {
		this.reporter.error(message);
	}

	@Override
	public void warning(String message) {
		this.reporter.warning(message);
	}
}
