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

import io.inverno.mod.base.converter.SplittablePrimitiveDecoder;
import io.inverno.mod.configuration.ConfigurationKey.Parameter;
import io.inverno.mod.configuration.internal.GenericConfigurationKey;
import io.inverno.mod.configuration.internal.GenericConfigurationQueryResult;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Base implementation for {@link ConfigurationSource} where configuration properties are resolved using a hash code of a {@link ConfigurationKey} corresponding to a {@link HashConfigurationQuery}.
 * </p>
 *
 * <p>
 * This implementation is intended for configuration sources whose data can be loaded in-memory typically as a hash table (eg. command line parameters, property files...).
 * </p>
 *
 * <p>
 * Implementors must implement the {@link AbstractHashConfigurationSource#load()} method which is called to load the configuration properties in memory.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see ConfigurationSource
 *
 * @param <A> raw configuration value type
 * @param <B> the hash configuration source type
 */
public abstract class AbstractHashConfigurationSource<A, B extends AbstractHashConfigurationSource<A, B>> extends AbstractConfigurationSource<AbstractHashConfigurationSource.HashConfigurationQuery<A, B>, AbstractHashConfigurationSource.HashExecutableConfigurationQuery<A, B>, AbstractHashConfigurationSource.HashListConfigurationQuery<A, B>, A> implements DefaultableConfigurationSource<AbstractHashConfigurationSource.HashConfigurationQuery<A, B>, AbstractHashConfigurationSource.HashExecutableConfigurationQuery<A, B>, AbstractHashConfigurationSource.HashListConfigurationQuery<A, B>, B> {
	
	/**
	 * The defaulting strategy.
	 */
	protected final DefaultingStrategy defaultingStrategy;
	
	/**
	 * The initial configuration source (i.e. before setting defaulting strategy).
	 */
	protected B initial;
	
	/**
	 * <p>
	 * Creates a hash configuration source with the specified decoder and noop defaulting strategy.
	 * </p>
	 * 
	 * @param decoder a splittable primitive decoder
	 * 
	 * @throws NullPointerException if the specified decoder is null
	 */
	public AbstractHashConfigurationSource(SplittablePrimitiveDecoder<A> decoder) {
		this(decoder, DefaultingStrategy.noOp());
	}

	/**
	 * <p>
	 * Creates a hash configuration source with the specified decoder and defaulting strategy.
	 * </p>
	 * 
	 * @param decoder a splittable primitive decoder
	 * @param defaultingStrategy a defaulting strategy
	 */
	protected AbstractHashConfigurationSource(SplittablePrimitiveDecoder<A> decoder, DefaultingStrategy defaultingStrategy) {
		super(decoder);
		this.defaultingStrategy = defaultingStrategy;
	}
	
	/**
	 * <p>
	 * Creates a hash configuration source from the specified initial source and using the specified defaulting strategy.
	 * </p>
	 *
	 * @param initial            the initial configuration source.
	 * @param defaultingStrategy a defaulting strategy
	 */
	protected AbstractHashConfigurationSource(B initial, DefaultingStrategy defaultingStrategy) {
		super(initial.decoder);
		this.initial = initial;
		this.defaultingStrategy = defaultingStrategy;
	}

	@Override
	@SuppressWarnings("unchecked")
	public B unwrap() {
		return this.initial != null ? this.initial : (B)this;
	}
	
	/**
	 * <p>
	 * Loads the configuration properties.
	 * </p>
	 * 
	 * @return A mono emitting the list of configuration properties
	 */
	protected abstract Mono<List<ConfigurationProperty>> load();
	
	@Override
	public HashExecutableConfigurationQuery<A, B> get(String... names) throws IllegalArgumentException {
		return new HashExecutableConfigurationQuery<>(this).and().get(names);
	}

	@Override
	public HashListConfigurationQuery<A, B> list(String name) throws IllegalArgumentException {
		return new HashListConfigurationQuery<>(this, name);
	}
	
	/**
	 * <p>
	 * The configuration query used by the a configuration source.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see ConfigurationQuery
	 *
	 * @param <A> raw configuration value type
	 * @param <B> the hash configuration source type
	 */
	public static class HashConfigurationQuery<A, B extends AbstractHashConfigurationSource<A, B>> implements ConfigurationQuery<HashConfigurationQuery<A, B>, HashExecutableConfigurationQuery<A, B>> {

		private final HashExecutableConfigurationQuery<A, B> executableQuery;
		
		private final List<String> names;
		
		private final LinkedList<Parameter> parameters;
		
		private HashConfigurationQuery(HashExecutableConfigurationQuery<A, B> executableQuery) {
			this.executableQuery = executableQuery;
			this.names = new LinkedList<>();
			this.parameters = new LinkedList<>();
		}
		
		@Override
		public HashExecutableConfigurationQuery<A, B> get(String... names) {
			if(names == null || names.length == 0) {
				throw new IllegalArgumentException("You can't query an empty list of configuration properties");
			}
			this.names.addAll(Arrays.asList(names));
			return this.executableQuery;
		}
	}

	/**
	 * <p>
	 * The executable configuration query used by a hash configuration source.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see ExecutableConfigurationQuery
	 *
	 * @param <A> raw configuration value type
	 * @param <B> the hash configuration source type
	 */
	public static class HashExecutableConfigurationQuery<A, B extends AbstractHashConfigurationSource<A, B>> implements ExecutableConfigurationQuery<HashConfigurationQuery<A, B>, HashExecutableConfigurationQuery<A, B>> {
		
		private final B source;
		
		private final LinkedList<HashConfigurationQuery<A, B>> queries;
		
		@SuppressWarnings("unchecked")
		private HashExecutableConfigurationQuery(AbstractHashConfigurationSource<A, B> source) {
			this.source = (B)source;
			this.queries = new LinkedList<>();
		}

		@Override
		public HashConfigurationQuery<A, B> and() {
			this.queries.add(new HashConfigurationQuery<>(this));
			return this.queries.peekLast();
		}
		
		@Override
		public HashExecutableConfigurationQuery<A, B> withParameters(List<Parameter> parameters) throws IllegalArgumentException {
			HashConfigurationQuery<A, B> currentQuery = this.queries.peekLast();
			currentQuery.parameters.clear();
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
					throw new IllegalArgumentException("The following parameters were specified more than once: " + duplicateParameters.stream().collect(Collectors.joining(", ")));
				}
			}
			return this;
		}

		@Override
		public Flux<ConfigurationQueryResult> execute() {
			return this.source.load()
				.map(properties -> properties.stream().collect(Collectors.toMap(property -> new GenericConfigurationKey(property.getKey().getName(), property.getKey().getParameters()), Function.identity())))
				.flatMapMany(indexedProperties -> Flux.<ConfigurationQueryResult>fromStream(this.queries.stream()
					.flatMap(query -> query.names.stream().map(name -> new GenericConfigurationKey(name, query.parameters)))
					.map(key -> {
						for(ConfigurationKey currentKey : this.source.defaultingStrategy.getDefaultingKeys(key)) {
							GenericConfigurationKey genericCurrentKey;
							if(currentKey instanceof GenericConfigurationKey) {
								genericCurrentKey = (GenericConfigurationKey)currentKey;
							}
							else {
								genericCurrentKey = new GenericConfigurationKey(currentKey.getName(), currentKey.getParameters());
							}
							
							ConfigurationProperty currentProperty = indexedProperties.get(genericCurrentKey);
							if(currentProperty != null) {
								return new HashConfigurationQueryResult<A, B>(key, currentProperty);
							}
						}
						return new HashConfigurationQueryResult<A, B>(key, null);
					})
				))
				.onErrorResume(ex -> Flux.<ConfigurationQueryResult>fromStream(this.queries.stream()
						.flatMap(query -> query.names.stream().map(name -> new HashConfigurationQueryResult<A, B>(new GenericConfigurationKey(name, query.parameters), this.source, ex)))
					)
				);
		}
	}
	
	/**
	 * <p>
	 * The configuration query result returned by a hash configuration source.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see ConfigurationQueryResult
	 *
	 * @param <A> raw configuration value type
	 * @param <B> the hash configuration source type
	 */
	public static class HashConfigurationQueryResult<A, B extends AbstractHashConfigurationSource<A, B>> extends GenericConfigurationQueryResult {

		private HashConfigurationQueryResult(ConfigurationKey queryKey, ConfigurationProperty queryResult) {
			super(queryKey, queryResult);
		}
		
		private HashConfigurationQueryResult(ConfigurationKey queryKey, B source, Throwable error) {
			super(queryKey, source, error);
		}
	}
	
	/**
	 * <p>
	 * The list configuration query used by a hash configuration source.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @see ListConfigurationQuery
	 *
	 * @param <A> raw configuration value type
	 * @param <B> the hash configuration source type
	 */
	public static class HashListConfigurationQuery<A, B extends AbstractHashConfigurationSource<A, B>> implements ListConfigurationQuery<HashListConfigurationQuery<A, B>> {

		private final B source;
		
		private final String name;
		
		private final LinkedList<Parameter> parameters;
		
		@SuppressWarnings("unchecked")
		private HashListConfigurationQuery(AbstractHashConfigurationSource<A, B> source, String name) {
			this.source = (B)source;
			this.name = name;
			this.parameters = new LinkedList<>();
		}
		
		@Override
		public HashListConfigurationQuery<A, B> withParameters(List<Parameter> parameters) throws IllegalArgumentException {
			this.parameters.clear();
			if(parameters != null && !parameters.isEmpty()) {
				Set<String> parameterKeys = new HashSet<>();
				List<String> duplicateParameters = new LinkedList<>();
				for(Parameter parameter : parameters) {
					this.parameters.add(parameter);
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
		public Flux<ConfigurationProperty> execute() {
			return this.execute(true);
		}

		@Override
		public Flux<ConfigurationProperty> executeAll() {
			return this.execute(false);
		}
		
		private Flux<ConfigurationProperty> execute(boolean exact) {
			return Flux.defer(() -> {
				List<ConfigurationKey> defaultingMatchingKeys = this.source.defaultingStrategy.getListDefaultingKeys(new GenericConfigurationKey(this.name, this.parameters));

				return this.source.load()
					.flatMapMany(properties -> Flux.fromStream(properties.stream().filter(property -> { 
						boolean currentExact = exact;
						for(ConfigurationKey machingKey : defaultingMatchingKeys) {
							if(property.getKey().matches(machingKey, currentExact)) {
								return true;
							}
							// we only want to include extra parameters for the query key
							currentExact = true;
						}
						return false;
					})));
			});
		}
	}
}
