/*
 * Copyright 2020 Jeremy KUHN
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
package io.inverno.mod.configuration.compiler.spi;

import javax.lang.model.type.TypeMirror;

import io.inverno.core.compiler.spi.Info;

/**
 * <p>
 * A configuration property info describes a configuration property in a
 * configuration.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ConfigurationInfo
 */
public interface ConfigurationPropertyInfo extends Info {

	/**
	 * <p>
	 * Returns the qualified name of a configuration property defined a
	 * configuration.
	 * </p>
	 */
	@Override
	PropertyQualifiedName getQualifiedName();
	
	/**
	 * <p>
	 * Determines whether the property defined a default values.
	 * </p>
	 * 
	 * <p>
	 * Default values are returned in default implementation in the configuration
	 * interface.
	 * </p>
	 * 
	 * @return true if the property has a default value, false otherwise
	 */
	boolean isDefault();
	
	/**
	 * <p>
	 * Returns the type of the configuration property.
	 * </p>
	 * 
	 * @return a type
	 */
	TypeMirror getType();
}
