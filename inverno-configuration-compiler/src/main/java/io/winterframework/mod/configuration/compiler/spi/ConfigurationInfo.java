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

import javax.lang.model.type.DeclaredType;

import io.inverno.core.compiler.spi.BeanQualifiedName;
import io.inverno.core.compiler.spi.Info;

/**
 * <p>
 * A configuration info describes a configuration.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public interface ConfigurationInfo extends Info {

	/**
	 * <p>
	 * Returns the qualified name of the bean deriving from the configuration.
	 * </p>
	 */
	@Override
	BeanQualifiedName getQualifiedName();

	/**
	 * <p>
	 * Returns the type of the configuration.
	 * </p>
	 * 
	 * @return a type
	 */
	DeclaredType getType();

	/**
	 * <p>
	 * Returns the list of properties defined in the configuration.
	 * </p>
	 * 
	 * @return an array of configuration properties
	 */
	ConfigurationPropertyInfo[] getProperties();

	/**
	 * <p>
	 * Indicates whether a bean should be generated for the configuration.
	 * </p>
	 * 
	 * @return true to generate a bean, false otherwise
	 */
	boolean isGenerateBean();

	/**
	 * <p>
	 * Indicates whether the generated configuration bean is overridable.
	 * </p>
	 * 
	 * @return true of the bean must be overridable, false otherwise
	 */
	boolean isOverridable();

	/**
	 * <p>
	 * Accepts the specified configuration info visitor.
	 * </p>
	 * 
	 * @param <R>     the type of the visitor result
	 * @param <P>     the type of the visitor parameter
	 * @param visitor the visitor to invoke
	 * @param p       the parameter
	 * 
	 * @return the visitor result
	 */
	<R, P> R accept(ConfigurationInfoVisitor<R, P> visitor, P p);
}
