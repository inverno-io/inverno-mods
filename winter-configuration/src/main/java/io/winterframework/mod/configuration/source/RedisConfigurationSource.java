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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.lettuce.core.KeyValue;
import io.lettuce.core.Limit;
import io.lettuce.core.Range;
import io.lettuce.core.Range.Boundary;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.winterframework.mod.base.converter.ConverterException;
import io.winterframework.mod.base.converter.JoinablePrimitiveEncoder;
import io.winterframework.mod.base.converter.SplittablePrimitiveDecoder;
import io.winterframework.mod.configuration.AbstractConfigurableConfigurationSource;
import io.winterframework.mod.configuration.ConfigurableConfigurationSource;
import io.winterframework.mod.configuration.ConfigurationKey;
import io.winterframework.mod.configuration.ConfigurationKey.Parameter;
import io.winterframework.mod.configuration.ConfigurationProperty;
import io.winterframework.mod.configuration.ConfigurationQuery;
import io.winterframework.mod.configuration.ConfigurationQueryResult;
import io.winterframework.mod.configuration.ConfigurationSource;
import io.winterframework.mod.configuration.ConfigurationUpdate;
import io.winterframework.mod.configuration.ConfigurationUpdate.SpecialValue;
import io.winterframework.mod.configuration.ConfigurationUpdateResult;
import io.winterframework.mod.configuration.ExecutableConfigurationQuery;
import io.winterframework.mod.configuration.ExecutableConfigurationUpdate;
import io.winterframework.mod.configuration.internal.GenericConfigurationKey;
import io.winterframework.mod.configuration.internal.GenericConfigurationProperty;
import io.winterframework.mod.configuration.internal.GenericConfigurationQueryResult;
import io.winterframework.mod.configuration.internal.GenericConfigurationUpdateResult;
import io.winterframework.mod.configuration.internal.JavaStringConverter;
import io.winterframework.mod.configuration.internal.parser.option.ConfigurationOptionParser;
import io.winterframework.mod.configuration.internal.parser.option.ParseException;
import io.winterframework.mod.configuration.internal.parser.option.StringProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A configurable configuration source that stores and looks up properties in a
 * Redis data store.
 * </p>
 * 
 * <p>
 * This implementation supports basic versioning which allows to set multiple
 * properties and activate or revert them atomically.
 * </p>
 * 
 * <p>
 * Properties can be viewed in a tree of properties whose nodes correspond to
 * parameters in natural order, a global revision is defined at the root of the
 * tree and finer revisions can also be created in child nodes to version
 * particular branches.
 * </p>
 * 
 * <p>
 * Particular care must be taken when deciding to version a specific branch, a
 * property can only be versioned once which is the case when versioned property
 * sets are disjointed. More specifically, a given property can't be versioned
 * twice which might happen when the configuration is activated with different
 * overlapping sets of parameters. An exception is normally thrown when such
 * situation is detected. On the other hand, it is quite possible to version
 * nested branches.
 * </p>
 * 
 * <p>
 * For instance, the following setup is most likely to fail if properties can be
 * defined with both {@code environment="production"} and {@code zone="eu"}
 * parameters:
 * </p>
 * 
 * <ul>
 * <li>{@code []}: global tree</li>
 * <li>{@code [environment="production"]}: production environment tree</li>
 * <li>{@code [zone="eu"]}: eu zone tree</li>
 * </ul>
 * 
 * <p>While the following setup will work just fine:</p>
 * 
 * <ul>
 * <li>{@code []}: global tree</li>
 * <li>{@code [environment="production"]}: production environment tree</li>
 * <li>{@code [environment="production", zone="eu"]}: eu zone tree</li>
 * </ul>
 * 
 * <p>
 * Properties are set for the working revision corresponding to their
 * parameters. The working revision is activated using the
 * {@link RedisConfigurationSource#activate(Parameter...)} method, the
 * {@link RedisConfigurationSource#activate(int, Parameter...)} is used to
 * activate a specific revision.
 * </p>
 * 
 * <p>
 * A typical workflow to set properties is:
 * </p>
 * 
 * <blockquote>
 * 
 * <pre>
 * RedisConfigurationSource source = ...;
 * source
 *     .set("db.url", "jdbc:oracle:thin:@dev.db.server:1521:sid").withParameters("env", "dev").and()
 *     .set("db.url", "jdbc:oracle:thin:@prod_eu.db.server:1521:sid").withParameters("env", "prod", "zone", "eu").and()
 *     .set("db.url", "jdbc:oracle:thin:@prod_us.db.server:1521:sid").withParameters("env", "prod", "zone", "us")
 *     .execute()
 *     .blockLast();
 * 
 * // Activate working revision globally
 * source.activate().block();
 * 
 * // Activate working revision for dev environment and prod environment independently
 * source.activate("env", "dev").block();
 * source.activate("env", "prod").block();
 * </pre>
 * 
 * </blockquote>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ConfigurableConfigurationSource
 */
public class RedisConfigurationSource extends AbstractConfigurableConfigurationSource<RedisConfigurationSource.RedisConfigurationQuery, RedisConfigurationSource.RedisExecutableConfigurationQuery, RedisConfigurationSource.RedisConfigurationQueryResult, RedisConfigurationSource.RedisConfigurationUpdate, RedisConfigurationSource.RedisExecutableConfigurationUpdate, RedisConfigurationSource.RedisConfigurationUpdateResult, String> {

	private static final String METADATA_FIELD_ACTIVE_REVISION = "active_revision";
	private static final String METADATA_FIELD_WORKING_REVISION = "working_revision";
	
	private RedisReactiveCommands<String, String> commands;
	
	/**
	 * <p>
	 * Creates a redis configuration source with the specified redis client.
	 * </p>
	 * 
	 * @param redisClient a redis client
	 */
	public RedisConfigurationSource(RedisClient redisClient) {
		this(redisClient, new JavaStringConverter(), new JavaStringConverter());
	}
	
	/**
	 * <p>
	 * Creates a redis configuration source with the specified redis client, string
	 * value encoder and decoder.
	 * </p>
	 * 
	 * @param redisClient a redis client
	 * @param encoder     a string encoder
	 * @param decoder     a string decoder
	 */
	public RedisConfigurationSource(RedisClient redisClient, JoinablePrimitiveEncoder<String> encoder, SplittablePrimitiveDecoder<String> decoder) {
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
	
	/**
	 * <p>
	 * Returns the list of metadata parameters.
	 * </p>
	 * 
	 * @return a mono emitting the list of metadata parameters
	 */
	protected Mono<List<List<String>>> getMetaDataParameterSets() {
		return this.commands.smembers(META_DATA_CONTROL_KEY)
			.map(value -> Arrays.stream(value.split(",")).filter(s -> !s.isEmpty()).collect(Collectors.toList()))
			.collectList();
	}
	
	/**
	 * <p>
	 * Returns the configuration metadata for the specified list of parameters
	 * extracted form a configuration query.
	 * </p>
	 * 
	 * @param metaDataParameters a list of parameters
	 * 
	 * @return a mono emitting the metadata
	 */
	protected Mono<RedisConfigurationMetaData> getMetaData(List<Parameter> metaDataParameters) {
		String metaDataKey = asMetaDataKey(metaDataParameters);
		return this.commands.hgetall(metaDataKey)
			.collectMap(KeyValue::getKey, KeyValue::getValue)
			.doOnNext(data -> {
				if(!data.containsKey(METADATA_FIELD_WORKING_REVISION)) {
					throw new IllegalStateException("Invalid meta data found for key " + metaDataKey + ": Missing " + METADATA_FIELD_WORKING_REVISION);
				}
			})
			.map(data -> new RedisConfigurationMetaData(metaDataParameters, data));
	}
	
	private Mono<RedisConfigurationMetaData> getMetaData(List<Parameter> parameters, List<List<String>> metaDataParameterSets) {
		Map<String, Parameter> parametersByKey = parameters.stream().collect(Collectors.toMap(Parameter::getKey, Function.identity()));
		return Flux.fromStream(metaDataParameterSets.stream()
				.filter(set -> parametersByKey.keySet().containsAll(set))
				.map(parametersSet -> parametersSet.stream().map(parametersByKey::get).collect(Collectors.toList()))
			)
			.groupBy(queryParameters -> queryParameters.size())
			.sort(Collections.reverseOrder(Comparator.comparing(cardGroup -> cardGroup.key())))
			.flatMap(cardGroup -> cardGroup
				.flatMap(queryParameters -> {
					return this.getMetaData(queryParameters);
				})
				.singleOrEmpty()
				.onErrorMap(IndexOutOfBoundsException.class, ex -> new IllegalStateException("A conflict of MetaData has been detected when considering parameters [" + parameters.stream().map(Parameter::toString).collect(Collectors.joining(", ")) + "]")) // TODO make this conflict explicit what are the problematic metadata
			)
			.next();
	}
	
	/**
	 * <p>
	 * Returns the configuration metadata for the specified list of parameters.
	 * </p>
	 * 
	 * @param parameters an array of parameters
	 * 
	 * @return a mono emitting the metadata
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<RedisConfigurationMetaData> getMetaData(Parameter... parameters) throws IllegalArgumentException {
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
			.flatMap(metaDataParameterSets -> this.getMetaData(parametersList, metaDataParameterSets));
	}
	
	/**
	 * <p>
	 * Activates the working revision for the properties defined with the specified parameters.
	 * </p>
	 * 
	 * @param parameters an array of parameters
	 * 
	 * @return a mono that completes when the operation succeeds or fails
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
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
			.flatMap(metaDataParameterSets -> this.getMetaData(parametersList, metaDataParameterSets))
			.defaultIfEmpty(new RedisConfigurationMetaData(null, 1))
			.flatMap(metaData -> {
				int workingRevision = metaData.getWorkingRevision().get();
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
	
	/**
	 * <p>
	 * Activates the specified revisions for the properties defined with the
	 * specified parameters.
	 * </p>
	 * 
	 * @param revision   the revision to activate
	 * @param parameters an array of parameters
	 * 
	 * @return a mono that completes when the operation succeeds or fails
	 * @throws IllegalArgumentException if parameters were specified more than once
	 *                                  or if the specified revision is greater than
	 *                                  the current working revision
	 */
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
			.flatMap(metaDataParameterSets -> this.getMetaData(parametersList, metaDataParameterSets))
			.defaultIfEmpty(new RedisConfigurationMetaData(null, 1))
			.flatMap(metaData -> {
				int workingRevision = metaData.getWorkingRevision().get();
				if(revision > workingRevision) {
					return Mono.error(new IllegalArgumentException("The revision to activate: " + revision + " can't be greater than the current working revision: " + workingRevision));
				}
				else if(metaData.getActiveRevision().filter(activeRevision -> activeRevision  == revision).isPresent()) {
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
	
	/**
	 * <p>
	 * Provides information about a particular configuration branch.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see RedisConfigurationSource
	 */
	public static class RedisConfigurationMetaData {
		
		private List<Parameter> parameters;
		
		private Optional<Integer> activeRevision;
		
		private Optional<Integer> workingRevision;
		
		private RedisConfigurationMetaData(List<Parameter> parameters, Map<String, String> data) throws IllegalArgumentException {
			this.parameters = parameters;
			this.activeRevision = Optional.ofNullable(data.get(METADATA_FIELD_ACTIVE_REVISION)).map(Integer::parseInt);
			this.workingRevision = Optional.ofNullable(data.get(METADATA_FIELD_WORKING_REVISION)).map(Integer::parseInt);
		}
		
		private RedisConfigurationMetaData(Integer activeRevision, Integer workingRevision) throws IllegalArgumentException {
			this.activeRevision = Optional.ofNullable(activeRevision);
			this.workingRevision = Optional.ofNullable(workingRevision);
		}
		
		/**
		 * <p>
		 * Returns the parameters representing a configuration branch.
		 * </p>
		 * 
		 * @return a list of parameters
		 */
		public List<Parameter> getParameters() {
			return this.parameters;
		}
		
		/**
		 * <p>
		 * Returns the working revision of the configuration branch.
		 * </p>
		 * 
		 * @return an optional returning the working revision, or an empty optional if
		 *         no working revision has been defined
		 */
		public Optional<Integer> getWorkingRevision() {
			return this.workingRevision;
		}
		
		/**
		 * <p>
		 * Returns the active revision of the configuration branch.
		 * </p>
		 * 
		 * @return an optional returning the active revision, or an empty optional if no
		 *         revision has been activated so far
		 */
		public Optional<Integer> getActiveRevision() {
			return this.activeRevision;
		}
	}
	
	/**
	 * <p>
	 * The configuration key used by the Redis configuration source.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see ConfigurationKey
	 */
	public static class RedisConfigurationKey extends GenericConfigurationKey {

		private Optional<RedisConfigurationMetaData> metaData;
		private Optional<Integer> revision;
		
		private RedisConfigurationKey(String name, RedisConfigurationMetaData metaData, Integer actualRevision) {
			this(name, metaData, actualRevision, null);
		}
		
		private RedisConfigurationKey(String name, RedisConfigurationMetaData metaData, Integer actualRevision, Collection<Parameter> parameters) {
			super(name, parameters);
			this.metaData = Optional.ofNullable(metaData);
			this.revision = Optional.ofNullable(actualRevision);
		}
		
		/**
		 * <p>
		 * Returns the meta data associated with the key.
		 * </p>
		 * 
		 * @return the meta data
		 */
		public Optional<RedisConfigurationMetaData> getMetaData() {
			return metaData;
		}
		
		/**
		 * <p>
		 * Returns revision of the property identified by the key.
		 * </p>
		 * 
		 * @return an optional returning the revision or an empty optional if there's no
		 *         revision
		 */
		public Optional<Integer> getRevision() {
			return this.revision;
		}
	}
	
	/**
	 * <p>
	 * The configuration query used by the Redis configuration source.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see ConfigurationQuery
	 */
	public static class RedisConfigurationQuery implements ConfigurationQuery<RedisConfigurationQuery, RedisExecutableConfigurationQuery, RedisConfigurationQueryResult> {

		private RedisExecutableConfigurationQuery executableQuery;
		
		private List<String> names;
		
		private LinkedList<Parameter> parameters;
		
		private RedisConfigurationMetaData metaData;
		
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
		
		private Optional<RedisConfigurationMetaData> getMetaData() {
			return Optional.ofNullable(this.metaData);
		}
	}
	
	/**
	 * <p>
	 * The executable configuration query used by the Redis configuration source.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see ExecutableConfigurationQuery
	 */
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
		
		/**
		 * <p>
		 * Specifies the revision (inclusive) up to which properties should be searched.
		 * </p>
		 * 
		 * @param revision a revision
		 * 
		 * @return the executable configuration query
		 * @throws IllegalArgumentException if the revision is invalid
		 */
		public RedisExecutableConfigurationQuery atRevision(int revision) throws IllegalArgumentException {
			if(revision < 1) {
				throw new IllegalArgumentException("Revision must be a positive integer");
			}
			RedisConfigurationQuery currentQuery = this.queries.peekLast();
			currentQuery.metaData = new RedisConfigurationMetaData(revision, null);
			return this;
		}
		
		@Override
		public Flux<RedisConfigurationQueryResult> execute() {
			return this.source.getMetaDataParameterSets()
				.flatMapMany(metaDataParameterSets -> {
					Map<Integer, Map<List<Parameter>, List<RedisConfigurationQuery>>> queriesByMetaDataByCard = new TreeMap<>(Comparator.reverseOrder());
					for(RedisConfigurationQuery query : this.queries) {
						if(query.metaData != null) {
							continue;
						}
						Map<String, Parameter> parametersByKey = query.parameters.stream().collect(Collectors.toMap(Parameter::getKey, Function.identity()));
						metaDataParameterSets.stream()
							.filter(set -> parametersByKey.keySet().containsAll(set))
							.map(parametersSet -> parametersSet.stream().map(parametersByKey::get).collect(Collectors.toList()))
							.forEach(metaKeyParameters -> {
								if(queriesByMetaDataByCard.get(metaKeyParameters.size()) == null) {
									queriesByMetaDataByCard.put(metaKeyParameters.size(), new HashMap<>());
								}
								Map<List<Parameter>, List<RedisConfigurationQuery>> currentCard = queriesByMetaDataByCard.get(metaKeyParameters.size());
								if(!currentCard.containsKey(metaKeyParameters)) {
									currentCard.put(metaKeyParameters, new ArrayList<>());
								}
								currentCard.get(metaKeyParameters).add(query);
							});
					}
					
					return Flux.fromIterable(queriesByMetaDataByCard.values())
						.concatMap(queriesByMetaData -> Mono.just(queriesByMetaData)
							.doOnSubscribe(subscription -> {
								queriesByMetaData.values().stream().forEach(queries -> {
									for(Iterator<RedisConfigurationQuery> queriesIterator = queries.iterator(); queriesIterator.hasNext();) {
										if(queriesIterator.next().getMetaData().isPresent()) {
											queriesIterator.remove();
										}
									}
								});
							})
							.flatMapIterable(Map::entrySet)
							.filter(e -> !e.getValue().isEmpty())
							.flatMap(e -> this.source.getMetaData(e.getKey())
								.doOnNext(metaData -> {
									for(RedisConfigurationQuery query : e.getValue()) {
										if(query.getMetaData().isPresent()) {
											throw new IllegalStateException("MetaData " + asMetaDataKey(e.getKey()) + " is conflicting with " + asMetaDataKey(query.getMetaData().get().getParameters()) + " when considering parameters [" + query.parameters.stream().map(Parameter::toString).collect(Collectors.joining(", ")) + "]"); // TODO create an adhoc exception?
										}
										query.metaData = metaData;
									}
								})
							)
						);
				})
				.thenMany(Flux.fromStream(this.queries.stream()))
				.flatMap(query -> Flux.fromStream(query.names.stream().map(name -> new RedisConfigurationKey(name, query.getMetaData().orElse(null), null, query.parameters)))
					.flatMap(queryKey -> {
						if(query.getMetaData().isPresent() && !query.getMetaData().get().getActiveRevision().isPresent()) {
							// property is managed (ie. we found metadata) but there's no active or selected revision
							return Mono.just(new RedisConfigurationQueryResult(queryKey, null));
						}
						return this.source.commands
							.zrevrangebyscoreWithScores(asPropertyKey(queryKey), Range.from(Boundary.including(0), query.getMetaData().flatMap(RedisConfigurationMetaData::getActiveRevision).map(Boundary::including).orElse(Boundary.unbounded())), Limit.create(0, 1))
							.next()
							.map(result -> Optional.of(result))
							.defaultIfEmpty(Optional.empty())
							.map(result -> result.map(value -> {
									try {
										Optional<String> actualValue = new ConfigurationOptionParser<RedisConfigurationSource>(new StringProvider(value.getValue())).StartValueRevision();
										if(actualValue != null) {
											return new RedisConfigurationQueryResult(queryKey, new GenericConfigurationProperty<ConfigurationKey, RedisConfigurationSource, String>(new RedisConfigurationKey(queryKey.getName(), query.getMetaData().orElse(null), (int)value.getScore(), queryKey.getParameters()), actualValue.orElse(null), this.source));
										}
										else {
											// unset
											return new RedisConfigurationQueryResult(queryKey, new GenericConfigurationProperty<ConfigurationKey, RedisConfigurationSource, String>(new RedisConfigurationKey(queryKey.getName(), query.getMetaData().orElse(null), (int)value.getScore(), queryKey.getParameters()), this.source));
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
	
	/**
	 * <p>
	 * The configuration query result returned by the Redis configuration source.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see ConfigurationQueryResult
	 */
	public static class RedisConfigurationQueryResult extends GenericConfigurationQueryResult<RedisConfigurationKey, ConfigurationProperty<?, ?>> {
		
		private RedisConfigurationQueryResult(RedisConfigurationKey queryKey, ConfigurationProperty<?, ?> queryResult) {
			super(queryKey, queryResult);
		}

		private RedisConfigurationQueryResult(RedisConfigurationKey queryKey, RedisConfigurationSource source, Throwable error) {
			super(queryKey, source, error);
		}
	}
	
	/**
	 * <p>
	 * The configuration update used by the Redis configuration source.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see ConfigurationUpdate
	 */
	public static class RedisConfigurationUpdate implements ConfigurationUpdate<RedisConfigurationUpdate, RedisExecutableConfigurationUpdate, RedisConfigurationUpdateResult> {

		private RedisExecutableConfigurationUpdate executableQuery;
		
		private Map<String, Object> values;
		
		private LinkedList<Parameter> parameters;
		
		private RedisConfigurationMetaData metaData;
		
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
		
		private Optional<RedisConfigurationMetaData> getMetaData() {
			return Optional.ofNullable(this.metaData);
		}
	}
	
	/**
	 * <p>
	 * The executable configuration update used by the Redis configuration source.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see ExecutableConfigurationUpdate
	 */
	public static class RedisExecutableConfigurationUpdate implements ExecutableConfigurationUpdate<RedisConfigurationUpdate, RedisExecutableConfigurationUpdate, RedisConfigurationUpdateResult> {

		private RedisConfigurationSource source;
		
		private LinkedList<RedisConfigurationUpdate> updates;
		
		private RedisExecutableConfigurationUpdate(RedisConfigurationSource source) {
			this.source = source;
			this.updates = new LinkedList<>();
		}
		
		@Override
		public RedisConfigurationUpdate and() {
			this.updates.add(new RedisConfigurationUpdate(this));
			return this.updates.peekLast();
		}
		
		@Override
		public RedisExecutableConfigurationUpdate withParameters(Parameter... parameters) throws IllegalArgumentException {
			if(parameters != null && parameters.length > 0) {
				RedisConfigurationUpdate currentQuery = this.updates.peekLast();
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
				.flatMapMany(metaDataParameterSets -> {
					Map<Integer, Map<List<Parameter>, List<RedisConfigurationUpdate>>> updatesByMetaDataByCard = new TreeMap<>(Comparator.reverseOrder());
					for(RedisConfigurationUpdate update : this.updates) {
						Map<String, Parameter> parametersByKey = update.parameters.stream().collect(Collectors.toMap(Parameter::getKey, Function.identity()));
						metaDataParameterSets.stream()
							.filter(set -> parametersByKey.keySet().containsAll(set))
							.map(parametersSet -> parametersSet.stream().map(parametersByKey::get).collect(Collectors.toList()))
							.forEach(metaKeyParameters -> {
								if(updatesByMetaDataByCard.get(metaKeyParameters.size()) == null) {
									updatesByMetaDataByCard.put(metaKeyParameters.size(), new HashMap<>());
								}
								Map<List<Parameter>, List<RedisConfigurationUpdate>> currentCard = updatesByMetaDataByCard.get(metaKeyParameters.size());
								if(!currentCard.containsKey(metaKeyParameters)) {
									currentCard.put(metaKeyParameters, new ArrayList<>());
								}
								currentCard.get(metaKeyParameters).add(update);
							});
					}
					
					return Flux.fromIterable(updatesByMetaDataByCard.values())
						.concatMap(updatesByMetaData -> Mono.just(updatesByMetaData)
							.doOnSubscribe(subscription -> {
								updatesByMetaData.values().stream().forEach(updates -> {
									for(Iterator<RedisConfigurationUpdate> updatesIterator = updates.iterator(); updatesIterator.hasNext();) {
										if(updatesIterator.next().getMetaData().isPresent()) {
											updatesIterator.remove();
										}
									}
								});
							})
							.flatMapIterable(Map::entrySet)
							.filter(e -> !e.getValue().isEmpty())
							.flatMap(e -> this.source.getMetaData(e.getKey())
								.doOnNext(metaData -> {
									for(RedisConfigurationUpdate update : e.getValue()) {
										if(update.getMetaData().isPresent()) {
											throw new IllegalStateException("MetaData " + asMetaDataKey(e.getKey()) + " is conflicting with " + asMetaDataKey(update.getMetaData().get().getParameters()) + " when considering parameters [" + update.parameters.stream().map(Parameter::toString).collect(Collectors.joining(", ")) + "]"); // TODO create an adhoc exception?, propagate error in the query result?
										}
										update.metaData = metaData;
									}
								})
							)
						);
				})
				.thenMany(Flux.fromStream(this.updates.stream()))
				.flatMap(update -> Flux.fromStream(update.values.entrySet().stream())
					.flatMap(valueEntry -> {
						RedisConfigurationKey updateKey = new RedisConfigurationKey(valueEntry.getKey(), update.getMetaData().orElse(new RedisConfigurationMetaData(null, 1)), null, update.parameters);
						
						String redisKey = asPropertyKey(updateKey);
						int workingRevision = updateKey.getMetaData().get().getWorkingRevision().get();
						StringBuilder redisEncodedValue = new StringBuilder().append(workingRevision).append("{");
						if(valueEntry.getValue() == null) {
							redisEncodedValue.append("null");
						}
						if(valueEntry.getValue() instanceof SpecialValue) {
							redisEncodedValue.append(valueEntry.getValue().toString().toLowerCase());
						}
						else {
							try {
								redisEncodedValue.append("\"").append(this.source.encoder.encode(valueEntry.getValue())).append("\"");
							} 
							catch (ConverterException e) {
								return Mono.just(new RedisConfigurationUpdateResult(updateKey, this.source, new IllegalStateException("Error setting key " + updateKey.toString() + " at revision " + updateKey.revision, e)));
							}
						}
						redisEncodedValue.append("}");
						
						this.source.commands.multi().subscribe();
						this.source.commands.zremrangebyscore(redisKey, Range.from(Boundary.including(workingRevision), Boundary.including(workingRevision))).subscribe();
						this.source.commands.zadd(redisKey, workingRevision, redisEncodedValue.toString()).subscribe();
						
						return this.source.commands.exec()
							.map(transactionResult -> {
								if(transactionResult.wasDiscarded()) {
									return new RedisConfigurationUpdateResult(updateKey, this.source, new RuntimeException("Error setting key " + updateKey.toString() + " at revision " + workingRevision + ": Transaction was discarded"));
								}
								return new RedisConfigurationUpdateResult(updateKey);
							});
					})
				);
		}
	}
	
	/**
	 * <p>
	 * The configuration update result returned by the Redis configuration source.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see ConfigurationUpdateResult
	 */
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
	
	/**
	 * <p>
	 * Returns the configuration metadata for one parameters.
	 * </p>
	 * 
	 * @param k1 the parameter name
	 * @param v1 the parameter value
	 * 
	 * @return a mono emitting the metadata
	 */
	public Mono<RedisConfigurationMetaData> getMetaData(String k1, Object v1) {
		return this.getMetaData(Parameter.of(k1, v1));
	}

	/**
	 * <p>
	 * Returns the configuration metadata for two parameters.
	 * </p>
	 * 
	 * @param k1 the first parameter name
	 * @param v1 the first parameter value
	 * @param k2 the second parameter name
	 * @param v2 the second parameter value
	 * 
	 * @return a mono emitting the metadata
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<RedisConfigurationMetaData> getMetaData(String k1, Object v1, String k2, Object v2) throws IllegalArgumentException {
		return this.getMetaData(Parameter.of(k1, v1), Parameter.of(k2, v2));
	}
	
	/**
	 * <p>
	 * Returns the configuration metadata for three parameters.
	 * </p>
	 * 
	 * @param k1 the first parameter name
	 * @param v1 the first parameter value
	 * @param k2 the second parameter name
	 * @param v2 the second parameter value
	 * @param k3 the third parameter name
	 * @param v3 the third parameter value
	 * 
	 * @return a mono emitting the metadata
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<RedisConfigurationMetaData> getMetaData(String k1, Object v1, String k2, Object v2, String k3, Object v3) throws IllegalArgumentException {
		return this.getMetaData(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3));
	}
	
	/**
	 * <p>
	 * Returns the configuration metadata for four parameters.
	 * </p>
	 * 
	 * @param k1 the first parameter name
	 * @param v1 the first parameter value
	 * @param k2 the second parameter name
	 * @param v2 the second parameter value
	 * @param k3 the third parameter name
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter name
	 * @param v4 the fourth parameter value
	 * 
	 * @return a mono emitting the metadata
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<RedisConfigurationMetaData> getMetaData(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) throws IllegalArgumentException {
		return this.getMetaData(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4));
	}
	
	/**
	 * <p>
	 * Returns the configuration metadata for five parameters.
	 * </p>
	 * 
	 * @param k1 the first parameter name
	 * @param v1 the first parameter value
	 * @param k2 the second parameter name
	 * @param v2 the second parameter value
	 * @param k3 the third parameter name
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter name
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter name
	 * @param v5 the fifth parameter value
	 * 
	 * @return a mono emitting the metadata
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<RedisConfigurationMetaData> getMetaData(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5) throws IllegalArgumentException {
		return this.getMetaData(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5));
	}
	
	/**
	 * <p>
	 * Returns the configuration metadata for six parameters.
	 * </p>
	 * 
	 * @param k1 the first parameter name
	 * @param v1 the first parameter value
	 * @param k2 the second parameter name
	 * @param v2 the second parameter value
	 * @param k3 the third parameter name
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter name
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter name
	 * @param v5 the fifth parameter value
	 * @param k6 the sixth parameter name
	 * @param v6 the sixth parameter value
	 * 
	 * @return a mono emitting the metadata
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<RedisConfigurationMetaData> getMetaData(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6) throws IllegalArgumentException {
		return this.getMetaData(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6));
	}
	
	/**
	 * <p>
	 * Returns the configuration metadata for seven parameters.
	 * </p>
	 * 
	 * @param k1 the first parameter name
	 * @param v1 the first parameter value
	 * @param k2 the second parameter name
	 * @param v2 the second parameter value
	 * @param k3 the third parameter name
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter name
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter name
	 * @param v5 the fifth parameter value
	 * @param k6 the sixth parameter name
	 * @param v6 the sixth parameter value
	 * @param k7 the seventh parameter name
	 * @param v7 the seventh parameter value
	 * 
	 * @return a mono emitting the metadata
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<RedisConfigurationMetaData> getMetaData(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7) throws IllegalArgumentException {
		return this.getMetaData(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7));
	}
	
	/**
	 * <p>
	 * Returns the configuration metadata for eight parameters.
	 * </p>
	 * 
	 * @param k1 the first parameter name
	 * @param v1 the first parameter value
	 * @param k2 the second parameter name
	 * @param v2 the second parameter value
	 * @param k3 the third parameter name
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter name
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter name
	 * @param v5 the fifth parameter value
	 * @param k6 the sixth parameter name
	 * @param v6 the sixth parameter value
	 * @param k7 the seventh parameter name
	 * @param v7 the seventh parameter value
	 * @param k8 the eighth parameter name
	 * @param v8 the eighth parameter value
	 * 
	 * @return a mono emitting the metadata
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<RedisConfigurationMetaData> getMetaData(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8) throws IllegalArgumentException {
		return this.getMetaData(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8));
	}
	
	/**
	 * <p>
	 * Returns the configuration metadata for nine parameters.
	 * </p>
	 * 
	 * @param k1 the first parameter name
	 * @param v1 the first parameter value
	 * @param k2 the second parameter name
	 * @param v2 the second parameter value
	 * @param k3 the third parameter name
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter name
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter name
	 * @param v5 the fifth parameter value
	 * @param k6 the sixth parameter name
	 * @param v6 the sixth parameter value
	 * @param k7 the seventh parameter name
	 * @param v7 the seventh parameter value
	 * @param k8 the eighth parameter name
	 * @param v8 the eighth parameter value
	 * @param k9 the ninth parameter name
	 * @param v9 the ninth parameter value
	 * 
	 * @return a mono emitting the metadata
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<RedisConfigurationMetaData> getMetaData(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9) throws IllegalArgumentException {
		return this.getMetaData(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8), Parameter.of(k9, v9));
	}
	
	/**
	 * <p>
	 * Returns the configuration metadata for ten parameters.
	 * </p>
	 * 
	 * @param k1  the first parameter name
	 * @param v1  the first parameter value
	 * @param k2  the second parameter name
	 * @param v2  the second parameter value
	 * @param k3  the third parameter name
	 * @param v3  the third parameter value
	 * @param k4  the fourth parameter name
	 * @param v4  the fourth parameter value
	 * @param k5  the fifth parameter name
	 * @param v5  the fifth parameter value
	 * @param k6  the sixth parameter name
	 * @param v6  the sixth parameter value
	 * @param k7  the seventh parameter name
	 * @param v7  the seventh parameter value
	 * @param k8  the eighth parameter name
	 * @param v8  the eighth parameter value
	 * @param k9  the ninth parameter name
	 * @param v9  the ninth parameter value
	 * @param k10 the tenth parameter name
	 * @param v10 the tenth parameter value
	 * 
	 * @return a mono emitting the metadata
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<RedisConfigurationMetaData> getMetaData(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9, String k10, Object v10) throws IllegalArgumentException {
		return this.getMetaData(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8), Parameter.of(k9, v9), Parameter.of(k10, v10));
	}
	
	/**
	 * <p>
	 * Activates the working revision for the properties defined with the specified parameter.
	 * </p>
	 * 
	 * @param k1 the parameter name
	 * @param v1 the parameter value
	 * 
	 * @return a mono that completes when the operation succeeds or fails
	 */
	public Mono<Void> activate(String k1, Object v1) {
		return this.activate(Parameter.of(k1, v1));
	}

	/**
	 * <p>
	 * Activates the working revision for the properties defined with the two specified parameters.
	 * </p>
	 * 
	 * @param k1 the first parameter name
	 * @param v1 the first parameter value
	 * @param k2 the second parameter name
	 * @param v2 the second parameter value
	 * 
	 * @return a mono that completes when the operation succeeds or fails
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<Void> activate(String k1, Object v1, String k2, Object v2) throws IllegalArgumentException {
		return this.activate(Parameter.of(k1, v1), Parameter.of(k2, v2));
	}
	
	/**
	 * <p>
	 * Activates the working revision for the properties defined with the three specified parameters.
	 * </p>
	 * 
	 * @param k1 the first parameter name
	 * @param v1 the first parameter value
	 * @param k2 the second parameter name
	 * @param v2 the second parameter value
	 * @param k3 the third parameter name
	 * @param v3 the third parameter value
	 * 
	 * @return a mono that completes when the operation succeeds or fails
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<Void> activate(String k1, Object v1, String k2, Object v2, String k3, Object v3) throws IllegalArgumentException {
		return this.activate(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3));
	}
	
	/**
	 * <p>
	 * Activates the working revision for the properties defined with the four specified parameters.
	 * </p>
	 * 
	 * @param k1 the first parameter name
	 * @param v1 the first parameter value
	 * @param k2 the second parameter name
	 * @param v2 the second parameter value
	 * @param k3 the third parameter name
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter name
	 * @param v4 the fourth parameter value
	 * 
	 * @return a mono that completes when the operation succeeds or fails
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<Void> activate(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) throws IllegalArgumentException {
		return this.activate(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4));
	}
	
	/**
	 * <p>
	 * Activates the working revision for the properties defined with the five specified parameters.
	 * </p>
	 * 
	 * @param k1 the first parameter name
	 * @param v1 the first parameter value
	 * @param k2 the second parameter name
	 * @param v2 the second parameter value
	 * @param k3 the third parameter name
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter name
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter name
	 * @param v5 the fifth parameter value
	 * 
	 * @return a mono that completes when the operation succeeds or fails
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<Void> activate(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5) throws IllegalArgumentException {
		return this.activate(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5));
	}
	
	/**
	 * <p>
	 * Activates the working revision for the properties defined with the six specified parameters.
	 * </p>
	 * 
	 * @param k1 the first parameter name
	 * @param v1 the first parameter value
	 * @param k2 the second parameter name
	 * @param v2 the second parameter value
	 * @param k3 the third parameter name
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter name
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter name
	 * @param v5 the fifth parameter value
	 * @param k6 the sixth parameter name
	 * @param v6 the sixth parameter value
	 * 
	 * @return a mono that completes when the operation succeeds or fails
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<Void> activate(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6) throws IllegalArgumentException {
		return this.activate(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6));
	}
	
	/**
	 * <p>
	 * Activates the working revision for the properties defined with the seven specified parameters.
	 * </p>
	 * 
	 * @param k1 the first parameter name
	 * @param v1 the first parameter value
	 * @param k2 the second parameter name
	 * @param v2 the second parameter value
	 * @param k3 the third parameter name
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter name
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter name
	 * @param v5 the fifth parameter value
	 * @param k6 the sixth parameter name
	 * @param v6 the sixth parameter value
	 * @param k7 the seventh parameter name
	 * @param v7 the seventh parameter value
	 * 
	 * @return a mono that completes when the operation succeeds or fails
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<Void> activate(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7) throws IllegalArgumentException {
		return this.activate(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7));
	}
	
	/**
	 * <p>
	 * Activates the working revision for the properties defined with the eight specified parameters.
	 * </p>
	 * 
	 * @param k1 the first parameter name
	 * @param v1 the first parameter value
	 * @param k2 the second parameter name
	 * @param v2 the second parameter value
	 * @param k3 the third parameter name
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter name
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter name
	 * @param v5 the fifth parameter value
	 * @param k6 the sixth parameter name
	 * @param v6 the sixth parameter value
	 * @param k7 the seventh parameter name
	 * @param v7 the seventh parameter value
	 * @param k8 the eighth parameter name
	 * @param v8 the eighth parameter value
	 * 
	 * @return a mono that completes when the operation succeeds or fails
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<Void> activate(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8) throws IllegalArgumentException {
		return this.activate(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8));
	}
	
	/**
	 * <p>
	 * Activates the working revision for the properties defined with the nine specified parameters.
	 * </p>
	 * 
	 * @param k1 the first parameter name
	 * @param v1 the first parameter value
	 * @param k2 the second parameter name
	 * @param v2 the second parameter value
	 * @param k3 the third parameter name
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter name
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter name
	 * @param v5 the fifth parameter value
	 * @param k6 the sixth parameter name
	 * @param v6 the sixth parameter value
	 * @param k7 the seventh parameter name
	 * @param v7 the seventh parameter value
	 * @param k8 the eighth parameter name
	 * @param v8 the eighth parameter value
	 * @param k9 the ninth parameter name
	 * @param v9 the ninth parameter value
	 * 
	 * @return a mono that completes when the operation succeeds or fails
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<Void> activate(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9) throws IllegalArgumentException {
		return this.activate(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8), Parameter.of(k9, v9));
	}

	/**
	 * <p>
	 * Activates the working revision for the properties defined with the ten specified parameters.
	 * </p>
	 * 
	 * @param k1  the first parameter name
	 * @param v1  the first parameter value
	 * @param k2  the second parameter name
	 * @param v2  the second parameter value
	 * @param k3  the third parameter name
	 * @param v3  the third parameter value
	 * @param k4  the fourth parameter name
	 * @param v4  the fourth parameter value
	 * @param k5  the fifth parameter name
	 * @param v5  the fifth parameter value
	 * @param k6  the sixth parameter name
	 * @param v6  the sixth parameter value
	 * @param k7  the seventh parameter name
	 * @param v7  the seventh parameter value
	 * @param k8  the eighth parameter name
	 * @param v8  the eighth parameter value
	 * @param k9  the ninth parameter name
	 * @param v9  the ninth parameter value
	 * @param k10 the tenth parameter name
	 * @param v10 the tenth parameter value
	 * 
	 * @return a mono that completes when the operation succeeds or fails
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<Void> activate(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9, String k10, Object v10) throws IllegalArgumentException {
		return this.activate(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8), Parameter.of(k9, v9), Parameter.of(k10, v10));
	}

	/**
	 * <p>
	 * Activates the specified revisions for the properties defined with the
	 * specified parameter.
	 * </p>
	 * 
	 * @param revision the revision to activate
	 * @param k1       the parameter name
	 * @param v1       the parameter value
	 * 
	 * @return a mono that completes when the operation succeeds or fails
	 * @throws IllegalArgumentException if the specified revision is greater than
	 *                                  the current working revision
	 */
	public Mono<Void> activate(int revision, String k1, Object v1) {
		return this.activate(revision, Parameter.of(k1, v1));
	}

	/**
	 * <p>
	 * Activates the specified revisions for the properties defined with the two
	 * specified parameters.
	 * </p>
	 * 
	 * @param revision the revision to activate
	 * @param k1       the first parameter name
	 * @param v1       the first parameter value
	 * @param k2       the second parameter name
	 * @param v2       the second parameter value
	 * 
	 * @return a mono that completes when the operation succeeds or fails
	 * @throws IllegalArgumentException if parameters were specified more than once
	 *                                  or if the specified revision is greater than
	 *                                  the current working revision
	 */
	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2));
	}
	
	/**
	 * <p>
	 * Activates the specified revisions for the properties defined with the three
	 * specified parameters.
	 * </p>
	 * 
	 * @param revision the revision to activate
	 * @param k1       the first parameter name
	 * @param v1       the first parameter value
	 * @param k2       the second parameter name
	 * @param v2       the second parameter value
	 * @param k3       the third parameter name
	 * @param v3       the third parameter value
	 * 
	 * @return a mono that completes when the operation succeeds or fails
	 * @throws IllegalArgumentException if parameters were specified more than once
	 *                                  or if the specified revision is greater than
	 *                                  the current working revision
	 */
	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2, String k3, Object v3) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3));
	}
	
	/**
	 * <p>
	 * Activates the specified revisions for the properties defined with the four
	 * specified parameters.
	 * </p>
	 * 
	 * @param revision the revision to activate
	 * @param k1       the first parameter name
	 * @param v1       the first parameter value
	 * @param k2       the second parameter name
	 * @param v2       the second parameter value
	 * @param k3       the third parameter name
	 * @param v3       the third parameter value
	 * @param k4       the fourth parameter name
	 * @param v4       the fourth parameter value
	 * 
	 * @return a mono that completes when the operation succeeds or fails
	 * @throws IllegalArgumentException if parameters were specified more than once
	 *                                  or if the specified revision is greater than
	 *                                  the current working revision
	 */
	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4));
	}
	
	/**
	 * <p>
	 * Activates the specified revisions for the properties defined with the five
	 * specified parameters.
	 * </p>
	 * 
	 * @param revision the revision to activate
	 * @param k1       the first parameter name
	 * @param v1       the first parameter value
	 * @param k2       the second parameter name
	 * @param v2       the second parameter value
	 * @param k3       the third parameter name
	 * @param v3       the third parameter value
	 * @param k4       the fourth parameter name
	 * @param v4       the fourth parameter value
	 * @param k5       the fifth parameter name
	 * @param v5       the fifth parameter value
	 * 
	 * @return a mono that completes when the operation succeeds or fails
	 * @throws IllegalArgumentException if parameters were specified more than once
	 *                                  or if the specified revision is greater than
	 *                                  the current working revision
	 */
	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5));
	}
	
	/**
	 * <p>
	 * Activates the specified revisions for the properties defined with the six
	 * specified parameters.
	 * </p>
	 * 
	 * @param revision the revision to activate
	 * @param k1       the first parameter name
	 * @param v1       the first parameter value
	 * @param k2       the second parameter name
	 * @param v2       the second parameter value
	 * @param k3       the third parameter name
	 * @param v3       the third parameter value
	 * @param k4       the fourth parameter name
	 * @param v4       the fourth parameter value
	 * @param k5       the fifth parameter name
	 * @param v5       the fifth parameter value
	 * @param k6       the sixth parameter name
	 * @param v6       the sixth parameter value
	 * 
	 * @return a mono that completes when the operation succeeds or fails
	 * @throws IllegalArgumentException if parameters were specified more than once
	 *                                  or if the specified revision is greater than
	 *                                  the current working revision
	 */
	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6));
	}
	
	/**
	 * <p>
	 * Activates the specified revisions for the properties defined with the seven
	 * specified parameters.
	 * </p>
	 * 
	 * @param revision the revision to activate
	 * @param k1       the first parameter name
	 * @param v1       the first parameter value
	 * @param k2       the second parameter name
	 * @param v2       the second parameter value
	 * @param k3       the third parameter name
	 * @param v3       the third parameter value
	 * @param k4       the fourth parameter name
	 * @param v4       the fourth parameter value
	 * @param k5       the fifth parameter name
	 * @param v5       the fifth parameter value
	 * @param k6       the sixth parameter name
	 * @param v6       the sixth parameter value
	 * @param k7       the seventh parameter name
	 * @param v7       the seventh parameter value
	 * 
	 * @return a mono that completes when the operation succeeds or fails
	 * @throws IllegalArgumentException if parameters were specified more than once
	 *                                  or if the specified revision is greater than
	 *                                  the current working revision
	 */
	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7));
	}
	
	/**
	 * <p>
	 * Activates the specified revisions for the properties defined with the eight
	 * specified parameters.
	 * </p>
	 * 
	 * @param revision the revision to activate
	 * @param k1       the first parameter name
	 * @param v1       the first parameter value
	 * @param k2       the second parameter name
	 * @param v2       the second parameter value
	 * @param k3       the third parameter name
	 * @param v3       the third parameter value
	 * @param k4       the fourth parameter name
	 * @param v4       the fourth parameter value
	 * @param k5       the fifth parameter name
	 * @param v5       the fifth parameter value
	 * @param k6       the sixth parameter name
	 * @param v6       the sixth parameter value
	 * @param k7       the seventh parameter name
	 * @param v7       the seventh parameter value
	 * @param k8       the eighth parameter name
	 * @param v8       the eighth parameter value
	 * 
	 * @return a mono that completes when the operation succeeds or fails
	 * @throws IllegalArgumentException if parameters were specified more than once
	 *                                  or if the specified revision is greater than
	 *                                  the current working revision
	 */
	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8));
	}
	
	/**
	 * <p>
	 * Activates the specified revisions for the properties defined with the nine
	 * specified parameters.
	 * </p>
	 * 
	 * @param revision the revision to activate
	 * @param k1       the first parameter name
	 * @param v1       the first parameter value
	 * @param k2       the second parameter name
	 * @param v2       the second parameter value
	 * @param k3       the third parameter name
	 * @param v3       the third parameter value
	 * @param k4       the fourth parameter name
	 * @param v4       the fourth parameter value
	 * @param k5       the fifth parameter name
	 * @param v5       the fifth parameter value
	 * @param k6       the sixth parameter name
	 * @param v6       the sixth parameter value
	 * @param k7       the seventh parameter name
	 * @param v7       the seventh parameter value
	 * @param k8       the eighth parameter name
	 * @param v8       the eighth parameter value
	 * @param k9       the ninth parameter name
	 * @param v9       the ninth parameter value
	 * 
	 * @return a mono that completes when the operation succeeds or fails
	 * @throws IllegalArgumentException if parameters were specified more than once
	 *                                  or if the specified revision is greater than
	 *                                  the current working revision
	 */
	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8), Parameter.of(k9, v9));
	}
	
	/**
	 * <p>
	 * Activates the specified revisions for the properties defined with the ten
	 * specified parameters.
	 * </p>
	 * 
	 * @param revision the revision to activate
	 * @param k1       the first parameter name
	 * @param v1       the first parameter value
	 * @param k2       the second parameter name
	 * @param v2       the second parameter value
	 * @param k3       the third parameter name
	 * @param v3       the third parameter value
	 * @param k4       the fourth parameter name
	 * @param v4       the fourth parameter value
	 * @param k5       the fifth parameter name
	 * @param v5       the fifth parameter value
	 * @param k6       the sixth parameter name
	 * @param v6       the sixth parameter value
	 * @param k7       the seventh parameter name
	 * @param v7       the seventh parameter value
	 * @param k8       the eighth parameter name
	 * @param v8       the eighth parameter value
	 * @param k9       the ninth parameter name
	 * @param v9       the ninth parameter value
	 * @param k10      the tenth parameter name
	 * @param v10      the tenth parameter value
	 * 
	 * @return a mono that completes when the operation succeeds or fails
	 * @throws IllegalArgumentException if parameters were specified more than once
	 *                                  or if the specified revision is greater than
	 *                                  the current working revision
	 */
	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9, String k10, Object v10) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8), Parameter.of(k9, v9), Parameter.of(k10, v10));
	}
}
