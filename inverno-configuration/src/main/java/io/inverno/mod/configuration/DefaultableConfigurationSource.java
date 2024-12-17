/*
 * Copyright 2022 Jeremy KUHN
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
 * A defaultable configuration source has the ability to search for default properties if no exact result exist for a given query.
 * </p>
 * 
 * <p>
 * Such source relies on {@code DefaultingStrategy} to obtain the list of queries to execute from the original query (see
 * {@link DefaultingStrategy#getDefaultingKeys(io.inverno.mod.configuration.ConfigurationKey)}).
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see DefaultingStrategy
 */
public interface DefaultableConfigurationSource extends ConfigurationSource {
	
	/**
	 * <p>
	 * Returns a proxy of the defaultable configuration source instance using the specified defaulting strategy.
	 * </p>
	 * 
	 * @param defaultingStrategy a defaulting strategy
	 * 
	 * @return a new defaultable configuration source
	 */
	DefaultableConfigurationSource withDefaultingStrategy(DefaultingStrategy defaultingStrategy);
}
