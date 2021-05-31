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
 * A nested configuration property info describes a configuration property
 * nested in another configuration property (that can itself be nested).
 * </p>
 * 
 * <p>
 * Note that the type of a nested property info must be a configuration type and
 * therefore correspond to a configuration info.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ConfigurationPropertyInfo
 */
public interface NestedConfigurationPropertyInfo extends ConfigurationPropertyInfo {

	/**
	 * <p>
	 * Returns the configuration info defining the nested property.
	 * </p>
	 * 
	 * @return a configuration info
	 */
	ConfigurationInfo getConfiguration();
}
