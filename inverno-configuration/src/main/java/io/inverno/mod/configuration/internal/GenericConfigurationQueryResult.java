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

import java.util.Optional;

import io.inverno.mod.configuration.ConfigurationProperty;
import io.inverno.mod.configuration.ConfigurationKey;
import io.inverno.mod.configuration.ConfigurationQueryResult;
import io.inverno.mod.configuration.ConfigurationSource;
import io.inverno.mod.configuration.ConfigurationSourceException;

/**
 * <p>
 * Generic {@link ConfigurationQueryResult} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ConfigurationQueryResult
 *
 * @param <A> the key type
 * @param <B> the property type
 */
public class GenericConfigurationQueryResult<A extends ConfigurationKey, B extends ConfigurationProperty<?,?>> implements ConfigurationQueryResult<A, B> {

	protected A queryKey;
	protected Optional<B> queryResult;
	protected Throwable error;
	protected ConfigurationSource<?,?,?> errorSource;
	
	/**
	 * <p>
	 * Creates a generic successful configuration query result with the specified
	 * query key and result property.
	 * </p>
	 * 
	 * @param queryKey    the query key
	 * @param queryResult the result property
	 */
	public GenericConfigurationQueryResult(A queryKey, B queryResult) {
		this.queryKey = queryKey;
		this.queryResult = Optional.ofNullable(queryResult);
	}
	
	/**
	 * <p>
	 * Creates a generic faulty configuration query result with the specified query
	 * key, configuration source and error.
	 * </p>
	 * 
	 * @param queryKey the query key
	 * @param source   the configuration source
	 * @param error    the error
	 */
	public GenericConfigurationQueryResult(A queryKey, ConfigurationSource<?,?,?> source, Throwable error) {
		this.queryKey = queryKey;
		this.errorSource = source;
		this.error = error;
	}
	
	@Override
	public A getQueryKey() {
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
