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
import io.inverno.mod.web.compiler.spi.ResponseBodyKind;
import io.inverno.mod.web.compiler.spi.ResponseBodyReactiveKind;
import io.inverno.mod.web.compiler.spi.client.WebClientResponseBodyInfo;
import javax.lang.model.type.TypeMirror;

/**
 * <p>
 * Generic {@link WebClientResponseBodyInfo} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class GenericWebClientResponseBodyInfo extends AbstractWebClientRouteReturnInfo implements WebClientResponseBodyInfo {

	private final TypeMirror type;
	private final ResponseBodyKind responseBodyKind;
	private final ResponseBodyReactiveKind responseBodyReactiveKind;

	/**
	 * <p>
	 * Creates a generic Web client response body info.
	 * </p>
	 *
	 * @param reporter                 a reporter info
	 * @param type                     a type
	 * @param responseBodyKind         a response body kind
	 * @param responseBodyReactiveKind a response body reactive kind
	 */
	public GenericWebClientResponseBodyInfo(ReporterInfo reporter, TypeMirror type, ResponseBodyKind responseBodyKind, ResponseBodyReactiveKind responseBodyReactiveKind) {
		super(reporter);
		this.type = type;
		this.responseBodyKind = responseBodyKind;
		this.responseBodyReactiveKind = responseBodyReactiveKind;
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
}
