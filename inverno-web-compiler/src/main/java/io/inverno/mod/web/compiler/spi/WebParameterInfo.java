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
package io.inverno.mod.web.compiler.spi;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import io.inverno.core.compiler.spi.Info;

/**
 * <p>
 * Common web route parameter information.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see WebRouteInfo
 */
public interface WebParameterInfo extends Info {
	
	@Override
	WebParameterQualifiedName getQualifiedName();
	
	/**
	 * <p>
	 * Returns the web parameter element.
	 * </p>
	 * 
	 * @return a variable element
	 */
	VariableElement getElement();
	
	/**
	 * <p>
	 * Determines whether the parameter is required.
	 * </p>
	 * 
	 * @return true if the parameter is required, false otherwise
	 */
	boolean isRequired();
	
	/**
	 * <p>
	 * Returns the type of the parameter.
	 * </p>
	 * 
	 * @return the parameter type
	 */
	TypeMirror getType();
}
