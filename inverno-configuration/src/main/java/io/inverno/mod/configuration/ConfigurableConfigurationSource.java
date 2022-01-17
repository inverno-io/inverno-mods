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

import java.util.Map;

/**
 * <p>
 * A configurable {@link ConfigurationSource} that supports configuration properties updates.
 * </p>
 *
 * <p>
 * Configuration properties can be set in the configuration source as follows:
 * </p>
 *
 * <blockquote><pre>
 * ConfigurableConfigurationSource{@literal <?,?,?,?,?>} source = ...;
 *
 * source.set("prop1", "value1")
 *     .and().set("prop2", "value2")
 *     .execute()
 *     .subscribe();
 * </pre></blockquote>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see ConfigurationSource
 * @see ConfigurationQuery
 * @see ExecutableConfigurationQuery
 * @see ConfigurationQueryResult
 * @see ConfigurationUpdate
 * @see ExecutableConfigurationUpdate
 * @see ConfigurationUpdateResult
 *
 * @param <A> source specific query type
 * @param <B> source specific executable query type
 * @param <C> source specific list query type
 * @param <D> source specific update type
 * @param <E> source specific executable update type
 */
public interface ConfigurableConfigurationSource<A extends ConfigurationQuery<A, B>, B extends ExecutableConfigurationQuery<A, B>, C extends ListConfigurationQuery<C>, D extends ConfigurationUpdate<D, E>, E extends ExecutableConfigurationUpdate<D, E>> extends ConfigurationSource<A, B, C> {

	/**
	 * <p>
	 * Creates a configuration update to set one configuration property in the configuration source.
	 * </p>
	 *
	 * @param name1  the property key
	 * @param value1 the property value
	 *
	 * @return an executable configuration update
	 */
	default E set(String name1, Object value1) {
		return this.set(Map.of(name1, value1));
	}
	
	/**
	 * <p>
	 * Creates a configuration update to set two configuration properties in the configuration source.
	 * </p>
	 *
	 * @param name1  the first property key
	 * @param value1 the first property value
	 * @param name2  the second property key
	 * @param value2 the second property value
	 *
	 * @return an executable configuration update
	 */
	default E set(String name1, Object value1, String name2, Object value2) {
		return this.set(Map.of(name1, value1, name2, value2));
	}

	/**
	 * <p>
	 * Creates a configuration update to set three configuration properties in the configuration source.
	 * </p>
	 *
	 * @param name1  the first property key
	 * @param value1 the first property value
	 * @param name2  the second property key
	 * @param value2 the second property value
	 * @param name3  the third property key
	 * @param value3 the third property value
	 *
	 * @return an executable configuration update
	 */
	default E set(String name1, Object value1, String name2, Object value2, String name3, Object value3) {
		return this.set(Map.of(name1, value1, name2, value2, name3, value3));
	}
	
	/**
	 * <p>
	 * Creates a configuration update to set four configuration properties in the configuration source.
	 * </p>
	 *
	 * @param name1  the first property key
	 * @param value1 the first property value
	 * @param name2  the second property key
	 * @param value2 the second property value
	 * @param name3  the third property key
	 * @param value3 the third property value
	 * @param name4  the fourth property key
	 * @param value4 the fourth property value
	 *
	 * @return an executable configuration update
	 */
	default E set(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4) {
		return this.set(Map.of(name1, value1, name2, value2, name3, value3, name4, value4));
	}
	
	/**
	 * <p>
	 * Creates a configuration update to set five configuration properties in the configuration source.
	 * </p>
	 *
	 * @param name1  the first property key
	 * @param value1 the first property value
	 * @param name2  the second property key
	 * @param value2 the second property value
	 * @param name3  the third property key
	 * @param value3 the third property value
	 * @param name4  the fourth property key
	 * @param value4 the fourth property value
	 * @param name5  the fifth property key
	 * @param value5 the fifth property value
	 *
	 * @return an executable configuration update
	 */
	default E set(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4, String name5, Object value5) {
		return this.set(Map.of(name1, value1, name2, value2, name3, value3, name4, value4, name5, value5));
	}
	
	/**
	 * <p>
	 * Creates a configuration update to set six configuration properties in the configuration source.
	 * </p>
	 *
	 * @param name1  the first property key
	 * @param value1 the first property value
	 * @param name2  the second property key
	 * @param value2 the second property value
	 * @param name3  the third property key
	 * @param value3 the third property value
	 * @param name4  the fourth property key
	 * @param value4 the fourth property value
	 * @param name5  the fifth property key
	 * @param value5 the fifth property value
	 * @param name6  the sixth property key
	 * @param value6 the sixth property value
	 *
	 * @return an executable configuration update
	 */
	default E set(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4, String name5, Object value5, String name6, Object value6) {
		return this.set(Map.of(name1, value1, name2, value2, name3, value3, name4, value4, name5, value5, name6, value6));
	}
	
