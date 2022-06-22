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
import io.inverno.mod.configuration.DefaultingStrategy;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * A {@link DefaultingStrategy} that prioritizes query parameters from left to right: the best matching property is the one matching the most continuous parameters from left to right.
 * </p>
 *
 * <p>
 * If we consider query key {@code property[p1=v1,...pn=vn]}, it supersedes key {@code property[p1=v1,...pn-1=vn-1]} which supersedes key {@code property[p1=v1,...pn-2=vn-2]}... which supersedes key
 * {@code property[]}. It basically tells the source to lookup by successively removing the rightmost parameter if no exact result exists for a particular query.
 * </p>
 * 
 * <p>
 * An original query with {@code n} parameters then results in {@code n+1} potential queries to execute for the source. The query result is the first non-empty result or the empty result.
 * </p>
 *
 * <p>
 * The order into which parameters are defined in the original query is then significant: {@code property[p1=v1,p2=v2] != property[p2=v2,p1=v1]}.
 * </p>
 * 
 * <p>
 * When listing properties, this strategy includes the <i>parent</i> properties which are obtained by successively removing the rightmost parameter from the original query.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class LookupDefaultingStrategy implements DefaultingStrategy {

	/**
	 * The lookup defaulting strategy singleton.
	 */
	public static final LookupDefaultingStrategy INSTANCE = new LookupDefaultingStrategy();
	
	@Override
	public List<ConfigurationKey> getDefaultingKeys(ConfigurationKey queryKey) {
		String name = queryKey.getName();
		int keyLength = queryKey.getParameters().size();
		List<ConfigurationKey> defaultingKeys = new ArrayList<>(keyLength + 1);

		List<ConfigurationKey.Parameter> parametersList = new ArrayList<>(queryKey.getParameters());
		for(int i=0;i<keyLength + 1;i++) {
			defaultingKeys.add(new GenericConfigurationKey(name, parametersList.subList(0, keyLength - i)));
		}

		return defaultingKeys;
	}

	@Override
	public List<ConfigurationKey> getListDefaultingKeys(ConfigurationKey queryKey) {
		String name = queryKey.getName();
		int keyLength = queryKey.getParameters().size();
		
		List<ConfigurationKey> defaultingKeys = new ArrayList<>(keyLength + 1);
		
		List<ConfigurationKey.Parameter> parametersList = new ArrayList<>(queryKey.getParameters());
		for(int i = keyLength;i >= 0;i--) {
			if(i < keyLength) {
				if(parametersList.get(i).isUndefined()) {
					// Avoid duplicates
					continue;
				}
				parametersList.set(i, ConfigurationKey.Parameter.undefined(parametersList.get(i).getKey()));
			}
			defaultingKeys.add(new GenericConfigurationKey(name, new ArrayList<>(parametersList)));
		}
		
		return defaultingKeys;
	}
}
