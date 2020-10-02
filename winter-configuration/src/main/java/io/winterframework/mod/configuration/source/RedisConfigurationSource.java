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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.lettuce.core.Limit;
import io.lettuce.core.Range;
import io.lettuce.core.Range.Boundary;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.winterframework.mod.configuration.AbstractConfigurableConfigurationSource;
import io.winterframework.mod.configuration.ConfigurationKey;
import io.winterframework.mod.configuration.ConfigurationKey.Parameter;
import io.winterframework.mod.configuration.ConfigurationProperty;
import io.winterframework.mod.configuration.ConfigurationQuery;
import io.winterframework.mod.configuration.ConfigurationSource;
import io.winterframework.mod.configuration.ConfigurationUpdate;
import io.winterframework.mod.configuration.ExecutableConfigurationQuery;
import io.winterframework.mod.configuration.ExecutableConfigurationUpdate;
import io.winterframework.mod.configuration.ValueCodecException;
import io.winterframework.mod.configuration.ValueDecoder;
import io.winterframework.mod.configuration.ValueEncoder;
import io.winterframework.mod.configuration.codec.StringValueDecoder;
import io.winterframework.mod.configuration.codec.StringValueEncoder;
import io.winterframework.mod.configuration.internal.GenericConfigurationKey;
import io.winterframework.mod.configuration.internal.GenericConfigurationProperty;
import io.winterframework.mod.configuration.internal.GenericConfigurationQueryResult;
import io.winterframework.mod.configuration.internal.GenericConfigurationUpdateResult;
import io.winterframework.mod.configuration.internal.parser.option.ConfigurationOptionParser;
import io.winterframework.mod.configuration.internal.parser.option.ParseException;
import io.winterframework.mod.configuration.internal.parser.option.StringProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
public class RedisConfigurationSource extends AbstractConfigurableConfigurationSource<RedisConfigurationSource.RedisConfigurationQuery, RedisConfigurationSource.RedisExecutableConfigurationQuery, RedisConfigurationSource.RedisConfigurationQueryResult, RedisConfigurationSource.RedisConfigurationUpdate, RedisConfigurationSource.RedisExecutableConfigurationUpdate, RedisConfigurationSource.RedisConfigurationUpdateResult, String> {

	private static final String METADATA_FIELD_ACTIVE_REVISION = "active_revision";
	private static final String METADATA_FIELD_WORKING_REVISION = "working_revision";
	
	private RedisReactiveCommands<String, String> commands;
	
	public RedisConfigurationSource(RedisClient redisClient) {
		this(redisClient, new StringValueEncoder(), new StringValueDecoder());
	}
	
	public RedisConfigurationSource(RedisClient redisClient, ValueEncoder<String> encoder, ValueDecoder<String> decoder) {
		super(encoder, decoder);
		this.commands = redisClient.connect().reactive();
	}
	
	@Override
	public RedisExecutableConfigurationQuery get(String... names) throws IllegalArgumentException {
		return new RedisExecutableConfigurationQuery(this).and().get(names);
	}
	
	@Override
	public RedisExecutableConfigurationUpdate set(Map<String, Object> values) throws IllegalArgumentException {
		return new RedisExecutableConfigurationUpdate(this).and().set(values);
	}
	
	// This should be cached
	private Mono<List<List<String>>> getMetaDataParameterSets() {
		return this.commands.smembers(META_DATA_CONTROL_KEY)
			.map(value -> Arrays.stream(value.split(",")).filter(s -> !s.isEmpty()).collect(Collectors.toList()))
			.collectList()
			.defaultIfEmpty(List.of());
	}
	
