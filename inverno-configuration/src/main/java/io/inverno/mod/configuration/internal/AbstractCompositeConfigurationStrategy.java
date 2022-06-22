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
import io.inverno.mod.configuration.ConfigurationSourceException;
import io.inverno.mod.configuration.source.CompositeConfigurationStrategy;

/**
 * <p>
 * Base {@link CompositeConfigurationStrategy} implementation.
 * </p>
 * 
 * <p>
 * This base implementation considers that a result supersedes another one if it specifies more parameters (i.e. it is more precise). It considers that a result is resolved if it exactly specifies the
 * parameters of the query.
 * </p>
 * 
 * <p>
 * Failures can be ignored globally or not.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public abstract class AbstractCompositeConfigurationStrategy implements CompositeConfigurationStrategy {
	
	private final boolean ignoreFailure;

	/**
	 * <p>
	 * Creates a composite configuration strategy.
	 * </p>
	 * 
	 * @param ignoreFailure true to ignore all failure, false otherwise
	 */
	public AbstractCompositeConfigurationStrategy(boolean ignoreFailure) {
		this.ignoreFailure = ignoreFailure;
	}

	@Override
	public boolean ignoreFailure(ConfigurationSourceException error) {
		return this.ignoreFailure;
	}

	@Override
	public boolean isSuperseded(ConfigurationKey queryKey, ConfigurationKey previousKey, ConfigurationKey resultKey) {
		if(resultKey == null) {
			return false;
		}
		if(previousKey == null) {
			return true;
		}

		// non-parameterized sources should always return results corresponding to the query therefore any result they returned should supersede the previous one and eventually get resolved
		return resultKey.getParameters().size() > previousKey.getParameters().size();
	}

	@Override
	public boolean isResolved(ConfigurationKey queryKey, ConfigurationKey resultKey) {
		if(resultKey == null) {
			return false;
		}
		
		// non-parameterized sources should always return results corresponding to the query therefore any result they returned is a resolved result
		return queryKey.getParameters().size() == resultKey.getParameters().size();
	}
}
