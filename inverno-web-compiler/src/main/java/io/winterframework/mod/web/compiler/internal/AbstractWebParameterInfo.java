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

import java.util.Objects;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.core.compiler.spi.support.AbstractInfo;
import io.inverno.mod.web.compiler.spi.WebParameterInfo;
import io.inverno.mod.web.compiler.spi.WebParameterQualifiedName;

/**
 * <p>
 * Base {@link WebParameterInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AbstractInfo
 */
abstract class AbstractWebParameterInfo extends AbstractInfo<WebParameterQualifiedName> implements WebParameterInfo {

	private final VariableElement element;

	private final TypeMirror type;
	
	private final boolean required;
	
	/**
	 * <p>
	 * Creates an web parameter info.
	 * </p>
	 * 
	 * @param name             the parameter qualified name
	 * @param reporter         the parameter reporter
	 * @param parameterElement the parameter element
	 * @param type             the parameter type
	 * @param required         true to indicate a required parameter, false
	 *                         otherwise
	 */
	public AbstractWebParameterInfo(WebParameterQualifiedName name, ReporterInfo reporter, VariableElement parameterElement, TypeMirror type, boolean required) {
		super(name, reporter instanceof NoOpReporterInfo ? ((NoOpReporterInfo)reporter).getReporter() : reporter);
		this.element = Objects.requireNonNull(parameterElement);
		this.type = Objects.requireNonNull(type);
		this.required = required;
	}

	@Override
	public VariableElement getElement() {
		return this.element;
	}

	@Override
	public TypeMirror getType() {
		return this.type;
	}
	
	@Override
	public boolean isRequired() {
		return this.required;
	}
}