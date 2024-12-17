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
package io.inverno.mod.configuration.internal;

import io.inverno.mod.base.converter.JoinablePrimitiveEncoder;
import io.inverno.mod.base.converter.SplittablePrimitiveDecoder;
import io.inverno.mod.configuration.ConfigurableConfigurationSource;
import io.inverno.mod.configuration.ConfigurationKey;
import io.inverno.mod.configuration.ConfigurationQuery;
import io.inverno.mod.configuration.ConfigurationQueryResult;
import io.inverno.mod.configuration.ConfigurationUpdate;
import io.inverno.mod.configuration.ConfigurationUpdateResult;
import io.inverno.mod.configuration.ExecutableConfigurationQuery;
import io.inverno.mod.configuration.ExecutableConfigurationUpdate;
import io.inverno.mod.configuration.ListConfigurationQuery;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Base implementation for {@link ConfigurableConfigurationSource}.
 * </p>
 *
 * <p>
 * Implementors must rely on the encoder and decoder provided in this implementation to respectively store and retrieve configuration values in/from the configuration source.
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
 * @param <C> source specific list query type
 * @param <D> source specific update type
 * @param <E> source specific executable update type
 * @param <F> raw configuration value type
 * @param <G> configurable configuration source type
 */
public abstract class AbstractConfigurableConfigurationSource<
		A extends ConfigurationQuery<A, B>,
		B extends ExecutableConfigurationQuery<A, B>,
		C extends ListConfigurationQuery<C>,
		D extends ConfigurationUpdate<D, E>,
		E extends ExecutableConfigurationUpdate<D, E>,
		F,
		G extends AbstractConfigurableConfigurationSource<A, B, C, D, E, F, G>
	>
		extends AbstractConfigurationSource<A, B, C, F, G> implements ConfigurableConfigurationSource {

	/**
	 * The data encoder to use to encode configuration data into the data source.
	 */
	protected JoinablePrimitiveEncoder<F> encoder;
	
	/**
	 * <p>
	 * Creates a configurable configuration source with the specified encoder and decoder.
	 * </p>
	 *
	 * @param encoder a joinable primitive encoder
	 * @param decoder a splittable primitive decoder
	 *
	 * @throws NullPointerException if one of the specified decoder or encoder is null
	 */
	public AbstractConfigurableConfigurationSource(JoinablePrimitiveEncoder<F> encoder, SplittablePrimitiveDecoder<F> decoder) {
		super(decoder);
		this.setEncoder(encoder);
	}

	/**
	 * <p>
	 * Creates a configurable configuration source from the specified original source.
	 * </p>
	 *
	 * @param original the original configuration source
	 */
	protected AbstractConfigurableConfigurationSource(G original) {
		this(original, original.defaultParameters);
	}

	/**
	 * <p>
	 * Creates a configurable configuration source from the specified original source which applies the specified default parameters.
	 * </p>
	 *
	 * @param original          the original configuration source
	 * @param defaultParameters the default parameters to apply when querying the source
	 */
	protected AbstractConfigurableConfigurationSource(G original, List<ConfigurationKey.Parameter> defaultParameters) {
		super(original, defaultParameters);
		this.setEncoder(original.encoder);
	}

	/**
	 * <p>
	 * Returns the configuration value encoder.
	 * </p>
	 * 
	 * @return a joinable primitive encoder
	 */
	public JoinablePrimitiveEncoder<F> getEncoder() {
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
	public void setEncoder(JoinablePrimitiveEncoder<F> encoder) {
		Objects.requireNonNull(encoder, "Value encoder can't be null");
		this.encoder = encoder;
	}

	@Override
	public final E set(String name1, Object value1) {
		return this.set(Map.of(name1, value1));
	}

	@Override
	public final E set(String name1, Object value1, String name2, Object value2) {
		return this.set(Map.of(name1, value1, name2, value2));
	}

	@Override
	public final E set(String name1, Object value1, String name2, Object value2, String name3, Object value3) {
		return this.set(Map.of(name1, value1, name2, value2, name3, value3));
	}

	@Override
	public final E set(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4) {
		return this.set(Map.of(name1, value1, name2, value2, name3, value3, name4, value4));
	}

	@Override
	public final E set(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4, String name5, Object value5) {
		return this.set(Map.of(name1, value1, name2, value2, name3, value3, name4, value4, name5, value5));
	}

	@Override
	public final E set(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4, String name5, Object value5, String name6, Object value6) {
		return this.set(Map.of(name1, value1, name2, value2, name3, value3, name4, value4, name5, value5));
	}

	@Override
	public final E set(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4, String name5, Object value5, String name6, Object value6, String name7, Object value7) {
		return this.set(Map.of(name1, value1, name2, value2, name3, value3, name4, value4, name5, value5, name6, value6, name7, value7));
	}

	@Override
	public final E set(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4, String name5, Object value5, String name6, Object value6, String name7, Object value7, String name8, Object value8) {
		return this.set(Map.of(name1, value1, name2, value2, name3, value3, name4, value4, name5, value5, name6, value6, name7, value7, name8, value8));
	}

	@Override
	public final E set(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4, String name5, Object value5, String name6, Object value6, String name7, Object value7, String name8, Object value8, String name9, Object value9) {
		return this.set(Map.of(name1, value1, name2, value2, name3, value3, name4, value4, name5, value5, name6, value6, name7, value7, name8, value8, name9, value9));
	}

	@Override
	public final E set(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4, String name5, Object value5, String name6, Object value6, String name7, Object value7, String name8, Object value8, String name9, Object value9, String name10, Object value10) {
		return this.set(Map.of(name1, value1, name2, value2, name3, value3, name4, value4, name5, value5, name6, value6, name7, value7, name8, value8, name9, value9, name10, value10));
	}

	@Override
	public abstract E set(Map<String, Object> values) throws IllegalArgumentException;
}
