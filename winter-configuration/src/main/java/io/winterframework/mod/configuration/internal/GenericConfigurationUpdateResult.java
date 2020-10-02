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
package io.winterframework.mod.configuration.internal;

import io.winterframework.mod.configuration.ConfigurationKey;
import io.winterframework.mod.configuration.ConfigurationSource;
import io.winterframework.mod.configuration.ConfigurationSourceException;
import io.winterframework.mod.configuration.ConfigurationUpdateResult;

/**
 * @author jkuhn
 *
 */
public class GenericConfigurationUpdateResult<A extends ConfigurationKey> implements ConfigurationUpdateResult<A> {

	protected A updateKey;
	protected Throwable error;
	protected ConfigurationSource<?,?,?> errorSource;
	
	public GenericConfigurationUpdateResult(A updateKey) {
		this.updateKey = updateKey;
	}
	
	public GenericConfigurationUpdateResult(A updateKey, ConfigurationSource<?,?,?> source, Throwable error) {
		this.updateKey = updateKey;
		this.errorSource = source;
		this.error = error;
	}
	
	@Override
	public A getUpdateKey() {
		return this.updateKey;
	}

	@Override
	public void check() throws ConfigurationSourceException {
		if(this.error != null) {
			throw new ConfigurationSourceException(this.errorSource, this.error);
		}
	}

}
