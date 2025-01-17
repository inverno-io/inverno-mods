/*
 * Copyright 2021 Jeremy KUHN
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
import io.inverno.mod.web.compiler.spi.ResponseBodyKind;
import io.inverno.mod.web.compiler.spi.ResponseBodyReactiveKind;
import java.util.Objects;

import javax.lang.model.type.TypeMirror;

import io.inverno.mod.web.compiler.spi.server.WebServerResponseBodyInfo;

/**
 * <p>
 * Generic {@link WebServerResponseBodyInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class GenericWebServerResponseBodyInfo implements WebServerResponseBodyInfo {

	private final ReporterInfo reporter;
	private final TypeMirror type;
	private final ResponseBodyKind responseBodyKind;
	private final ResponseBodyReactiveKind responseBodyReactiveKind;
	
	/**
	 * <p>
	 * Creates a generic Web server response body info.
	 * </p>
	 *
	 * @param reporter                 a reporter info
	 * @param type                     the actual type of the response body
	 * @param responseBodyKind         the response body kind
	 * @param responseBodyReactiveKind the response body reactive kind
	 */
	public GenericWebServerResponseBodyInfo(ReporterInfo reporter, TypeMirror type, ResponseBodyKind responseBodyKind, ResponseBodyReactiveKind responseBodyReactiveKind) {
		this.reporter = reporter;
		this.type = Objects.requireNonNull(type);
		this.responseBodyKind = Objects.requireNonNull(responseBodyKind);
		this.responseBodyReactiveKind = Objects.requireNonNull(responseBodyReactiveKind);
	}

	@Override
	public TypeMirror getType() {
		return this.type;
	}

	@Override
	public ResponseBodyKind getBodyKind() {
		return this.responseBodyKind;
	}

	@Override
	public ResponseBodyReactiveKind getBodyReactiveKind() {
		return this.responseBodyReactiveKind;
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
		this.reporter.hasWarning();
	}
}