	// - metaParametersSets should be cached (save 1 roundtrip to redis)
	// - the whole method could also be cached (save n round trip to redis where n is the number of sets for which parameters.containsAll() is true)
	// - actually individuals calls to hgetall should be cached as well 
	private Mono<Map<String, String>> getMetaData(List<Parameter> parameters, List<List<String>> metaDataParameterSets) {
		Map<String, Parameter> parametersByKey = parameters.stream().collect(Collectors.toMap(Parameter::getKey, Function.identity()));
		return Flux.fromStream(metaDataParameterSets.stream()
				.filter(set -> parametersByKey.keySet().containsAll(set))
				.map(parametersSet -> parametersSet.stream().map(parametersByKey::get).collect(Collectors.toList()))
			)
			.groupBy(queryParameters -> queryParameters.size())
			.sort(Collections.reverseOrder(Comparator.comparing(cardGroup -> cardGroup.key())))
			.flatMap(cardGroup -> cardGroup
				.flatMap(queryParameters -> {
					String metaDataKey = asMetaDataKey(queryParameters);
					return this.commands.hgetall(metaDataKey)
						.filter(metaData -> !metaData.isEmpty())
						.doOnNext(metaData -> {
							if(!metaData.containsKey(METADATA_FIELD_WORKING_REVISION)) {
								throw new IllegalStateException("Invalid meta data found for key " + metaDataKey + ": Missing " + METADATA_FIELD_WORKING_REVISION);
							}
						});
				})
				.singleOrEmpty()
				.onErrorMap(IndexOutOfBoundsException.class, ex -> new IllegalStateException("Conflict has been detected")) // TODO make this conflict explicit what are the problematic metadata
			)
			.next();
	}
	
	public Mono<Integer> getActiveRevision(Parameter... parameters) throws IllegalArgumentException {
		Set<String> parameterKeys = new HashSet<>();
		List<String> duplicateParameters = new LinkedList<>();
		for(Parameter parameter : parameters) {
			if(!parameterKeys.add(parameter.getKey())) {
				duplicateParameters.add(parameter.getKey());
			}
		}
		if(duplicateParameters != null && duplicateParameters.size() > 0) {
			throw new IllegalArgumentException("The following parameters were specified more than once: " + duplicateParameters.stream().collect(Collectors.joining(", ")));
		}
		
		List<Parameter> parametersList = parameters != null ? Arrays.asList(parameters) : List.of();
		return this.getMetaDataParameterSets()
			.filter(metaDataParameterSets -> !metaDataParameterSets.isEmpty())
			.flatMap(metaDataParameterSets -> this.getMetaData(parametersList, metaDataParameterSets))
			.filter(metaData -> metaData.containsKey(METADATA_FIELD_ACTIVE_REVISION))
			.map(metaData -> Integer.parseInt(metaData.get(METADATA_FIELD_ACTIVE_REVISION)));
	}
	
	public Mono<Integer> getWorkingRevision(Parameter... parameters) throws IllegalArgumentException {
		Set<String> parameterKeys = new HashSet<>();
		List<String> duplicateParameters = new LinkedList<>();
		for(Parameter parameter : parameters) {
			if(!parameterKeys.add(parameter.getKey())) {
				duplicateParameters.add(parameter.getKey());
			}
		}
		if(duplicateParameters != null && duplicateParameters.size() > 0) {
			throw new IllegalArgumentException("The following parameters were specified more than once: " + duplicateParameters.stream().collect(Collectors.joining(", ")));
		}
		
		List<Parameter> parametersList = parameters != null ? Arrays.asList(parameters) : List.of();
		return this.getMetaDataParameterSets()
			.filter(metaDataParameterSets -> !metaDataParameterSets.isEmpty())
			.flatMap(metaDataParameterSets -> this.getMetaData(parametersList, metaDataParameterSets))
			.map(metaData -> Integer.parseInt(metaData.get(METADATA_FIELD_WORKING_REVISION)));
	}
	
