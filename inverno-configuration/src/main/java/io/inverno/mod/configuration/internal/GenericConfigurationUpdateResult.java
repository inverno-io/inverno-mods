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
package io.inverno.mod.configuration.internal;

import io.inverno.mod.configuration.ConfigurationKey;
import io.inverno.mod.configuration.ConfigurationSource;
import io.inverno.mod.configuration.ConfigurationSourceException;
import io.inverno.mod.configuration.ConfigurationUpdateResult;

/**
 * <p>
 * Generic {@link ConfigurationUpdateResult} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ConfigurationUpdateResult
 */
public class GenericConfigurationUpdateResult implements ConfigurationUpdateResult {

	protected final ConfigurationKey updateKey;
	protected Throwable error;
	protected ConfigurationSource<?,?> errorSource;
	
	/**
	 * <p>
	 * Creates a generic successful configuration update result with the specified query key.
	 * </p>
	 *
	 * @param updateKey the query key
	 */
	public GenericConfigurationUpdateResult(ConfigurationKey updateKey) {
		this.updateKey = updateKey;
	}
	
	/**
	 * <p>
	 * Creates a generic faulty configuration update result with the specified query key, configuration source and error.
	 * </p>
	 *
	 * @param updateKey the query key
	 * @param source    the configuration source
	 * @param error     the error
	 */
	public GenericConfigurationUpdateResult(ConfigurationKey updateKey, ConfigurationSource<?,?> source, Throwable error) {
		this.updateKey = updateKey;
		this.errorSource = source;
		this.error = error;
	}
	
	@Override
	public ConfigurationKey getUpdateKey() {
		return this.updateKey;
	}

	@Override
	public void check() throws ConfigurationSourceException {
		if(this.error != null) {
			throw new ConfigurationSourceException(this.errorSource, this.error);
		}
	}

}
