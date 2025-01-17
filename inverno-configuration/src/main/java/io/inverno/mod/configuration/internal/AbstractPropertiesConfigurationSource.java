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
import io.inverno.mod.configuration.ConfigurationKey.Parameter;
import io.inverno.mod.configuration.ConfigurationProperty;
import io.inverno.mod.configuration.ConfigurationQuery;
import io.inverno.mod.configuration.ConfigurationQueryResult;
import io.inverno.mod.configuration.ConfigurationSource;
import io.inverno.mod.configuration.ExecutableConfigurationQuery;
import io.inverno.mod.configuration.ListConfigurationQuery;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
public abstract class AbstractPropertiesConfigurationSource<A, B extends AbstractPropertiesConfigurationSource<A,B>>
	extends
		AbstractConfigurationSource<AbstractPropertiesConfigurationSource.PropertyConfigurationQuery<A, B>,	AbstractPropertiesConfigurationSource.PropertyExecutableConfigurationQuery<A, B>, AbstractPropertiesConfigurationSource.PropertyListConfigurationQuery<A, B>, A, B> {
	
	/**
	 * <p>
	 * Creates a properties configuration source with the specified decoder and property accessor.
	 * </p>
	 *
	 * @param decoder          a value decoder
	 *
	 * @throws NullPointerException if the specified decoder is null
	 */
	public AbstractPropertiesConfigurationSource(SplittablePrimitiveDecoder<A> decoder) {
		super(decoder);
	}
	
	/**
	 * <p>
	 * Returns the value of the property identified by the specified name.
	 * </p>
	 * 
	 * <p>
	 * This methods should rely on an underlying synchronous property accessor.
	 * </p>
	 * 
	 * @param name the configuration property name
	 * 
	 * @return an optional returning the configuration property value, or an empty optional if there's no value defined for the specified key
	 */
	protected abstract Optional<A> getPropertyValue(String name);
	
	/**
	 * <p>
	 * Returns the list of property names managed by the source.
	 * </p>
	 * 
	 * @return a set of configuration property names
	 */
	protected abstract Set<String> listProperties();
	
	@Override
	public PropertyExecutableConfigurationQuery<A, B> get(String... names) throws IllegalArgumentException {
		return new PropertyExecutableConfigurationQuery<>(this).and().get(names);
	}

	@Override
	public PropertyListConfigurationQuery<A, B> list(String name) throws IllegalArgumentException {
		return new PropertyListConfigurationQuery<>(this, name);
	}

	protected AbstractPropertiesConfigurationSource(B original, List<Parameter> defaultParameters) {
		super(original, defaultParameters);
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

		private final PropertyExecutableConfigurationQuery<A, B> executableQuery;

		private final List<String> names;

		private List<Parameter> parameters;
		
		private PropertyConfigurationQuery(PropertyExecutableConfigurationQuery<A, B> executableQuery) {
			this.executableQuery = executableQuery;
			this.names = new LinkedList<>();
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
	public static class PropertyExecutableConfigurationQuery<A, B extends AbstractPropertiesConfigurationSource<A, B>> implements ExecutableConfigurationQuery<PropertyConfigurationQuery<A, B>, PropertyExecutableConfigurationQuery<A, B>> {

		private final B source;
		
		private final LinkedList<PropertyConfigurationQuery<A, B>> queries;
		
		@SuppressWarnings("unchecked")
		private PropertyExecutableConfigurationQuery(AbstractPropertiesConfigurationSource<A, B> source) {
			this.source = (B)source;
			this.queries = new LinkedList<>();
		}
		
		@Override
		public PropertyExecutableConfigurationQuery<A, B> withParameters(List<Parameter> parameters) throws IllegalArgumentException {
			PropertyConfigurationQuery<A, B> currentQuery = this.queries.peekLast();
			currentQuery.parameters = new LinkedList<>(this.source.defaultParameters);
			if(parameters != null && !parameters.isEmpty()) {
				Set<String> parameterKeys = new HashSet<>();
				List<String> duplicateParameters = new LinkedList<>();
				for(Parameter parameter : parameters) {
					if(parameter.isWildcard() || parameter.isUndefined()) {
						throw new IllegalArgumentException("Query parameter can not be undefined or a wildcard: " + parameter);
					}
					currentQuery.parameters.add(parameter);
					if(!parameterKeys.add(parameter.getKey())) {
						duplicateParameters.add(parameter.getKey());
					}
				}
				if(!duplicateParameters.isEmpty()) {
					throw new IllegalArgumentException("The following parameters were specified more than once: " + String.join(", ", duplicateParameters));
				}
			}
			return this;
		}
		
		@Override
		public PropertyConfigurationQuery<A, B> and() {
			PropertyConfigurationQuery<A, B> nextQuery = new PropertyConfigurationQuery<>(this);
			nextQuery.parameters = this.source.defaultParameters;
			this.queries.add(nextQuery);
			return nextQuery;
		}

		@Override
		public Flux<ConfigurationQueryResult> execute() {
			return Flux.fromStream(this.queries.stream().flatMap(query -> query.names.stream().map(name -> new GenericConfigurationKey(name, query.parameters)))
				.map(key -> new PropertyConfigurationQueryResult(key,
						this.source.getPropertyValue(key.getName())
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
	 */
	public static class PropertyConfigurationQueryResult extends GenericConfigurationQueryResult {

		private PropertyConfigurationQueryResult(ConfigurationKey queryKey, ConfigurationProperty queryResult) {
			super(queryKey, queryResult);
		}
	}
	
	/**
	 * <p>
	 * The list configuration query used by a properties configuration source.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @see ListConfigurationQuery
	 *
	 * @param <A> raw configuration value type
	 * @param <B> the properties configuration source type
	 */
	public static class PropertyListConfigurationQuery<A, B extends AbstractPropertiesConfigurationSource<A, B>> implements ListConfigurationQuery<PropertyListConfigurationQuery<A, B>> {

		private final B source;
		
		private final String name;
		
		@SuppressWarnings("unchecked")
		private PropertyListConfigurationQuery(AbstractPropertiesConfigurationSource<A, B> source, String name) {
			this.source = (B)source;
			this.name = name;
		}
		
		@Override
		public PropertyListConfigurationQuery<A, B> withParameters(List<Parameter> parameters) throws IllegalArgumentException {
			// parameters are ignored
			return this;
		}

		@Override
		public Flux<ConfigurationProperty> execute() {
			return Flux.fromStream(() -> this.source.listProperties().stream().filter(name -> name.equals(this.name)).map(name -> new GenericConfigurationProperty<ConfigurationKey, B, A>(new GenericConfigurationKey(name), this.source.getPropertyValue(name).get(), this.source)));
		}

		@Override
		public Flux<ConfigurationProperty> executeAll() {
			return Flux.fromStream(() -> this.source.listProperties().stream().filter(name -> name.equals(this.name)).map(name -> new GenericConfigurationProperty<ConfigurationKey, B, A>(new GenericConfigurationKey(name), this.source.getPropertyValue(name).get(), this.source)));
		}
	}
}
