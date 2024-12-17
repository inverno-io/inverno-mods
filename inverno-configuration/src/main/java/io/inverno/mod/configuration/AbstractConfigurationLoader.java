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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import io.inverno.mod.configuration.ConfigurationKey.Parameter;

/**
 * <p>
 * Base implementation for {@link ConfigurationLoader}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 * @param <A> the configuration type to load
 * @param <B> the configuration loader type
 */
public abstract class AbstractConfigurationLoader<A, B extends AbstractConfigurationLoader<A,B>> implements ConfigurationLoader<A, B> {

	/**
	 * The configuration source to use to load the configuration.
	 */
	protected ConfigurationSource source;
	
	/**
	 * The parameters to use to retrieve configuration values.
	 */
	protected List<Parameter> parameters;
	
	@SuppressWarnings("unchecked")
	@Override
	public B withParameters(List<Parameter> parameters) throws IllegalArgumentException {
		if(parameters != null && !parameters.isEmpty()) {
			Set<String> parameterKeys = new HashSet<>();
			List<String> duplicateParameters = new LinkedList<>();

			for(Parameter parameter : parameters) {
				if(!parameterKeys.add(parameter.getKey())) {
					duplicateParameters.add(parameter.getKey());
				}
			}
			if(!duplicateParameters.isEmpty()) {
				throw new IllegalArgumentException("The following parameters were specified more than once: " + String.join(", ", duplicateParameters));
			}
			this.parameters = parameters;
		}
		else {
			this.parameters = List.of();
		}
		return (B)this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public B withSource(ConfigurationSource source) {
		this.source = source;
		return (B)this;
	}
}
