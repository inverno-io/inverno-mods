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

/**
 * <p>
 * A configuration info visitor is used to process a configuration info.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @param <R> the visitor result type
 * @param <P> the visitor parameter type
 */
public interface ConfigurationInfoVisitor<R, P> {

	/**
	 * <p>
	 * Visits configuration info.
	 * </p>
	 * 
	 * @param configurationInfo the info to visit
	 * @param p                 a visitor parameter
	 * 
	 * @return a visitor result
	 */
	R visit(ConfigurationInfo configurationInfo, P p);

	/**
	 * <p>
	 * Visits configuration property info.
	 * </p>
	 * 
	 * @param configurationPropertyInfo the info to visit
	 * @param p                         a visitor parameter
	 * 
	 * @return a visitor result
	 */
	R visit(ConfigurationPropertyInfo configurationPropertyInfo, P p);

	/**
	 * <p>
	 * Visits nested configuration property info.
	 * </p>
	 * 
	 * @param nestedConfigurationPropertyInfo the info to visit
	 * @param p                               a visitor parameter
	 * 
	 * @return a visitor result
	 */
	R visit(NestedConfigurationPropertyInfo nestedConfigurationPropertyInfo, P p);
}
