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
package io.winterframework.mod.configuration.source;

import java.util.List;
import java.util.stream.Collectors;

import io.winterframework.mod.configuration.ConfigurationEntry;
import io.winterframework.mod.configuration.ConfigurationKey;
import io.winterframework.mod.configuration.ConfigurationQuery;
import io.winterframework.mod.configuration.ExecutableConfigurationQuery;
import io.winterframework.mod.configuration.ExecutableConfigurationQuery.Parameter;

/**
 * @author jkuhn
 *
 */
public class DefaultCompositeConfigurationStrategy implements CompositeConfigurationStrategy {

	@Override
	public boolean isSuperseded(ConfigurationKey queryKey, ConfigurationEntry<?, ?> oldResult, ConfigurationEntry<?, ?> newResult) {
		if(newResult == null) {
			return false;
		}
		
		if(oldResult == null) {
			return true;
		}

		// TODO deal with sources that are not parameterized
		return newResult.getKey().getParameters().size() > oldResult.getKey().getParameters().size();
	}

	@Override
	public boolean isResolved(ConfigurationKey queryKey, ConfigurationEntry<?, ?> result) {
		if(result == null) {
			return false;
		}
		
		// TODO deal with sources that are not parameterized
		return queryKey.getParameters().size() == result.getKey().getParameters().size();
	}

	@Override
	public ExecutableConfigurationQuery<?, ?, ?> populateSourceQuery(ConfigurationKey queryKey, ConfigurationQuery<?, ?, ?> sourceQuery, ConfigurationEntry<?, ?> result) {
		ExecutableConfigurationQuery<?, ?, ?> resultQuery = null;
		
		// a b c d
		//   b c d
		//     c d
		//       d
		//        

		// TODO deal with sources that are not parameterized
		// Here we will create n query whereas for such sources only need one query for the entry name (and no parameters)
		// - we can make things smart in the source implementation
		// - we can be smart here but then we must have a way to determine the nature of the source which is usually the role of types (that should prevent to call withParameters() for a source that doesn't supports it
		
		int depth = result != null ? queryKey.getParameters().size() - result.getKey().getParameters().size() : queryKey.getParameters().size() + 1;
		ConfigurationQuery<?, ?, ?> currentSourceQuery = sourceQuery;
		List<Parameter> parametersList = queryKey.getParameters().entrySet().stream().map(e -> ExecutableConfigurationQuery.parameter(e.getKey(), e.getValue())).collect(Collectors.toList());
		for(int i=0;i<depth;i++) {
			resultQuery = currentSourceQuery.get(queryKey.getName()).withParameters(parametersList.subList(i, parametersList.size()).stream().toArray(Parameter[]::new));
			if(i < depth-1) {
				currentSourceQuery = resultQuery.and();
			}
		}
		return resultQuery;
	}
}
