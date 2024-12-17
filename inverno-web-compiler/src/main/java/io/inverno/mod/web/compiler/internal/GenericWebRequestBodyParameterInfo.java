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
package io.inverno.mod.web.compiler.internal;

import io.inverno.mod.web.compiler.spi.RequestBodyKind;
import io.inverno.mod.web.compiler.spi.RequestBodyReactiveKind;
import java.util.Objects;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.mod.web.compiler.spi.WebParameterQualifiedName;
import io.inverno.mod.web.compiler.spi.WebRequestBodyParameterInfo;

/**
 * <p>
 * Generic {@link WebRequestBodyParameterInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AbstractWebParameterInfo
 */
public class GenericWebRequestBodyParameterInfo extends AbstractWebParameterInfo implements WebRequestBodyParameterInfo {

	private final RequestBodyKind requestBodyKind;
	
	private final RequestBodyReactiveKind requestBodyReactiveKind;
	
	/**
	 * <p>
	 * Creates a generic Web request body parameter info.
	 * </p>
	 * 
	 * @param name                    the parameter qualified name
	 * @param reporter                the parameter reporter
	 * @param parameterElement        the parameter element
	 * @param requestBodyType         the actual type of the request body
	 * @param requestBodyKind         the request body kind
	 * @param requestBodyReactiveKind the request body reactive kind
	 */
	public GenericWebRequestBodyParameterInfo(WebParameterQualifiedName name, ReporterInfo reporter, VariableElement parameterElement, TypeMirror requestBodyType, RequestBodyKind requestBodyKind, RequestBodyReactiveKind requestBodyReactiveKind) {
		super(name, reporter, parameterElement, requestBodyType, true);
		this.requestBodyKind = Objects.requireNonNull(requestBodyKind);
		this.requestBodyReactiveKind = Objects.requireNonNull(requestBodyReactiveKind);
	}

	@Override
	public RequestBodyReactiveKind getBodyReactiveKind() {
		return this.requestBodyReactiveKind;
	}
	
	@Override
	public RequestBodyKind getBodyKind() {
		return this.requestBodyKind;
	}
}
