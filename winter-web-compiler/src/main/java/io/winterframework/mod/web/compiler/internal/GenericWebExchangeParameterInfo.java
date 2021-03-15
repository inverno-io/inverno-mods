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
package io.winterframework.mod.web.compiler.internal;

import javax.lang.model.element.VariableElement;

import io.winterframework.core.compiler.spi.ReporterInfo;
import io.winterframework.mod.web.compiler.spi.WebExchangeParameterInfo;
import io.winterframework.mod.web.compiler.spi.WebParameterQualifiedName;

/**
 * <p>
 * Generic {@link WebExchangeParameterInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AbstractWebParameterInfo
 */
class GenericWebExchangeParameterInfo extends AbstractWebParameterInfo implements WebExchangeParameterInfo {

	/**
	 * <p>
	 * Creates a generic web exchange parameter info.
	 * </p>
	 * 
	 * @param name             the parameter qualified name
	 * @param reporter         the parameter reporter
	 * @param parameterElement the parameter element
	 */
	public GenericWebExchangeParameterInfo(WebParameterQualifiedName name, ReporterInfo reporter, VariableElement parameterElement) {
		super(name, reporter, parameterElement, parameterElement.asType(), true);
	}
}
