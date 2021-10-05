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

import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.mod.web.compiler.spi.WebExchangeContextParameterInfo;
import io.inverno.mod.web.compiler.spi.WebParameterQualifiedName;

/**
 * <p>
 * Generic {@link WebExchangeContextParameterInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 * 
 * @see AbstractWebParameterInfo
 */
class GenericWebExchangeContextParameterInfo extends AbstractWebParameterInfo implements WebExchangeContextParameterInfo {

	/**
	 * <p>
	 * Creates a generic web exchange context parameter info.
	 * </p>
	 * 
	 * @param name             the parameter qualified name
	 * @param reporter         the parameter reporter
	 * @param parameterElement the parameter element
	 */
	public GenericWebExchangeContextParameterInfo(WebParameterQualifiedName name, ReporterInfo reporter, VariableElement parameterElement) {
		super(name, reporter, parameterElement, parameterElement.asType(), true);
	}
}
