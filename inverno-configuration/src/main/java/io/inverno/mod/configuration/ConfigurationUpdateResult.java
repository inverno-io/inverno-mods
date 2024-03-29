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
package io.inverno.mod.configuration;

/**
 * <p>
 * Represents a single update result.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see ConfigurationUpdate
 * @see ConfigurableConfigurationSource
 */
public interface ConfigurationUpdateResult {

	/**
	 * <p>
	 * Returns the configuration key corresponding to the update that was executed.
	 * </p>
	 * 
	 * @return a configuration key
	 */
	ConfigurationKey getUpdateKey();
	
	/**
	 * <p>
	 * Checks that the update was successful.
	 * </p>
	 * 
	 * @throws ConfigurationSourceException if the update was not successful
	 */
	void check() throws ConfigurationSourceException;
}
