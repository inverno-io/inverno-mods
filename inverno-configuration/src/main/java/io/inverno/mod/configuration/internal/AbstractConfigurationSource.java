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

import io.inverno.mod.base.converter.SplittablePrimitiveDecoder;
import io.inverno.mod.configuration.ConfigurationKey;
import io.inverno.mod.configuration.ConfigurationQuery;
import io.inverno.mod.configuration.ConfigurationQueryResult;
import io.inverno.mod.configuration.ConfigurationSource;
import io.inverno.mod.configuration.ExecutableConfigurationQuery;
import io.inverno.mod.configuration.ListConfigurationQuery;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
 * @param <C> source specific list configuration query type
 * @param <D> raw configuration value type
 * @param <E> configuration source type
 */
public abstract class AbstractConfigurationSource<A extends ConfigurationQuery<A, B>, B extends ExecutableConfigurationQuery<A, B>, C extends ListConfigurationQuery<C>, D, E extends AbstractConfigurationSource<A, B, C, D, E>> implements ConfigurationSource {

	/**
	 * The original configuration source (i.e. before setting fixed parameters and/or defaulting strategy).
	 */
	protected final E original;

	/**
	 * Default parameters to apply first when querying the source.
	 */
	protected final List<ConfigurationKey.Parameter> defaultParameters;

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
		this.original = null;
		this.defaultParameters = List.of();
		this.setDecoder(decoder);
	}

	/**
	 * <p>
	 * Creates a configuration source from the specified original source.
	 * </p>
	 *
	 * @param original the original configuration source
	 */
	protected AbstractConfigurationSource(E original) {
		this(original, original.defaultParameters);
	}

	/**
	 * <p>
	 * Creates a configuration source from the specified original source which applies the specified default parameters.
	 * </p>
	 *
	 * @param original          the original configuration source
	 * @param defaultParameters the default parameters to apply when querying the source
	 */
	protected AbstractConfigurationSource(E original, List<ConfigurationKey.Parameter> defaultParameters) {
		this.original = original;
		this.defaultParameters = Collections.unmodifiableList(GenericConfigurationKey.requireDistinctParameters(defaultParameters));
		this.setDecoder(original.decoder);
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

	@Override
	@SuppressWarnings("unchecked")
	public E unwrap() {
		E current = (E)this;
		while(current.original != null) {
			current = current.original;
		}
		return current;
	}

	@Override
	public abstract B get(String... names) throws IllegalArgumentException;

	@Override
	public abstract C list(String name) throws IllegalArgumentException;

	@Override
	public final E withParameters(String k1, Object v1) {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1)));
	}

	@Override
	public final E withParameters(String k1, Object v1, String k2, Object v2) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2)));
	}

	@Override
	public final E withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2), ConfigurationKey.Parameter.of(k3, v3)));
	}

	@Override
	public final E withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2), ConfigurationKey.Parameter.of(k3, v3), ConfigurationKey.Parameter.of(k4, v4)));
	}

	@Override
	public final E withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2), ConfigurationKey.Parameter.of(k3, v3), ConfigurationKey.Parameter.of(k4, v4), ConfigurationKey.Parameter.of(k5, v5)));
	}

	@Override
	public final E withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2), ConfigurationKey.Parameter.of(k3, v3), ConfigurationKey.Parameter.of(k4, v4), ConfigurationKey.Parameter.of(k5, v5), ConfigurationKey.Parameter.of(k6, v6)));
	}

	@Override
	public final E withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2), ConfigurationKey.Parameter.of(k3, v3), ConfigurationKey.Parameter.of(k4, v4), ConfigurationKey.Parameter.of(k5, v5), ConfigurationKey.Parameter.of(k6, v6), ConfigurationKey.Parameter.of(k7, v7)));
	}

	@Override
	public final E withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2), ConfigurationKey.Parameter.of(k3, v3), ConfigurationKey.Parameter.of(k4, v4), ConfigurationKey.Parameter.of(k5, v5), ConfigurationKey.Parameter.of(k6, v6), ConfigurationKey.Parameter.of(k7, v7), ConfigurationKey.Parameter.of(k8, v8)));
	}

	@Override
	public final E withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2), ConfigurationKey.Parameter.of(k3, v3), ConfigurationKey.Parameter.of(k4, v4), ConfigurationKey.Parameter.of(k5, v5), ConfigurationKey.Parameter.of(k6, v6), ConfigurationKey.Parameter.of(k7, v7), ConfigurationKey.Parameter.of(k8, v8), ConfigurationKey.Parameter.of(k9, v9)));
	}

	@Override
	public final E withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9, String k10, Object v10) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2), ConfigurationKey.Parameter.of(k3, v3), ConfigurationKey.Parameter.of(k4, v4), ConfigurationKey.Parameter.of(k5, v5), ConfigurationKey.Parameter.of(k6, v6), ConfigurationKey.Parameter.of(k7, v7), ConfigurationKey.Parameter.of(k8, v8), ConfigurationKey.Parameter.of(k9, v9), ConfigurationKey.Parameter.of(k10, v10)));
	}

	@Override
	public final E withParameters(ConfigurationKey.Parameter... parameters) throws IllegalArgumentException {
		return this.withParameters(parameters != null ? Arrays.asList(parameters) : List.of());
	}

	@Override
	public abstract E withParameters(List<ConfigurationKey.Parameter> parameters) throws IllegalArgumentException;
}
