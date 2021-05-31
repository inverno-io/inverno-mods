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
package io.inverno.mod.configuration.source;

import java.util.ArrayList;
import java.util.List;

import io.inverno.mod.configuration.ConfigurationKey;
import io.inverno.mod.configuration.ConfigurationKey.Parameter;
import io.inverno.mod.configuration.ConfigurationProperty;
import io.inverno.mod.configuration.ConfigurationQuery;
import io.inverno.mod.configuration.ConfigurationSourceException;
import io.inverno.mod.configuration.ExecutableConfigurationQuery;

/**
 * <p>
 * Default {@link CompositeConfigurationStrategy} implementation.
 * </p>
 * 
 * <p>
 * This strategy prioritizes sources in the order in which they have been set in
 * the composite configuration source from the highest priority to the lowest.
 * </p>
 * 
 * <p>
 * It determines the best matching result for a given original query by
 * prioritizing query parameters from left to right: the best matching property
 * is the one matching the most continuous parameters from right to left. If we
 * consider query key {@code property[p1=v1,...pn=vn]}, it supersedes key
 * {@code property[p2=v2,...pn=vn]} which supersedes key
 * {@code property[p3=v3,...pn=vn]}... which supersedes key {@code property[]}.
 * </p>
 * 
 * <p>
 * As a result, an original query with {@code n} parameters results in
 * {@code n+1} queries being populated in the source query when no previous
 * result exists from previous sources and {@code n-p} queries when there was a
 * previous result with {@code p} parameters. A query is then resolved when a
 * result exactly matching the original query is found.
 * </p>
 * 
 * <p>
 * The order into which parameters are defined in the original query is then
 * significant: {@code property[p1=v1,p2=v2] != property[p2=v2,p1=v1]}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see CompositeConfigurationSource
 * @see CompositeConfigurationStrategy
 */
public class DefaultCompositeConfigurationStrategy implements CompositeConfigurationStrategy {

	private boolean ignoreFailure = true;
	
	/**
	 * <p>
	 * Enables/disables ignore failure globally.
	 * </p>
	 * 
	 * @param ignoreFailure true to ignore all failure, false otherwise
	 */
	public void setIgnoreFailure(boolean ignoreFailure) {
		this.ignoreFailure = ignoreFailure;
	}

	/**
	 * <p>
	 * Ignores all failure if the strategy is configured to ignore failures
	 * globally.
	 * </p>
	 * 
	 * @see DefaultCompositeConfigurationStrategy#setIgnoreFailure(boolean)
	 */
	@Override
	public boolean ignoreFailure(ConfigurationSourceException error) {
		return this.ignoreFailure;
	}
	
	@Override
	public boolean isSuperseded(ConfigurationKey queryKey, ConfigurationProperty<?, ?> previousResult, ConfigurationProperty<?, ?> result) {
		if(result == null) {
			return false;
		}
		if(previousResult == null) {
			return true;
		}

		// non-parameterized sources should always return results corresponding to the query therefore any result they returned should supersede the previous one and eventually get resolved
		return result.getKey().getParameters().size() > previousResult.getKey().getParameters().size();
	}

	@Override
	public boolean isResolved(ConfigurationKey queryKey, ConfigurationProperty<?, ?> result) {
		if(result == null) {
			return false;
		}
		
		// non-parameterized sources should always return results corresponding to the query therefore any result they returned is a resolved result
		return queryKey.getParameters().size() == result.getKey().getParameters().size();
	}

	@Override
	public ExecutableConfigurationQuery<?, ?, ?> populateSourceQuery(ConfigurationKey queryKey, ConfigurationQuery<?, ?, ?> sourceQuery, ConfigurationProperty<?, ?> previousResult) {
		ExecutableConfigurationQuery<?, ?, ?> resultQuery = null;
		
		// a b c d
		//   b c d
		//     c d
		//       d
		//        

		// This is safe to use with non-parameterized sources as they return the same result regardless of the queried parameters and the first return result is always resolved as it exactly corresponds to the query
		// Here we will create n query whereas for such sources we only need one query for the property name (and no parameters)
		// - we can make things smart in the source implementation
		// - we can be smart here but then we must have a way to determine the nature of the source which is usually the role of types (that should prevent to call withParameters() for a source that doesn't supports it
		
		int depth = previousResult != null ? queryKey.getParameters().size() - previousResult.getKey().getParameters().size() : queryKey.getParameters().size() + 1;
		ConfigurationQuery<?, ?, ?> currentSourceQuery = sourceQuery;
		
		List<Parameter> parametersList = new ArrayList<>(queryKey.getParameters());
		for(int i=0;i<depth;i++) {
			resultQuery = currentSourceQuery.get(queryKey.getName()).withParameters(parametersList.subList(i, parametersList.size()).stream().toArray(Parameter[]::new));
			if(i < depth-1) {
				currentSourceQuery = resultQuery.and();
			}
		}
		return resultQuery;
	}
}