	public Mono<Void> activate(Parameter... parameters) throws IllegalArgumentException {
		Set<String> parameterKeys = new HashSet<>();
		List<String> duplicateParameters = new LinkedList<>();
		for(Parameter parameter : parameters) {
			if(!parameterKeys.add(parameter.getKey())) {
				duplicateParameters.add(parameter.getKey());
			}
		}
		if(duplicateParameters != null && duplicateParameters.size() > 0) {
			throw new IllegalArgumentException("The following parameters were specified more than once: " + duplicateParameters.stream().collect(Collectors.joining(", ")));
		}
		
		List<Parameter> parametersList = parameters != null ? Arrays.asList(parameters) : List.of();
		String metaDataKey = asMetaDataKey(parametersList);
		return this.getMetaDataParameterSets()
			.filter(metaDataParameterSets -> !metaDataParameterSets.isEmpty())
			.flatMap(metaDataParameterSets -> this.getMetaData(parametersList, metaDataParameterSets))
			.defaultIfEmpty(Map.of(METADATA_FIELD_WORKING_REVISION, "1"))
			.flatMap(metaData -> {
				int workingRevision = Integer.parseInt(metaData.get(METADATA_FIELD_WORKING_REVISION));
				return this.commands.multi()
					.flatMap(multiResponse -> {
						this.commands.sadd(META_DATA_CONTROL_KEY, parametersList.stream().map(Parameter::getKey).sorted().collect(Collectors.joining(","))).subscribe(); // TODO Parameter key should be a valid Java identifier, idem for property name actually
						this.commands.hset(metaDataKey, Map.of(METADATA_FIELD_ACTIVE_REVISION, Integer.toString(workingRevision))).subscribe();
						this.commands.hset(metaDataKey, Map.of(METADATA_FIELD_WORKING_REVISION, Integer.toString(workingRevision + 1))).subscribe(); // We always set the working revision since metadata might not exist for the specified parameters
						return this.commands.exec();
					})
					.map(transactionResult -> {
						if(transactionResult.wasDiscarded()) {
							return Mono.error(new RuntimeException("Error activating revision " + workingRevision + " for key " + metaDataKey + ": Transaction was discarded"));
						}
						return Mono.empty();
					});
			})
			.then();
	}
	
	public Mono<Void> activate(int revision, Parameter... parameters) throws IllegalArgumentException {
		if(revision < 1) {
			throw new IllegalArgumentException("Revision must be a positive integer");
		}
		Set<String> parameterKeys = new HashSet<>();
		List<String> duplicateParameters = new LinkedList<>();
		for(Parameter parameter : parameters) {
			if(!parameterKeys.add(parameter.getKey())) {
				duplicateParameters.add(parameter.getKey());
			}
		}
		if(duplicateParameters != null && duplicateParameters.size() > 0) {
			throw new IllegalArgumentException("The following parameters were specified more than once: " + duplicateParameters.stream().collect(Collectors.joining(", ")));
		}
		
		List<Parameter> parametersList = parameters != null ? Arrays.asList(parameters) : List.of();
		String metaDataKey = asMetaDataKey(parametersList);
		return this.getMetaDataParameterSets()
			.filter(metaDataParameterSets -> !metaDataParameterSets.isEmpty())
			.flatMap(metaDataParameterSets -> this.getMetaData(parametersList, metaDataParameterSets))
			.defaultIfEmpty(Map.of(METADATA_FIELD_WORKING_REVISION, "1"))
			.flatMap(metaData -> {
				Integer activeRevision = metaData.containsKey(METADATA_FIELD_ACTIVE_REVISION) ? Integer.parseInt(metaData.get(METADATA_FIELD_ACTIVE_REVISION)) : null;
				int workingRevision = Integer.parseInt(metaData.get(METADATA_FIELD_WORKING_REVISION));
				if(revision > workingRevision) {
					return Mono.error(new IllegalArgumentException("The revision to activate: " + revision + " can't be greater than the current working revision: " + workingRevision));
				}
				else if(activeRevision != null && activeRevision == revision) {
					return Mono.empty();
				}
				else {
					return this.commands.multi()
						.flatMap(multiResponse -> {
							this.commands.sadd(META_DATA_CONTROL_KEY, parametersList.stream().map(Parameter::getKey).sorted().collect(Collectors.joining(","))).subscribe(); // TODO Parameter key should be a valid Java identifier, idem for property name actually
							this.commands.hset(metaDataKey, Map.of(METADATA_FIELD_ACTIVE_REVISION, Integer.toString(revision))).subscribe();
							this.commands.hset(metaDataKey, Map.of(METADATA_FIELD_WORKING_REVISION, Integer.toString(revision == workingRevision ? workingRevision + 1 : workingRevision))).subscribe(); // We always set the working revision since metadata might not exist for the specified parameters
							return this.commands.exec();
						})
						.map(transactionResult -> {
							if(transactionResult.wasDiscarded()) {
								return Mono.error(new RuntimeException("Error activating revision " + revision + " for key " + metaDataKey + ": Transaction was discarded"));
							}
							return Mono.empty();
						});
				}
			})
			.then();
	}
	
