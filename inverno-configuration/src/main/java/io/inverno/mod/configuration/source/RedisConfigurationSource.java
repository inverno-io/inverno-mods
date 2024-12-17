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
package io.inverno.mod.configuration.source;

import io.inverno.mod.base.converter.ConverterException;
import io.inverno.mod.base.converter.JoinablePrimitiveEncoder;
import io.inverno.mod.base.converter.SplittablePrimitiveDecoder;
import io.inverno.mod.configuration.ConfigurationKey;
import io.inverno.mod.configuration.ConfigurationProperty;
import io.inverno.mod.configuration.ConfigurationQuery;
import io.inverno.mod.configuration.ConfigurationQueryResult;
import io.inverno.mod.configuration.ConfigurationUpdate;
import io.inverno.mod.configuration.ConfigurationUpdateResult;
import io.inverno.mod.configuration.DefaultableConfigurationSource;
import io.inverno.mod.configuration.DefaultingStrategy;
import io.inverno.mod.configuration.ExecutableConfigurationQuery;
import io.inverno.mod.configuration.ExecutableConfigurationUpdate;
import io.inverno.mod.configuration.ListConfigurationQuery;
import io.inverno.mod.configuration.internal.AbstractConfigurableConfigurationSource;
import io.inverno.mod.configuration.internal.GenericConfigurationKey;
import io.inverno.mod.configuration.internal.GenericConfigurationProperty;
import io.inverno.mod.configuration.internal.GenericConfigurationQueryResult;
import io.inverno.mod.configuration.internal.GenericConfigurationUpdateResult;
import io.inverno.mod.configuration.internal.JavaStringConverter;
import io.inverno.mod.configuration.internal.parser.option.ConfigurationOptionParser;
import io.inverno.mod.configuration.internal.parser.option.ParseException;
import io.inverno.mod.configuration.internal.parser.option.StringProvider;
import io.inverno.mod.redis.RedisClient;
import io.inverno.mod.redis.operations.EntryOptional;
import io.inverno.mod.redis.operations.RedisKeyReactiveOperations;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A configurable configuration source that stores and looks up properties in a Redis data store.
 * </p>
 * 
 * <p>
 * Configuration are stored as string entries, the property key is of the form: {@code keyPrefix ":PROP:" propertyName "[" [ key "=" value [ "," key "=" value ]* "]"}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class RedisConfigurationSource extends AbstractConfigurableConfigurationSource<RedisConfigurationSource.RedisConfigurationQuery, RedisConfigurationSource.RedisExecutableConfigurationQuery, RedisConfigurationSource.RedisListConfigurationQuery, RedisConfigurationSource.RedisConfigurationUpdate, RedisConfigurationSource.RedisExecutableConfigurationUpdate, String, RedisConfigurationSource> implements DefaultableConfigurationSource {
	
	/**
	 * The default key prefix.
	 */
	public static final String DEFAULT_KEY_PREFIX = "CONF";
	
	private static final Logger LOGGER = LogManager.getLogger(RedisConfigurationSource.class);
	
	private final RedisClient<String, String> redisClient;
	private final DefaultingStrategy defaultingStrategy;
	
	private String keyPrefix;
	private String propertyKeyPrefix;
	
	/**
	 * <p>
	 * Creates a redis configuration source with the specified redis client.
	 * </p>
	 * 
	 * @param redisClient a redis client
	 */
	public RedisConfigurationSource(RedisClient<String, String> redisClient) {
		this(redisClient, new JavaStringConverter(), new JavaStringConverter());
	}
	
	/**
	 * <p>
	 * Creates a redis configuration source with the specified redis client, string value encoder and decoder.
	 * </p>
	 *
	 * @param redisClient a redis client
	 * @param encoder     a string encoder
	 * @param decoder     a string decoder
	 */
	public RedisConfigurationSource(RedisClient<String, String> redisClient, JoinablePrimitiveEncoder<String> encoder, SplittablePrimitiveDecoder<String> decoder) {
		super(encoder, decoder);
		this.redisClient = redisClient;
		this.defaultingStrategy = DefaultingStrategy.noOp();
		this.setKeyPrefix(DEFAULT_KEY_PREFIX);
	}
	
	/**
	 * <p>
	 * Creates a redis configuration source from the specified original source which applies the specified default parameters and uses the specified defaulting strategy.
	 * </p>
	 *
	 * @param original          the original configuration source.
	 * @param defaultParameters the default parameters to apply
	 */
	private RedisConfigurationSource(RedisConfigurationSource original, List<ConfigurationKey.Parameter> defaultParameters) {
		this(original, defaultParameters, original.defaultingStrategy);
	}

	/**
	 * <p>
	 * Creates a redis configuration source from the specified original source which uses the specified defaulting strategy.
	 * </p>
	 *
	 * @param original           the original configuration source.
	 * @param defaultingStrategy a defaulting strategy
	 */
	private RedisConfigurationSource(RedisConfigurationSource original, DefaultingStrategy defaultingStrategy) {
		this(original, original.defaultParameters, defaultingStrategy);
	}

	/**
	 * <p>
	 * 	 * Creates a redis configuration source from the specified original source which applies the specified default parameters and uses the specified defaulting strategy.
	 * 	 * </p>
	 *
	 * @param original           the original configuration source.
	 * @param defaultParameters the default parameters to apply
	 * @param defaultingStrategy a defaulting strategy
	 */
	private RedisConfigurationSource(RedisConfigurationSource original, List<ConfigurationKey.Parameter> defaultParameters, DefaultingStrategy defaultingStrategy) {
		super(original, defaultParameters);
		this.redisClient = original.redisClient;
		this.defaultingStrategy = defaultingStrategy;
		this.setKeyPrefix(original.keyPrefix);
	}

	/**
	 * <p>
	 * Returns the prefix that is prepended to configuration property keys.
	 * </p>
	 * 
	 * <p>
	 * A configuration property key is of the form: {@code keyPrefix ":PROP:" propertyName "[" [ key "=" value [ "," key "=" value ]* "]"}.
	 * </p>
	 * 
	 * @return the key prefix
	 */
	public final String getKeyPrefix() {
		return keyPrefix;
	}

	/**
	 * <p>
	 * Sets the prefix that is prepended to configuration property keys.
	 * </p>
	 * 
	 * <p>
	 * A configuration property key is of the form: {@code keyPrefix ":PROP:" propertyName "[" [ key "=" value [ "," key "=" value ]* "]"}.
	 * </p>
	 * 
	 * @param keyPrefix the key prefix to set
	 */
	public final void setKeyPrefix(String keyPrefix) {
		this.keyPrefix = StringUtils.isNotBlank(keyPrefix) ? keyPrefix : DEFAULT_KEY_PREFIX;
		this.propertyKeyPrefix = keyPrefix + ":PROP:";
	}

	@Override
	public RedisConfigurationSource withParameters(List<ConfigurationKey.Parameter> parameters) throws IllegalArgumentException {
		return new RedisConfigurationSource(this, parameters);
	}

	@Override
	public RedisConfigurationSource withDefaultingStrategy(DefaultingStrategy defaultingStrategy) {
		return new RedisConfigurationSource(this, defaultingStrategy);
	}

	@Override
	public RedisConfigurationSource unwrap() {
		RedisConfigurationSource current = this;
		while(current.original != null) {
			current = current.original;
		}
		return current;
	}

	@Override
	public RedisExecutableConfigurationQuery get(String... names) throws IllegalArgumentException {
		return new RedisConfigurationSource.RedisExecutableConfigurationQuery(this).and().get(names);
	}

	@Override
	public RedisListConfigurationQuery list(String name) throws IllegalArgumentException {
		return new RedisConfigurationSource.RedisListConfigurationQuery(this, name);
	}

	@Override
	public RedisExecutableConfigurationUpdate set(Map<String, Object> values) throws IllegalArgumentException {
		return new RedisConfigurationSource.RedisExecutableConfigurationUpdate(this).and().set(values);
	}
	
	/**
	 * <p>
	 * The configuration query used by the Redis configuration source.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 * 
	 * @see ConfigurationQuery
	 */
	public static class RedisConfigurationQuery implements ConfigurationQuery<RedisConfigurationQuery, RedisConfigurationSource.RedisExecutableConfigurationQuery> {

		private final RedisConfigurationSource.RedisExecutableConfigurationQuery executableQuery;
		
		private final List<String> names;
		
		private List<ConfigurationKey.Parameter> parameters;
		
		private RedisConfigurationQuery(RedisConfigurationSource.RedisExecutableConfigurationQuery executableQuery) {
			this.executableQuery = executableQuery;
			this.names = new LinkedList<>();
		}
		
		@Override
		public RedisConfigurationSource.RedisExecutableConfigurationQuery get(String... names) throws IllegalArgumentException {
			if(names == null || names.length == 0) {
				throw new IllegalArgumentException("You can't query an empty list of configuration properties");
			}
			this.names.addAll(Arrays.asList(names));
			return this.executableQuery;
		}
	}
	
	/**
	 * <p>
	 * The executable configuration query used by the Redis configuration source.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 * 
	 * @see ExecutableConfigurationQuery
	 */
	public static class RedisExecutableConfigurationQuery implements ExecutableConfigurationQuery<RedisConfigurationSource.RedisConfigurationQuery, RedisExecutableConfigurationQuery> {

		private final RedisConfigurationSource source;
		
		private final LinkedList<RedisConfigurationQuery> queries;
		
		private RedisExecutableConfigurationQuery(RedisConfigurationSource source) {
			this.source = source;
			this.queries = new LinkedList<>();
		}
		
		@Override
		public RedisExecutableConfigurationQuery withParameters(List<ConfigurationKey.Parameter> parameters) throws IllegalArgumentException {
			RedisConfigurationSource.RedisConfigurationQuery currentQuery = this.queries.peekLast();
			currentQuery.parameters = new LinkedList<>(this.source.defaultParameters);
			if(parameters != null && !parameters.isEmpty()) {
				Set<String> parameterKeys = new HashSet<>();
				List<String> duplicateParameters = new LinkedList<>();
				for(ConfigurationKey.Parameter parameter : parameters) {
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
		public RedisConfigurationQuery and() {
			RedisConfigurationQuery nextQuery = new RedisConfigurationQuery(this);
			nextQuery.parameters = this.source.defaultParameters;
			this.queries.add(nextQuery);
			return nextQuery;
		}
		
		@Override
		public Flux<ConfigurationQueryResult> execute() {
			List<RedisConfigurationQueryResult> results = this.queries.stream()
				.flatMap(query -> query.names.stream().map(name -> new GenericConfigurationKey(name, query.parameters)))
				.map(queryKey -> {
					List<ConfigurationKey> defaultingKeys;
					if(this.source.defaultingStrategy != null) {
						defaultingKeys = this.source.defaultingStrategy.getDefaultingKeys(queryKey);
					}
					else {
						defaultingKeys = List.of(queryKey);
					}
					return new RedisConfigurationQueryResult(queryKey, defaultingKeys);
				})
				.collect(Collectors.toList());
			
			return this.source.redisClient
				.mget(keys -> results.stream().flatMap(result -> result.defaultingKeys.stream()).map(this.source::asPropertyKey).forEach(keys::key))
				.zipWith(Flux.fromIterable(results).flatMap(result -> Mono.just(result).repeat(result.counter - 1)))
				.mapNotNull(tuple -> {
					EntryOptional<String, String> entry = tuple.getT1();
					RedisConfigurationQueryResult result = tuple.getT2();
					
					int remaining = result.counter--;
					if(result.resolved) {
						return null;
					}
					ConfigurationKey queryKey = result.defaultingKeys.get(result.defaultingKeys.size() - remaining);
					if(entry.getValue().isPresent()) {
						try {
							new ConfigurationOptionParser<RedisConfigurationSource>(new StringProvider(entry.getValue().get())).StartValue()
								.ifSetOrElse(
									actualValue -> result.setResult(new GenericConfigurationProperty<>(queryKey, actualValue, this.source)),
									() -> result.setResult(new GenericConfigurationProperty<>(queryKey, this.source)) // unset
								);
						}
						catch (ParseException e) {
							result.setError(this.source, new IllegalStateException("Invalid value found for key " + queryKey.toString(), e));
						}
						return result;
					}
					if(remaining == 1) {
						// empty result
						return result;
					}
					return null;
				});
		}
	}
	
	private static class RedisConfigurationQueryResult extends GenericConfigurationQueryResult {
	
		private final List<ConfigurationKey> defaultingKeys;
		
		private int counter;
		
		private boolean resolved;
		
		private RedisConfigurationQueryResult(ConfigurationKey queryKey, List<ConfigurationKey> defaultingKeys) {
			super(queryKey, null);
			this.defaultingKeys = defaultingKeys;
			this.counter = this.defaultingKeys.size();
		}
		
		private void setResult(ConfigurationProperty queryResult) {
			this.queryResult = queryResult;
			this.resolved = true;
		}
		
		private void setError(RedisConfigurationSource errorSource, Throwable error) {
			this.errorSource = errorSource;
			this.error = error;
			this.resolved = true;
		}
	}
	
	/**
	 * <p>
	 * The configuration update used by the Redis configuration source.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 * 
	 * @see ConfigurationUpdate
	 */
	public static class RedisConfigurationUpdate implements ConfigurationUpdate<RedisConfigurationUpdate, RedisConfigurationSource.RedisExecutableConfigurationUpdate> {

		private final RedisExecutableConfigurationUpdate executableQuery;
		
		private Map<String, Object> values;
		
		private List<ConfigurationKey.Parameter> parameters;
		
		private RedisConfigurationUpdate(RedisExecutableConfigurationUpdate executableQuery) {
			this.executableQuery = executableQuery;
			this.values = Map.of();
		}
		
		@Override
		public RedisExecutableConfigurationUpdate set(Map<String, Object> values) {
			if(values == null || values.isEmpty()) {
				throw new IllegalArgumentException("You can't update an empty list of configuration properties");
			}
			this.values = Collections.unmodifiableMap(values);
			return this.executableQuery;
		}
	}
	
	/**
	 * <p>
	 * The executable configuration update used by the Redis configuration source.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 * 
	 * @see ExecutableConfigurationUpdate
	 */
	public static class RedisExecutableConfigurationUpdate implements ExecutableConfigurationUpdate<RedisConfigurationSource.RedisConfigurationUpdate, RedisExecutableConfigurationUpdate> {

		private final RedisConfigurationSource source;
		
		private final LinkedList<RedisConfigurationUpdate> updates;
		
		private RedisExecutableConfigurationUpdate(RedisConfigurationSource source) {
			this.source = source;
			this.updates = new LinkedList<>();
		}
		
		@Override
		public RedisExecutableConfigurationUpdate withParameters(List<ConfigurationKey.Parameter> parameters) throws IllegalArgumentException {
			RedisConfigurationSource.RedisConfigurationUpdate currentQuery = this.updates.peekLast();
			currentQuery.parameters = new LinkedList<>(this.source.defaultParameters);
			if(parameters != null && !parameters.isEmpty()) {
				Set<String> parameterKeys = new HashSet<>();
				List<String> duplicateParameters = new LinkedList<>();
				for(ConfigurationKey.Parameter parameter : parameters) {
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
		public RedisConfigurationUpdate and() {
			RedisConfigurationUpdate nextUpdate = new RedisConfigurationUpdate(this);
			nextUpdate.parameters = this.source.defaultParameters;
			this.updates.add(nextUpdate);
			return nextUpdate;
		}

		@Override
		public Flux<ConfigurationUpdateResult> execute() {
			return Flux.from(this.source.redisClient.batch(operations -> Flux.fromStream(this.updates.stream())
				.flatMap(update -> Flux.fromStream(update.values.entrySet().stream())
					.map(valueEntry -> {
						GenericConfigurationKey updateKey = new GenericConfigurationKey(valueEntry.getKey(), update.parameters);

						String redisKey = this.source.asPropertyKey(updateKey);
						if(valueEntry.getValue() == null) {
							// delete
							return operations.del(redisKey)
								.map(reply -> new GenericConfigurationUpdateResult(updateKey))
								.onErrorResume(error -> Mono.just(new GenericConfigurationUpdateResult(updateKey, this.source, error)));
						}
						if(valueEntry.getValue() instanceof ConfigurationUpdate.SpecialValue) {
							// unset
							return operations.set(redisKey, valueEntry.getValue().toString().toLowerCase())
								.map(reply -> {
									if(!reply.equalsIgnoreCase("OK")) {
										return new GenericConfigurationUpdateResult(updateKey, this.source, new IllegalStateException("Error setting key " + updateKey + ": " + reply));
									}
									return new GenericConfigurationUpdateResult(updateKey);
								})
								.onErrorResume(error -> Mono.just(new GenericConfigurationUpdateResult(updateKey, this.source, error)));
						} 
						else {
							try {
								return operations.set(redisKey, "\"" + this.source.encoder.encode(valueEntry.getValue()) + "\"")
									.map(reply -> {
										if(!reply.equalsIgnoreCase("OK")) {
											return new GenericConfigurationUpdateResult(updateKey, this.source, new IllegalStateException("Error setting key " + updateKey + ": " + reply));
										}
										return new GenericConfigurationUpdateResult(updateKey);
									})
									.onErrorResume(error -> Mono.just(new GenericConfigurationUpdateResult(updateKey, this.source, error)));
							}
							catch (ConverterException e) {
								return Mono.just(new GenericConfigurationUpdateResult(updateKey, this.source, new IllegalStateException("Error setting key " + updateKey, e)));
							}
						}
					})
				)
			));
		}
	}
	
	/**
	 * <p>
	 * The list configuration query used by the Redis configuration source.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	public static class RedisListConfigurationQuery implements ListConfigurationQuery<RedisListConfigurationQuery> {

		private final RedisConfigurationSource source;
		
		private final String name;
		
		private List<ConfigurationKey.Parameter> parameters;

		private RedisListConfigurationQuery(RedisConfigurationSource source, String name) {
			this.source = source;
			this.name = name;
			this.parameters = this.source.defaultParameters;
		}
		
		
		@Override
		public RedisListConfigurationQuery withParameters(List<ConfigurationKey.Parameter> parameters) throws IllegalArgumentException {
			this.parameters = new LinkedList<>(this.source.defaultParameters);
			if(parameters != null && !parameters.isEmpty()) {
				Set<String> parameterKeys = new HashSet<>();
				List<String> duplicateParameters = new LinkedList<>();
				for(ConfigurationKey.Parameter parameter : parameters) {
					this.parameters.add(parameter);
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
		public Flux<ConfigurationProperty> execute() {
			return this.execute(true);
		}

		@Override
		public Flux<ConfigurationProperty> executeAll() {
			return this.execute(false);
		}

		private Flux<ConfigurationProperty> execute(boolean exact) {
			String propertiesPattern = this.source.propertyKeyPrefix + this.name + "*";
			List<ConfigurationKey> defaultingMatchingKeys = this.source.defaultingStrategy.getListDefaultingKeys(new GenericConfigurationKey(this.name, this.parameters));
			
			return Flux.from(this.source.redisClient.connection(operations -> operations.scan()
				.pattern(propertiesPattern)
				.count(100)
				.build("0")
				.expand(result -> {
					if(result.isFinished()) {
						return Mono.empty();
					}
					return operations.scan()
						.pattern(propertiesPattern)
						.count(100)
						.build(result.getCursor());
				})
				.flatMapIterable(RedisKeyReactiveOperations.KeyScanResult::getKeys)
				.mapNotNull(rawKey -> {
					try {
						ConfigurationOptionParser<?> parser = new ConfigurationOptionParser<>(new StringProvider(rawKey.substring(10)));
						ConfigurationKey key = parser.StartKey();
						return new GenericConfigurationKey(key.getName(), key.getParameters());
					}
					catch (ParseException e) {
						LOGGER.warn(() -> "Ignoring invalid key " + rawKey, e);
						return null;
					}
				})
				.filter(key -> {
					boolean currentExact = exact;
					for(ConfigurationKey matchingKey : defaultingMatchingKeys) {
						if(key.matches(matchingKey, currentExact)) {
							return true;
						}
						// we only want to include extra parameters for the query key
						currentExact = true;
					}
					return false;
				})
				.buffer(100)
				.flatMap(queryKeys -> operations.mget(keys -> queryKeys.stream().map(this.source::asPropertyKey).forEach(keys::key)).zipWithIterable(queryKeys))
				.mapNotNull(result -> {
					GenericConfigurationKey	queryKey = result.getT2();
					return result.getT1().getValue()
						.map(value -> {
							try {
								return new ConfigurationOptionParser<RedisConfigurationSource>(new StringProvider(value)).StartValue()
									.map(actualValue -> new GenericConfigurationProperty<>(queryKey, actualValue, this.source))
									.orElseGet(() -> new GenericConfigurationProperty<>(queryKey, this.source)); // unset
							}
							catch (ParseException e) {
								LOGGER.warn(() -> "Ignoring invalid value found for key " + queryKey, e);
							}
							return null;
						})
						.orElse(null);
				})
			));
		}
	}
	
	/**
	 * <p>
	 * Converts the specified configuration key to a Redis property key.
	 * </p>
	 *
	 * @param key a configuration key
	 *
	 * @return a Redis property key
	 */
	private String asPropertyKey(ConfigurationKey key) {
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(this.propertyKeyPrefix).append(key.getName());
		if(key.getParameters() != null && !key.getParameters().isEmpty()) {
			keyBuilder
				.append("[")
					.append(key.getParameters().stream()
					.sorted(Comparator.comparing(ConfigurationKey.Parameter::getKey))
					.map(ConfigurationKey.Parameter::toString)
					.collect(Collectors.joining(","))
				)
				.append("]");
		}
		return keyBuilder.toString();
	}
}
