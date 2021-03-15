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
 * A configuration source gives access to configuration properties.
 * </p>
 * 
 * <p>Configuration properties can be queries as follows:</p>
 * 
 * <blockquote><pre>
 * ConfigurationSource{@literal <?, ?, ?>} source = null;
 * 
 * Map{@literal <String, String>} propertiesAsString = source
 *     .get("prop1", "prop2")
 *     .execute()
 *     .collect(Collectors.toMap(
 *             result -> result.getQueryKey().getName(), 
 *             result -> result.getResult()
 *                 .flatMap(property -> property.asString()).orElse(null)
 *         )
 *     )
 *     .block();
 * </pre></blockquote>
 * 
 * <p>Parameters can be specified on a query to specify the context for which values must be retrieved:</p>
 * 
 * <blockquote><pre>
 * Map{@literal <String, String>} propertiesAsString = source
 *     .get("prop1", "prop2")
 *         .withParameters("environment", "test")
 *     .execute()
 *     .collect(Collectors.toMap(
 *             result -> result.getQueryKey().getName(), 
 *             result -> result.getResult()
 *                 .flatMap(property -> property.asString()).orElse(null)
 *         )
 *     )
 *     .block();
 * </pre></blockquote>
 * 
 * <p>Queries can be executed in a batch:</p>
 * 
 * <blockquote><pre>
 * Map{@literal <String, String>} propertiesAsString = source
 *     .get("prop1", "prop2").and()
 *     .get("prop3", "prop4")
 *         .withParameters("customer", "abc")
 *     .execute()
 *     .collect(Collectors.toMap(
 *             result -> result.getQueryKey().getName(), 
 *             result -> result.getResult()
 *                 .flatMap(property -> property.asString()).orElse(null)
 *         )
 *     )
 *     .block();
 * </pre></blockquote>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @param <A> source specific query type
 * @param <B> source specific executable query type
 * @param <C> source specific query result type
 */
public interface ConfigurationSource<A extends ConfigurationQuery<A, B, C>, B extends ExecutableConfigurationQuery<A, B, C>, C extends ConfigurationQueryResult<?,?>> {
	
	/**
	 * <p>
	 * Creates a configuration query to retrieve the specified properties.
	 * </p>
	 * 
	 * @param names an array of property names
	 * 
	 * @return an executable configuration query
	 * @throws IllegalArgumentException if the array of names is null or empty
	 */
	B get(String... names) throws IllegalArgumentException;
}
