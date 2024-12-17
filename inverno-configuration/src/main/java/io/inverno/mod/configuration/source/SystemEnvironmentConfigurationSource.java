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

import io.inverno.mod.base.converter.SplittablePrimitiveDecoder;
import io.inverno.mod.configuration.internal.AbstractPropertiesConfigurationSource;
import io.inverno.mod.configuration.ConfigurationKey;
import io.inverno.mod.configuration.internal.JavaStringConverter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * A configuration source that looks up properties from the system environment variables.
 * </p>
 *
 * <p>
 * Note that this source doesn't support parameterized queries, regardless of the parameters specified in a query, only the configuration key name is considered when resolving a value.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see AbstractPropertiesConfigurationSource
 */
public class SystemEnvironmentConfigurationSource extends AbstractPropertiesConfigurationSource<String, SystemEnvironmentConfigurationSource> {
	
	/**
	 * <p>
	 * Creates a system environment configuration source.
	 * </p>
	 */
	public SystemEnvironmentConfigurationSource() {
		this(new JavaStringConverter());
	}
	
	/**
	 * <p>
	 * Creates a system environment configuration source with the specified string value decoder.
	 * </p>
	 *
	 * @param decoder a string decoder
	 */
	public SystemEnvironmentConfigurationSource(SplittablePrimitiveDecoder<String> decoder) {
		super(decoder);
	}

	private SystemEnvironmentConfigurationSource(SystemEnvironmentConfigurationSource original, List<ConfigurationKey.Parameter> defaultParameters) {
		super(original, defaultParameters);
	}

	/**
	 * <p>
	 * The environment source doesn't support parameterized queries, regardless of the parameters specified in a query, only the configuration key name is considered when resolving a value.
	 * </p>
	 */
	@Override
	public SystemEnvironmentConfigurationSource withParameters(List<ConfigurationKey.Parameter> parameters) throws IllegalArgumentException {
		return new SystemEnvironmentConfigurationSource(this, parameters);
	}

	@Override
	protected Optional<String> getPropertyValue(String name) {
		return Optional.ofNullable(System.getenv(name));
	}

	@Override
	protected Set<String> listProperties() {
		return System.getenv().keySet();
	}
}
