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

import java.util.Optional;

import io.winterframework.mod.configuration.ConfigurationEntry;
import io.winterframework.mod.configuration.ConfigurationKey;
import io.winterframework.mod.configuration.ConfigurationQueryResult;
import io.winterframework.mod.configuration.ConfigurationSource;
import io.winterframework.mod.configuration.ConfigurationSourceException;

/**
 * @author jkuhn
 *
 */
public class GenericConfigurationQueryResult<A extends ConfigurationKey, B extends ConfigurationEntry<?,?>> implements ConfigurationQueryResult<A, B> {

	protected A queryKey;
	protected Optional<B> queryResult;
	protected Throwable error;
	protected ConfigurationSource<?,?,?> errorSource;
	
	public GenericConfigurationQueryResult(A queryKey, B queryResult) {
		this.queryKey = queryKey;
		this.queryResult = Optional.ofNullable(queryResult);
	}
	
	public GenericConfigurationQueryResult(A queryKey, ConfigurationSource<?,?,?> source, Throwable error) {
		this.queryKey = queryKey;
		this.errorSource = source;
		this.error = error;
	}
	
	@Override
	public A getQuery() {
		return this.queryKey;
	}

	@Override
	public Optional<B> getResult() throws ConfigurationSourceException {
		if(this.error != null) {
			throw new ConfigurationSourceException(this.errorSource, this.error);
		}
		return this.queryResult;
	}
}
