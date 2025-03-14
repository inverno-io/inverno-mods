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

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.mod.web.compiler.spi.WebHeaderParameterInfo;
import io.inverno.mod.web.compiler.spi.WebParameterQualifiedName;

/**
 * <p>
 * Generic {@link WebHeaderParameterInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AbstractWebParameterInfo
 */
public class GenericWebHeaderParameterInfo extends AbstractWebParameterInfo implements WebHeaderParameterInfo {

	/**
	 * <p>
	 * Creates a generic Web form parameter info.
	 * </p>
	 *
	 * @param name             the parameter qualified name
	 * @param reporter         the parameter reporter
	 * @param parameterElement the parameter element
	 * @param type             the parameter type
	 * @param required         true to indicate a required parameter, false otherwise
	 */
	public GenericWebHeaderParameterInfo(WebParameterQualifiedName name, ReporterInfo reporter,	VariableElement parameterElement, TypeMirror type, boolean required) {
		super(name, reporter, parameterElement, type, required);
	}
}
