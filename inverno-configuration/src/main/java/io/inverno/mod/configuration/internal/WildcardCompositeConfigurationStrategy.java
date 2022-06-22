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
package io.inverno.mod.configuration.internal;

import io.inverno.mod.configuration.ConfigurationKey;
import io.inverno.mod.configuration.source.CompositeConfigurationStrategy;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * A wildcard composite configuration strategy used to query sources in a composite configuration source with a wildcard defaulting strategy.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see WildcardDefaultingStrategy
 */
public class WildcardCompositeConfigurationStrategy extends AbstractCompositeConfigurationStrategy {

	/**
	 * <p>
	 * Creates a wildcard configuration strategy.
	 * </p>
	 * 
	 * @param ignoreFailure true to ignore all failure, false otherwise
	 */
	public WildcardCompositeConfigurationStrategy(boolean ignoreFailure) {
		super(ignoreFailure);
	}

	@Override
	public CompositeDefaultingStrategy createDefaultingStrategy() {
		return new WildcardCompositeDefaultingStrategy();
	}
	
	/**
	 * <p>
	 * A wildcard defaulting strategy that keeps track of query results from previous round in order to reduce the list of defaulting keys to query on subsequent sources.
	 * </p>
	 *
	 * <p>
	 * The {@link WildcardDefaultingStrategy} basically considers the k-combinations of the query parameters which are prioritized from left to right. This strategy filters queries that can't
	 * supersedes results from previous rounds.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	private static class WildcardCompositeDefaultingStrategy extends WildcardDefaultingStrategy implements CompositeConfigurationStrategy.CompositeDefaultingStrategy {

		private final Map<ConfigurationKey, ConfigurationKey> results;

		/**
		 * <p>
		 * Creates a composite wildcard defaulting strategy.
		 * </p>
		 */
		public WildcardCompositeDefaultingStrategy() {
			this.results = new HashMap<>();
		}
		
		@Override
		public void putResult(ConfigurationKey queryKey, ConfigurationKey resultKey) {
			this.results.put(queryKey, resultKey);
		}

		@Override
		public List<ConfigurationKey> getDefaultingKeys(ConfigurationKey queryKey) {
			ConfigurationKey previousKey = this.results.get(queryKey);
			
			List<ConfigurationKey> defaultingKeys = super.getDefaultingKeys(queryKey);
			
			if(previousKey != null) {
				Collection<ConfigurationKey.Parameter> previousKeyParameters = previousKey.getParameters();
				for(int i = 0;i < defaultingKeys.size();i++) {
					if(previousKeyParameters.equals(defaultingKeys.get(i).getParameters())) {
						return defaultingKeys.subList(0, i);
					}
				}
			}
			
			return defaultingKeys;
		}
	}
}
