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
package io.winterframework.mod.configuration;

/**
 * <p>
 * A configuration query is used to query configuration properties from a
 * configuration source.
 * </p>
 * 
 * <p>
 * Note that a single query can result in multiple results being returned if
 * multiple properties have been requested.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see ConfigurationSource
 * @see ExecutableConfigurationQuery
 * @see ConfigurationQueryResult
 * 
 * @param <A> the query type
 * @param <B> the executable query type
 * @param <C> the query result type
 */
public interface ConfigurationQuery<A extends ConfigurationQuery<A, B, C>, B extends ExecutableConfigurationQuery<A, B, C>, C extends ConfigurationQueryResult<?,?>> {

	/**
	 * <p>
	 * Returns an executable query that retrieves the specified properties.
	 * </p>
	 * 
	 * @param names a list of properties to retrieve
	 * 
	 * @return an executable query
	 * @throws IllegalArgumentException if the array of names is null or empty
	 */
	B get(String... names) throws IllegalArgumentException;
}
