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

import java.util.Optional;

/**
 * <p>
 * Represents a single query result providing the configuration property
 * retrieved from a configuration source with a query key.
 * </p>
 * 
 * <p>
 * Note that the query key and the property key may differs if the configuration
 * source uses a defaulting mechanism to return the value that best matches the
 * context specified in the query key.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ConfigurationQuery
 * @see ConfigurationSource
 * 
 * @param <A> the type of the configuration key
 * @param <B> the type of the resulting configuration property
 */
public interface ConfigurationQueryResult<A extends ConfigurationKey, B extends ConfigurationProperty<?,?>> {

	/**
	 * <p>
	 * Returns the configuration key corresponding to the query that was executed.
	 * </p>
	 * 
	 * @return a configuration key
	 */
	A getQueryKey();
	
	/**
	 * <p>
	 * Returns the resulting configuration property.
	 * </p>
	 * 
	 * @return an optional returning the configuration property or an empty optional
	 *         if the configuration returned no value for the property
	 * @throws ConfigurationSourceException if there was an error retrieving the
	 *                                      configuration property
	 */
	Optional<B> getResult() throws ConfigurationSourceException;
}