	/**
	 * <p>
	 * Creates a configuration update to set seven configuration properties in the configuration source.
	 * </p>
	 *
	 * @param name1  the first property key
	 * @param value1 the first property value
	 * @param name2  the second property key
	 * @param value2 the second property value
	 * @param name3  the third property key
	 * @param value3 the third property value
	 * @param name4  the fourth property key
	 * @param value4 the fourth property value
	 * @param name5  the fifth property key
	 * @param value5 the fifth property value
	 * @param name6  the sixth property key
	 * @param value6 the sixth property value
	 * @param name7  the seventh property key
	 * @param value7 the seventh property value
	 *
	 * @return an executable configuration update
	 */
	default E set(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4, String name5, Object value5, String name6, Object value6, String name7, Object value7) {
		return this.set(Map.of(name1, value1, name2, value2, name3, value3, name4, value4, name5, value5, name6, value6, name7, value7));
	}
	
	/**
	 * <p>
	 * Creates a configuration update to set eight configuration properties in the configuration source.
	 * </p>
	 *
	 * @param name1  the first property key
	 * @param value1 the first property value
	 * @param name2  the second property key
	 * @param value2 the second property value
	 * @param name3  the third property key
	 * @param value3 the third property value
	 * @param name4  the fourth property key
	 * @param value4 the fourth property value
	 * @param name5  the fifth property key
	 * @param value5 the fifth property value
	 * @param name6  the sixth property key
	 * @param value6 the sixth property value
	 * @param name7  the seventh property key
	 * @param value7 the seventh property value
	 * @param name8  the eighth property key
	 * @param value8 the eighth property value
	 *
	 * @return an executable configuration update
	 */
	default E set(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4, String name5, Object value5, String name6, Object value6, String name7, Object value7, String name8, Object value8) {
		return this.set(Map.of(name1, value1, name2, value2, name3, value3, name4, value4, name5, value5, name6, value6, name7, value7, name8, value8));
	}
	
	/**
	 * <p>
	 * Creates a configuration update to set nine configuration properties in the configuration source.
	 * </p>
	 *
	 * @param name1  the first property key
	 * @param value1 the first property value
	 * @param name2  the second property key
	 * @param value2 the second property value
	 * @param name3  the third property key
	 * @param value3 the third property value
	 * @param name4  the fourth property key
	 * @param value4 the fourth property value
	 * @param name5  the fifth property key
	 * @param value5 the fifth property value
	 * @param name6  the sixth property key
	 * @param value6 the sixth property value
	 * @param name7  the seventh property key
	 * @param value7 the seventh property value
	 * @param name8  the eighth property key
	 * @param value8 the eighth property value
	 * @param name9  the ninth property key
	 * @param value9 the ninth property value
	 *
	 * @return an executable configuration update
	 */
	default E set(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4, String name5, Object value5, String name6, Object value6, String name7, Object value7, String name8, Object value8, String name9, Object value9) {
		return this.set(Map.of(name1, value1, name2, value2, name3, value3, name4, value4, name5, value5, name6, value6, name7, value7, name8, value8, name9, value9));
	}
	
	/**
	 * <p>
	 * Creates a configuration update to set ten configuration properties in the configuration source.
	 * </p>
	 *
	 * @param name1   the first property key
	 * @param value1  the first property value
	 * @param name2   the second property key
	 * @param value2  the second property value
	 * @param name3   the third property key
	 * @param value3  the third property value
	 * @param name4   the fourth property key
	 * @param value4  the fourth property value
	 * @param name5   the fifth property key
	 * @param value5  the fifth property value
	 * @param name6   the sixth property key
	 * @param value6  the sixth property value
	 * @param name7   the seventh property key
	 * @param value7  the seventh property value
	 * @param name8   the eighth property key
	 * @param value8  the eighth property value
	 * @param name9   the ninth property key
	 * @param value9  the ninth property value
	 * @param name10  the tenth property key
	 * @param value10 the tenth property value
	 *
	 * @return an executable configuration update
	 */
	default E set(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4, String name5, Object value5, String name6, Object value6, String name7, Object value7, String name8, Object value8, String name9, Object value9, String name10, Object value10) {
		return this.set(Map.of(name1, value1, name2, value2, name3, value3, name4, value4, name5, value5, name6, value6, name7, value7, name8, value8, name9, value9, name10, value10));
	}

	/**
	 * <p>
	 * Creates a configuration update to set the configuration properties extracted from the specified values.
	 * </p>
	 *
	 * @param values a map containing properties keys and values to set in the configuration source
	 *
	 * @return an executable configuration update
	 *
	 * @throws IllegalArgumentException if the map of properties is null or empty
	 */
	E set(Map<String, Object> values) throws IllegalArgumentException;
}