	public static class RedisConfigurationKey extends GenericConfigurationKey {

		private Integer revision;
		
		private RedisConfigurationKey(String name, Integer revision) {
			this(name, revision, null);
		}
		
		private RedisConfigurationKey(String name, Integer revision, Collection<Parameter> parameters) {
			super(name, parameters);
			this.revision = revision;
		}
		
		public Integer getRevision() {
			return revision;
		}
	}
	
	public static class RedisConfigurationQuery implements ConfigurationQuery<RedisConfigurationQuery, RedisExecutableConfigurationQuery, RedisConfigurationQueryResult> {

		private RedisExecutableConfigurationQuery executableQuery;
		
		private List<String> names;
		
		private LinkedList<Parameter> parameters;
		
		private Integer revision;
		
		private boolean managed;
		
		private RedisConfigurationQuery(RedisExecutableConfigurationQuery executableQuery) {
			this.executableQuery = executableQuery;
			this.names = new LinkedList<>();
			this.parameters = new LinkedList<>();
		}
		
		@Override
		public RedisExecutableConfigurationQuery get(String... names) throws IllegalArgumentException {
			if(names == null || names.length == 0) {
				throw new IllegalArgumentException("You can't query an empty list of configuration properties");
			}
			this.names.addAll(Arrays.asList(names));
			return this.executableQuery;
		}
	}
	
	public static class RedisExecutableConfigurationQuery implements ExecutableConfigurationQuery<RedisConfigurationQuery, RedisExecutableConfigurationQuery, RedisConfigurationQueryResult> {

		private RedisConfigurationSource source;
		
		private LinkedList<RedisConfigurationQuery> queries;
		
		private RedisExecutableConfigurationQuery(RedisConfigurationSource source) {
			this.source = source;
			this.queries = new LinkedList<>();
		}
		
		@Override
		public RedisConfigurationQuery and() {
			this.queries.add(new RedisConfigurationQuery(this));
			return this.queries.peekLast();
		}

		@Override
		public RedisExecutableConfigurationQuery withParameters(Parameter... parameters) throws IllegalArgumentException {
			if(parameters != null && parameters.length > 0) {
				RedisConfigurationQuery currentQuery = this.queries.peekLast();
				Set<String> parameterKeys = new HashSet<>();
				currentQuery.parameters.clear();
				List<String> duplicateParameters = new LinkedList<>();
				for(Parameter parameter : parameters) {
					currentQuery.parameters.add(parameter);
					if(!parameterKeys.add(parameter.getKey())) {
						duplicateParameters.add(parameter.getKey());
					}
				}
				if(duplicateParameters != null && duplicateParameters.size() > 0) {
					throw new IllegalArgumentException("The following parameters were specified more than once: " + duplicateParameters.stream().collect(Collectors.joining(", ")));
				}
			}
			return this;
		}
		
		public RedisExecutableConfigurationQuery atRevision(int revision) throws IllegalArgumentException {
			if(revision < 1) {
				throw new IllegalArgumentException("Revision must be a positive integer");
			}
			RedisConfigurationQuery currentQuery = this.queries.peekLast();
			currentQuery.revision = revision;
			return this;
		}
		
