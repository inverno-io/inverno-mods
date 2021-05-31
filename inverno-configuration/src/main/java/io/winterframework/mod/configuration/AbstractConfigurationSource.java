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

import java.util.Objects;

import io.inverno.mod.base.converter.SplittablePrimitiveDecoder;

/**
 * <p>
 * Base implementation for {@link ConfigurationSource}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ConfigurationSource
 * @see ConfigurationQuery
 * @see ExecutableConfigurationQuery
 * @see ConfigurationQueryResult
 *
 * @param <A> source specific configuration query type
 * @param <B> source specific executable configuration query type
 * @param <C> source specific configuration query result
 * @param <D> raw configuration value type
 */
public abstract class AbstractConfigurationSource<A extends ConfigurationQuery<A, B, C>, B extends ExecutableConfigurationQuery<A, B, C>, C extends ConfigurationQueryResult<?,?>, D> implements ConfigurationSource<A, B, C> {

	/**
	 * The data encoder to use to decode configuration data from the data source.
	 */
	protected SplittablePrimitiveDecoder<D> decoder;
	
	/**
	 * <p>
	 * Creates a configuration source with the specified decoder.
	 * </p>
	 * 
	 * @param decoder a splittable primitive decoder
	 * 
	 * @throws NullPointerException if the specified decoder is null
	 */
	public AbstractConfigurationSource(SplittablePrimitiveDecoder<D> decoder) {
		this.setDecoder(decoder);
	}
	
	/**
	 * <p>
	 * Returns the value decoder.
	 * </p>
	 * 
	 * @return a splittable primitive decoder
	 */
	public SplittablePrimitiveDecoder<D> getDecoder() {
		return decoder;
	}

	/**
	 * <p>
	 * Sets the configuration value decoder.
	 * </p>
	 * 
	 * @param decoder a splittable primitive decoder
	 * 
	 * @throws NullPointerException if the specified decoder is null
	 */
	public void setDecoder(SplittablePrimitiveDecoder<D> decoder) {
		Objects.requireNonNull(decoder, "Value decoder can't be null");
		this.decoder = decoder;
	}
}
