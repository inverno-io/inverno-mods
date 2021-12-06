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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.inverno.mod.base.converter.SplittablePrimitiveDecoder;
import io.inverno.mod.configuration.ConfigurationKey.Parameter;
import io.inverno.mod.configuration.internal.GenericConfigurationKey;
import io.inverno.mod.configuration.internal.GenericConfigurationProperty;
import io.inverno.mod.configuration.internal.GenericConfigurationQueryResult;
import reactor.core.publisher.Flux;

/**
 * <p>
 * Base implementation for {@link ConfigurationSource} where configuration properties are resolved using a property accessor function to retrieve property values.
 * </p>
 *
 * <p>
 * This implementation is intended for configuration sources whose properties are uniquely identified by a single key such as system properties, system environment variables or maps in general.
 * </p>
 *
 * <p>
 * As a result, parameterized query are not supported with this kind of configuration source, regardless of the parameters specified when building a query, only the configuration key name is
 * considered when resolving a value.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see ConfigurationSource
 *
 * @param <A> raw configuration value type
 * @param <B> the properties configuration source type
 */
public abstract class AbstractPropertiesConfigurationSource<A, B extends AbstractPropertiesConfigurationSource<A,B>> extends AbstractConfigurationSource<AbstractPropertiesConfigurationSource.PropertyConfigurationQuery<A, B>, AbstractPropertiesConfigurationSource.PropertyExecutableConfigurationQuery<A, B>, A> {

	/**
	 * The property accessor.
	 */
	protected Function<String, A> propertyAccessor;
	
	/**
	 * <p>
	 * Creates a properties configuration source with the specified decoder and property accessor.
	 * </p>
	 *
	 * @param decoder          a value decoder
	 * @param propertyAccessor a property accessor
	 *
	 * @throws NullPointerException if the specified decoder is null
	 */
	public AbstractPropertiesConfigurationSource(SplittablePrimitiveDecoder<A> decoder, Function<String, A> propertyAccessor) {
		super(decoder);
		this.propertyAccessor = propertyAccessor;
	}
	
	@Override
	public PropertyExecutableConfigurationQuery<A, B> get(String... names) throws IllegalArgumentException {
		return new PropertyExecutableConfigurationQuery<>(this).and().get(names);
	}
	
	/**
	 * <p>
	 * The configuration query used by a properties configuration source.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see ConfigurationQuery
	 *
	 * @param <A> raw configuration value type
	 * @param <B> the properties configuration source type
	 */
	public static class PropertyConfigurationQuery<A, B extends AbstractPropertiesConfigurationSource<A,B>> implements ConfigurationQuery<PropertyConfigurationQuery<A, B>, PropertyExecutableConfigurationQuery<A, B>> {
		
		private List<String> names;
		
		private LinkedList<Parameter> parameters;
		
		private PropertyExecutableConfigurationQuery<A, B> executableQuery;
		
		private PropertyConfigurationQuery(PropertyExecutableConfigurationQuery<A, B> executableQuery) {
			this.executableQuery = executableQuery;
			this.names = new LinkedList<>();
			this.parameters = new LinkedList<>();
		}
		
		@Override
		public PropertyExecutableConfigurationQuery<A, B> get(String... names) throws IllegalArgumentException {
			if(names == null || names.length == 0) {
				throw new IllegalArgumentException("You can't query an empty list of configuration properties");
			}
			this.names.addAll(Arrays.asList(names));
			return this.executableQuery;
		}
	}
	
	/**
	 * <p>
	 * The executable configuration query used by a properties configuration source.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see ExecutableConfigurationQuery
	 *
	 * @param <A> raw configuration value type
	 * @param <B> the properties configuration source type
	 */
	public static class PropertyExecutableConfigurationQuery<A, B extends AbstractPropertiesConfigurationSource<A,B>> implements ExecutableConfigurationQuery<PropertyConfigurationQuery<A, B>, PropertyExecutableConfigurationQuery<A, B>> {

		private B source;
		
		private LinkedList<PropertyConfigurationQuery<A, B>> queries;
		
		@SuppressWarnings("unchecked")
		private PropertyExecutableConfigurationQuery(AbstractPropertiesConfigurationSource<A, B> source) {
			this.source = (B)source;
			this.queries = new LinkedList<>();
		}
		
		@Override
		public PropertyExecutableConfigurationQuery<A, B> withParameters(Parameter... parameters) throws IllegalArgumentException {
			if(parameters != null && parameters.length > 0) {
				PropertyConfigurationQuery<A, B> currentQuery = this.queries.peekLast();
				Set<String> parameterKeys = new HashSet<>();
				currentQuery.parameters.clear();
				List<String> duplicateParameters = new LinkedList<>();
				for(Parameter parameter : parameters) {
					currentQuery.parameters.add(parameter);
					if(!parameterKeys.add(parameter.getKey())) {
						duplicateParameters.add(parameter.getKey());
					}
				}
				if(!duplicateParameters.isEmpty()) {
					throw new IllegalArgumentException("The following parameters were specified more than once: " + duplicateParameters.stream().collect(Collectors.joining(", ")));
				}
			}
			return this;
		}
		
		@Override
		public PropertyConfigurationQuery<A, B> and() {
			this.queries.add(new PropertyConfigurationQuery<>(this));
			return this.queries.peekLast();
		}

		@Override
		public Flux<ConfigurationQueryResult> execute() {
			return Flux.fromStream(this.queries.stream().flatMap(query -> query.names.stream().map(name -> new GenericConfigurationKey(name, query.parameters)))
				.map(key -> new PropertyConfigurationQueryResult<>(key, 
						Optional.ofNullable(this.source.propertyAccessor.apply(key.getName()))
						.map(value -> new GenericConfigurationProperty<ConfigurationKey, B, A>(key, value, this.source))
						.orElse(null)
					)
				)
			);
		}
	}
	
	/**
	 * <p>
	 * The configuration query result returned by a properties configuration source.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see ConfigurationQueryResult
	 *
	 * @param <A> raw configuration value type
	 * @param <B> the properties configuration source type
	 */
	public static class PropertyConfigurationQueryResult<A, B extends AbstractPropertiesConfigurationSource<A,B>> extends GenericConfigurationQueryResult {

		private PropertyConfigurationQueryResult(ConfigurationKey queryKey, ConfigurationProperty queryResult) {
			super(queryKey, queryResult);
		}
	}
}