		@Override
		public Flux<RedisConfigurationQueryResult> execute() {
			// TODO decouple metadata retrieval from queries to optimize
			return this.source.getMetaDataParameterSets()
				.filter(metaDataParameterSets -> !metaDataParameterSets.isEmpty())
				.flatMapMany(metaDataParameterSets -> Flux.fromStream(this.queries.stream())
					.flatMap(query -> {
						if(query.revision != null) {
							return Mono.empty();
						}
						return this.source.getMetaData(query.parameters, metaDataParameterSets)
							.doOnNext(metaData -> {
								query.managed = true;
								if(metaData.containsKey(METADATA_FIELD_ACTIVE_REVISION)) {
									query.revision = Integer.parseInt(metaData.get(METADATA_FIELD_ACTIVE_REVISION));
								}
							});
					})
				)
				.thenMany(Flux.fromStream(this.queries.stream()))
				.flatMap(query -> Flux.fromStream(query.names.stream().map(name -> new RedisConfigurationKey(name, query.revision, query.parameters)))
					.flatMap(queryKey -> {
						if(query.managed && query.revision == null) {
							// property is managed (ie. we found metadata) but there's no active or selected revision
							return Mono.just(new RedisConfigurationQueryResult(queryKey, null));
						}
						return this.source.commands
							.zrevrangebyscoreWithScores(asPropertyKey(queryKey), Range.from(Boundary.including(0), queryKey.revision != null ? Boundary.including(queryKey.revision) : Boundary.unbounded()), Limit.create(0, 1))
							.next()
							.map(result -> Optional.of(result))
							.defaultIfEmpty(Optional.empty())
							.map(result -> result.map(value -> {
									try {
										Optional<String> actualValue = new ConfigurationOptionParser<RedisConfigurationSource>(new StringProvider(value.getValue())).StartValueRevision();
										if(actualValue != null) {
											return new RedisConfigurationQueryResult(queryKey, new GenericConfigurationProperty<ConfigurationKey, RedisConfigurationSource, String>(new RedisConfigurationKey(queryKey.getName(), (int)value.getScore(), queryKey.getParameters()), actualValue.orElse(null), this.source));
										}
										else {
											// unset
											return new RedisConfigurationQueryResult(queryKey, new GenericConfigurationProperty<ConfigurationKey, RedisConfigurationSource, String>(new RedisConfigurationKey(queryKey.getName(), (int)value.getScore(), queryKey.getParameters()), this.source));
										}
									} 
									catch (ParseException e) {
										return new RedisConfigurationQueryResult(queryKey, this.source, new IllegalStateException("Invalid value found for key " + queryKey.toString() + " at revision " + (int)value.getScore(), e));
									}
								})
								.orElse(new RedisConfigurationQueryResult(queryKey, (ConfigurationProperty<?, ?>)null))
							);
					})
				);
		}
	}
	
	public static class RedisConfigurationQueryResult extends GenericConfigurationQueryResult<RedisConfigurationKey, ConfigurationProperty<?, ?>> {
		
		private RedisConfigurationQueryResult(RedisConfigurationKey queryKey, ConfigurationProperty<?, ?> queryResult) {
			super(queryKey, queryResult);
		}

		private RedisConfigurationQueryResult(RedisConfigurationKey queryKey, RedisConfigurationSource source, Throwable error) {
			super(queryKey, source, error);
		}
	}
	
	public static class RedisConfigurationUpdate implements ConfigurationUpdate<RedisConfigurationUpdate, RedisExecutableConfigurationUpdate, RedisConfigurationUpdateResult> {

		private RedisExecutableConfigurationUpdate executableQuery;
		
		private Map<String, Object> values;
		
		private LinkedList<Parameter> parameters;
		
		private Integer workingRevision;
		
