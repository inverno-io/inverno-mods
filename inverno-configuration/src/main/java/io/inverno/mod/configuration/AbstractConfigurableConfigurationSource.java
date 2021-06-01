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

import io.inverno.mod.base.converter.JoinablePrimitiveEncoder;
import io.inverno.mod.base.converter.SplittablePrimitiveDecoder;

/**
 * <p>
 * Base implementation for {@link ConfigurableConfigurationSource}.
 * </p>
 * 
 * <p>
 * Implementors must rely on the encoder and decoder provided in this
 * implementation to respectively store and retrieve configuration values
 * in/from the configuration source.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ConfigurableConfigurationSource
 * @see ConfigurationQuery
 * @see ExecutableConfigurationQuery
 * @see ConfigurationQueryResult
 * @see ConfigurationUpdate
 * @see ExecutableConfigurationUpdate
 * @see ConfigurationUpdateResult
 * 
 * @param <A> source specific query type
 * @param <B> source specific executable query type
 * @param <C> source specific query result type
 * @param <D> source specific update type
 * @param <E> source specific executable update type
 * @param <F> source specific update result type
 * @param <G> raw configuration value type
 */
public abstract class AbstractConfigurableConfigurationSource<A extends ConfigurationQuery<A, B, C>, B extends ExecutableConfigurationQuery<A, B, C>, C extends ConfigurationQueryResult<?,?>, D extends ConfigurationUpdate<D, E, F>, E extends ExecutableConfigurationUpdate<D, E, F>, F extends ConfigurationUpdateResult<?>, G>
		extends AbstractConfigurationSource<A, B, C, G> implements ConfigurableConfigurationSource<A, B, C, D, E, F> {

	/**
	 * The data encoder to use to encode configuration data into the data source.
	 */
	protected JoinablePrimitiveEncoder<G> encoder;
	
	/**
	 * <p>
	 * Creates a configurable configuration source with the specified encoder and
	 * decoder.
	 * </p>
	 * 
	 * @param encoder a joinable primitive encoder
	 * @param decoder a splittable primitive decoder
	 * 
	 * @throws NullPointerException if one of the specified decoder or encoder is
	 *                              null
	 */
	public AbstractConfigurableConfigurationSource(JoinablePrimitiveEncoder<G> encoder, SplittablePrimitiveDecoder<G> decoder) {
		super(decoder);
		this.setEncoder(encoder);
	}

	/**
	 * <p>
	 * Returns the configuration value encoder.
	 * </p>
	 * 
	 * @return a joinable primitive encoder
	 */
	public JoinablePrimitiveEncoder<G> getEncoder() {
		return encoder;
	}

	/**
	 * <p>
	 * Sets the configuration value encoder.
	 * </p>
	 * 
	 * @param encoder a joinable primitive encoder
	 * 
	 * @throws NullPointerException if the specified encoder is null
	 */
	public void setEncoder(JoinablePrimitiveEncoder<G> encoder) {
		Objects.requireNonNull(encoder, "Value encoder can't be null");
		this.encoder = encoder;
	}
}
