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
package io.inverno.mod.irt.compiler.spi;

import javax.lang.model.type.TypeMirror;

/**
 * <p>
 * A parameter info corresponds to a parameter in a template declaration in a
 * template set source file.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 */
public interface ParameterInfo {

	/**
	 * <p>
	 * Returns the name of the parameter which is a valid Java identifier.
	 * </p>
	 * 
	 * @return a name
	 */
	String getName();
	
	/**
	 * <p>
	 * Returns the type of the parameter.
	 * </p>
	 * 
	 * @return a type
	 */
	TypeMirror getType();
	
	/**
	 * <p>
	 * Returns the raw Java formal parameter declaration (ie. [TYPE] [NAME]).
	 * </p>
	 * 
	 * @return the raw Java formal parameter declaration
	 */
	String getValue();
}
