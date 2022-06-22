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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * A lookup composite configuration strategy used to query sources in a composite configuration source with a lookup defaulting strategy.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see LookupDefaultingStrategy
 */
public class LookupCompositeConfigurationStrategy extends AbstractCompositeConfigurationStrategy {

	/**
	 * <p>
	 * Creates a lookup configuration strategy.
	 * </p>
	 * 
	 * @param ignoreFailure true to ignore all failure, false otherwise
	 */
	public LookupCompositeConfigurationStrategy(boolean ignoreFailure) {
		super(ignoreFailure);
	}

	@Override
	public CompositeDefaultingStrategy createDefaultingStrategy() {
		return new LookupCompositeDefaultingStrategy();
	}

	/**
	 * <p>
	 * A lookup defaulting strategy that keeps track of query results from previous round in order to reduce the list of defaulting keys to query on subsequent sources.
	 * </p>
	 *
	 * <p>
	 * The {@link LookupDefaultingStrategy} basically considers parameters from left to right, it looks up by removing parameters from right to left. This strategy filters queries that can't
	 * supersedes results from previous rounds.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	private static class LookupCompositeDefaultingStrategy extends LookupDefaultingStrategy implements CompositeConfigurationStrategy.CompositeDefaultingStrategy {

		private final Map<ConfigurationKey, ConfigurationKey> results;

		/**
		 * <p>
		 * Creates a composite lookup defaulting strategy.
		 * </p>
		 */
		public LookupCompositeDefaultingStrategy() {
			this.results = new HashMap<>();
		}
		
		@Override
		public void putResult(ConfigurationKey queryKey, ConfigurationKey resultKey) {
			this.results.put(queryKey, resultKey);
		}

		@Override
		public List<ConfigurationKey> getDefaultingKeys(ConfigurationKey queryKey) {
			ConfigurationKey previousKey = results.get(queryKey);
			
			String name = queryKey.getName();
			int depth = previousKey != null ? queryKey.getParameters().size() - previousKey.getParameters().size() : queryKey.getParameters().size() + 1;
			List<ConfigurationKey> defaultingKeys = new ArrayList<>(depth);

			List<ConfigurationKey.Parameter> parametersList = new ArrayList<>(queryKey.getParameters());
			for(int i=0;i<depth;i++) {
				defaultingKeys.add(new GenericConfigurationKey(name, parametersList.subList(0, parametersList.size() - i)));
			}
			
			return defaultingKeys;
		}
	}
}
