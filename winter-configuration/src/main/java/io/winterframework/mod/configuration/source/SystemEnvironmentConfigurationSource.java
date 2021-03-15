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

import io.winterframework.mod.base.converter.SplittablePrimitiveDecoder;
import io.winterframework.mod.configuration.AbstractPropertiesConfigurationSource;
import io.winterframework.mod.configuration.internal.JavaStringConverter;

/**
 * <p>
 * A configuration source that looks up properties from the system environment
 * variables.
 * </p>
 * 
 * <p>
 * Note that this source doesn't support parameterized queries, regardless of
 * the parameters specified in a query, only the configuration key name is
 * considered when resolving a value.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
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
	 * Creates a system environment configuration source with the specified string
	 * value decoder.
	 * </p>
	 * 
	 * @param decoder a string decoder
	 */
	public SystemEnvironmentConfigurationSource(SplittablePrimitiveDecoder<String> decoder) {
		super(decoder, System::getenv);
	}
}