		private RedisConfigurationUpdate(RedisExecutableConfigurationUpdate executableQuery) {
			this.executableQuery = executableQuery;
			this.values = Map.of();
			this.parameters = new LinkedList<>();
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
	
	public static class RedisExecutableConfigurationUpdate implements ExecutableConfigurationUpdate<RedisConfigurationUpdate, RedisExecutableConfigurationUpdate, RedisConfigurationUpdateResult> {

		private RedisConfigurationSource source;
		
		private LinkedList<RedisConfigurationUpdate> queries;
		
		private RedisExecutableConfigurationUpdate(RedisConfigurationSource source) {
			this.source = source;
			this.queries = new LinkedList<>();
		}
		
		@Override
		public RedisConfigurationUpdate and() {
			this.queries.add(new RedisConfigurationUpdate(this));
			return this.queries.peekLast();
		}
		
		@Override
		public RedisExecutableConfigurationUpdate withParameters(Parameter... parameters) throws IllegalArgumentException {
			if(parameters != null && parameters.length > 0) {
				RedisConfigurationUpdate currentQuery = this.queries.peekLast();
				Set<String> parameterKeys = new HashSet<>();
				currentQuery.parameters.clear();
				List<String> duplicateParameters = new LinkedList<>();
				for(Parameter parameter : parameters) {
					currentQuery.parameters.add(parameter);
					if(!parameterKeys.add(parameter.getKey())) {
						duplicateParameters.add(parameter.getKey());
					}
				}
				if(duplicateParameters != null && duplicateParameters.size() > 0) {
					throw new IllegalArgumentException("The following parameters were specified more than once: " + duplicateParameters.stream().collect(Collectors.joining(", ")));
				}
			}
			return this;
		}

		@Override
		public Flux<RedisConfigurationUpdateResult> execute() {
			return this.source.getMetaDataParameterSets()
				.filter(metaDataParameterSets -> !metaDataParameterSets.isEmpty())
				.flatMapMany(metaDataParameterSets -> Flux.fromStream(this.queries.stream())
					.flatMap(query -> this.source.getMetaData(query.parameters, metaDataParameterSets)
						.doOnNext(metaData -> {
							query.workingRevision = Integer.parseInt(metaData.get(METADATA_FIELD_WORKING_REVISION));
						})
					)
				)
				.thenMany(Flux.fromStream(this.queries.stream()))
				.flatMap(query -> Flux.fromStream(query.values.entrySet().stream())
					.flatMap(valueEntry -> {
						RedisConfigurationKey updateKey = new RedisConfigurationKey(valueEntry.getKey(), query.workingRevision != null ? query.workingRevision : 1, query.parameters);
						
						String redisKey = asPropertyKey(updateKey);
						String redisEncodedValue;
						try {
							redisEncodedValue = updateKey.revision + "{\"" + this.source.encoder.from(valueEntry.getValue()) + "\"}";
						} 
						catch (ValueCodecException e) {
							return Mono.just(new RedisConfigurationUpdateResult(updateKey, this.source, new IllegalStateException("Error setting key " + updateKey.toString() + " at revision " + updateKey.revision, e)));
						}
						
						this.source.commands.multi().subscribe();
						this.source.commands.zremrangebyscore(redisKey, Range.from(Boundary.including(updateKey.revision), Boundary.including(updateKey.revision))).subscribe();
						this.source.commands.zadd(redisKey, updateKey.revision, redisEncodedValue).subscribe();
						
						return this.source.commands.exec()
							.map(transactionResult -> {
								if(transactionResult.wasDiscarded()) {
									return new RedisConfigurationUpdateResult(updateKey, this.source, new RuntimeException("Error setting key " + updateKey.toString() + " at revision " + updateKey.revision + ": Transaction was discarded"));
								}
								return new RedisConfigurationUpdateResult(updateKey);
							});
					})
				);
		}
	}
	
	public static class RedisConfigurationUpdateResult extends GenericConfigurationUpdateResult<RedisConfigurationKey> {

		private RedisConfigurationUpdateResult(RedisConfigurationKey updateKey) {
			super(updateKey);
		}
		
		private RedisConfigurationUpdateResult(RedisConfigurationKey updateKey, ConfigurationSource<?, ?, ?> source, Throwable error) {
			super(updateKey, source, error);
		}
	}
	
	private static final String META_DATA_CONTROL_KEY = "CONF:META:CTRL";
	
	private static String asMetaDataKey(List<Parameter> parameters) {
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append("CONF:META:");
		if(parameters != null && !parameters.isEmpty()) {
			keyBuilder
				.append("[")
					.append(parameters.stream()
					.sorted(Comparator.comparing(Parameter::getKey))
					.map(Parameter::toString)
					.collect(Collectors.joining(","))
				)
				.append("]");
		}
		return keyBuilder.toString();
	}
	
	private static String asPropertyKey(ConfigurationKey key) {
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append("CONF:PROP:").append(key.getName());
		if(key.getParameters() != null && !key.getParameters().isEmpty()) {
			keyBuilder
				.append("[")
					.append(key.getParameters().stream()
					.sorted(Comparator.comparing(Parameter::getKey))
					.map(Parameter::toString)
					.collect(Collectors.joining(","))
				)
				.append("]");
		}
		return keyBuilder.toString();
	}
	
	public Mono<Integer> getActiveRevision(String k1, Object v1) {
		return this.getActiveRevision(Parameter.of(k1, v1));
	}

	public Mono<Integer> getActiveRevision(String k1, Object v1, String k2, Object v2) {
		return this.getActiveRevision(Parameter.of(k1, v1), Parameter.of(k2, v2));
	}
	
	public Mono<Integer> getActiveRevision(String k1, Object v1, String k2, Object v2, String k3, Object v3) {
		return this.getActiveRevision(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3));
	}
	
	public Mono<Integer> getActiveRevision(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) {
		return this.getActiveRevision(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4));
	}
	
	public Mono<Integer> getActiveRevision(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5) {
		return this.getActiveRevision(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5));
	}
	
	public Mono<Integer> getActiveRevision(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6) {
		return this.getActiveRevision(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6));
	}
	
	public Mono<Integer> getActiveRevision(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7) {
		return this.getActiveRevision(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7));
	}
	
	public Mono<Integer> getActiveRevision(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8) {
		return this.getActiveRevision(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8));
	}
	
	public Mono<Integer> getActiveRevision(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9) {
		return this.getActiveRevision(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8), Parameter.of(k9, v9));
	}
	
	public Mono<Integer> getActiveRevision(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9, String k10, Object v10) {
		return this.getActiveRevision(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8), Parameter.of(k9, v9), Parameter.of(k10, v10));
	}
	
	public Mono<Integer> getWorkingRevision(String k1, Object v1) {
		return this.getWorkingRevision(Parameter.of(k1, v1));
	}

	public Mono<Integer> getWorkingRevision(String k1, Object v1, String k2, Object v2) {
		return this.getWorkingRevision(Parameter.of(k1, v1), Parameter.of(k2, v2));
	}
	
	public Mono<Integer> getWorkingRevision(String k1, Object v1, String k2, Object v2, String k3, Object v3) {
		return this.getWorkingRevision(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3));
	}
	
	public Mono<Integer> getWorkingRevision(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) {
		return this.getWorkingRevision(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4));
	}
	
	public Mono<Integer> getWorkingRevision(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5) {
		return this.getWorkingRevision(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5));
	}
	
	public Mono<Integer> getWorkingRevision(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6) {
		return this.getWorkingRevision(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6));
	}
	
	public Mono<Integer> getWorkingRevision(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7) {
		return this.getWorkingRevision(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7));
	}
	
	public Mono<Integer> getWorkingRevision(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8) {
		return this.getWorkingRevision(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8));
	}
	
	public Mono<Integer> getWorkingRevision(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9) {
		return this.getWorkingRevision(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8), Parameter.of(k9, v9));
	}
	
	public Mono<Integer> getWorkingRevision(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9, String k10, Object v10) {
		return this.getWorkingRevision(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8), Parameter.of(k9, v9), Parameter.of(k10, v10));
	}
	
	public Mono<Void> activate(String k1, Object v1) {
		return this.activate(Parameter.of(k1, v1));
	}

	public Mono<Void> activate(String k1, Object v1, String k2, Object v2) {
		return this.activate(Parameter.of(k1, v1), Parameter.of(k2, v2));
	}
	
	public Mono<Void> activate(String k1, Object v1, String k2, Object v2, String k3, Object v3) {
		return this.activate(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3));
	}
	
	public Mono<Void> activate(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) {
		return this.activate(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4));
	}
	
	public Mono<Void> activate(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5) {
		return this.activate(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5));
	}
	
	public Mono<Void> activate(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6) {
		return this.activate(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6));
	}
	
	public Mono<Void> activate(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7) {
		return this.activate(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7));
	}
	
	public Mono<Void> activate(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8) {
		return this.activate(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8));
	}
	
	public Mono<Void> activate(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9) {
		return this.activate(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8), Parameter.of(k9, v9));
	}
	
	public Mono<Void> activate(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9, String k10, Object v10) {
		return this.activate(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8), Parameter.of(k9, v9), Parameter.of(k10, v10));
	}
	
	public Mono<Void> activate(int revision, String k1, Object v1) {
		return this.activate(revision, Parameter.of(k1, v1));
	}

	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2));
	}
	
	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2, String k3, Object v3) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3));
	}
	
	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4));
	}
	
	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5));
	}
	
	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6));
	}
	
	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7));
	}
	
	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8));
	}
	
	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8), Parameter.of(k9, v9));
	}
	
	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9, String k10, Object v10) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8), Parameter.of(k9, v9), Parameter.of(k10, v10));
	}
}
