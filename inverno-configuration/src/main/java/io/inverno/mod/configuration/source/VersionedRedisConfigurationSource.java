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
package io.inverno.mod.configuration.source;

import io.inverno.mod.base.converter.ConverterException;
import io.inverno.mod.base.converter.JoinablePrimitiveEncoder;
import io.inverno.mod.base.converter.SplittablePrimitiveDecoder;
import io.inverno.mod.configuration.AbstractConfigurableConfigurationSource;
import io.inverno.mod.configuration.ConfigurableConfigurationSource;
import io.inverno.mod.configuration.ConfigurationKey;
import io.inverno.mod.configuration.ConfigurationKey.Parameter;
import io.inverno.mod.configuration.ConfigurationProperty;
import io.inverno.mod.configuration.ConfigurationQuery;
import io.inverno.mod.configuration.ConfigurationQueryResult;
import io.inverno.mod.configuration.ConfigurationSource;
import io.inverno.mod.configuration.ConfigurationUpdate;
import io.inverno.mod.configuration.ConfigurationUpdate.SpecialValue;
import io.inverno.mod.configuration.ConfigurationUpdateResult;
import io.inverno.mod.configuration.DefaultableConfigurationSource;
import io.inverno.mod.configuration.DefaultingStrategy;
import io.inverno.mod.configuration.ExecutableConfigurationQuery;
import io.inverno.mod.configuration.ExecutableConfigurationUpdate;
import io.inverno.mod.configuration.ListConfigurationQuery;
import io.inverno.mod.configuration.internal.GenericConfigurationKey;
import io.inverno.mod.configuration.internal.GenericConfigurationProperty;
import io.inverno.mod.configuration.internal.GenericConfigurationQueryResult;
import io.inverno.mod.configuration.internal.GenericConfigurationUpdateResult;
import io.inverno.mod.configuration.internal.JavaStringConverter;
import io.inverno.mod.configuration.internal.parser.option.ConfigurationOptionParser;
import io.inverno.mod.configuration.internal.parser.option.ParseException;
import io.inverno.mod.configuration.internal.parser.option.StringProvider;
import static io.inverno.mod.configuration.source.RedisConfigurationSource.DEFAULT_KEY_PREFIX;
import io.inverno.mod.redis.RedisOperations;
import io.inverno.mod.redis.RedisTransactionalClient;
import io.inverno.mod.redis.operations.Bound;
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
 * This implementation supports basic versioning which allows to set multiple properties and activate or revert them atomically.
 * </p>
 *
 * <p>
 * Properties can be viewed in a tree of properties whose nodes correspond to parameters in natural order, a global revision is defined at the root of the tree and finer revisions can also be created
 * in child nodes to version particular branches.
 * </p>
 *
 * <p>
 * Particular care must be taken when deciding to version a specific branch, a property can only be versioned once which is the case when versioned property sets are disjointed. More specifically, a
 * given property can't be versioned twice which might happen when the configuration is activated with different overlapping sets of parameters. An exception is normally thrown when such situation is
 * detected. On the other hand, it is quite possible to version nested branches.
 * </p>
 *
 * <p>
 * For instance, the following setup is most likely to fail if properties can be defined with both {@code environment="production"} and {@code zone="eu"} parameters:
 * </p>
 *
 * <ul>
 * <li>{@code []}: global tree</li>
 * <li>{@code [environment="production"]}: production environment tree</li>
 * <li>{@code [zone="eu"]}: eu zone tree</li>
 * </ul>
 *
 * <p>
 * While the following setup will work just fine:</p>
 *
 * <ul>
 * <li>{@code []}: global tree</li>
 * <li>{@code [environment="production"]}: production environment tree</li>
 * <li>{@code [environment="production", zone="eu"]}: eu zone tree</li>
 * </ul>
 *
 * <p>
 * Properties are set for the working revision corresponding to their parameters. The working revision is activated using the {@link VersionedRedisConfigurationSource#activate(Parameter[])} method, the
 * {@link VersionedRedisConfigurationSource#activate(int, Parameter[])} is used to activate a specific revision.
 * </p>
 *
 * <p>
 * A typical workflow to set properties is:
 * </p>
 *
 * <blockquote>
 *
 * <pre>
 * VersionedRedisConfigurationSource source = ...;
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
 * <p>
 * This configuration source stored three types of entries:
 * </p>
 * 
 * <ul>
 * <li>Configuration properties as a sorted set with key of the form: {@code keyPrefix ":V:PROP:" propertyName "[" [ key "=" value [ "," key "=" value ]* "]"}.</li>
 * <li>Configuration metadata as hashes with key of the form: {@code keyPrefix ":V:META:" propertyName "[" [ key "=" value [ "," key "=" value ]* "]"}.</li>
 * <li>A configuration metadata control entry as a set with key of the form: {@code keyPrefix ":V:META:CTRL"}.</li>
 * </ul>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see ConfigurableConfigurationSource
 */
public class VersionedRedisConfigurationSource extends AbstractConfigurableConfigurationSource<VersionedRedisConfigurationSource.VersionedRedisConfigurationQuery, VersionedRedisConfigurationSource.VersionedRedisExecutableConfigurationQuery, VersionedRedisConfigurationSource.VersionedRedisListConfigurationQuery, VersionedRedisConfigurationSource.VersionedRedisConfigurationUpdate, VersionedRedisConfigurationSource.VersionedRedisExecutableConfigurationUpdate, String> implements DefaultableConfigurationSource<VersionedRedisConfigurationSource.VersionedRedisConfigurationQuery, VersionedRedisConfigurationSource.VersionedRedisExecutableConfigurationQuery, VersionedRedisConfigurationSource.VersionedRedisListConfigurationQuery, VersionedRedisConfigurationSource> {

	/**
	 * The default key prefix.
	 */
	public static final String DEFAUL_KEY_PREFIX = "CONF";
	
	private static final Logger LOGGER = LogManager.getLogger(VersionedRedisConfigurationSource.class);
	
	private static final String METADATA_FIELD_ACTIVE_REVISION = "active_revision";
	private static final String METADATA_FIELD_WORKING_REVISION = "working_revision";
	
	private final RedisTransactionalClient<String, String> redisClient;
	private final DefaultingStrategy defaultingStrategy;

	private String keyPrefix;
	private String metadataKeyPrefix;
	private String propertyKeyPrefix;
	
	private String metadataControlKey;
	
	private VersionedRedisConfigurationSource initial;
	
	/**
	 * <p>
	 * Creates a versioned Redis configuration source with the specified redis client.
	 * </p>
	 * 
	 * @param redisClient a redis client
	 */
	public VersionedRedisConfigurationSource(RedisTransactionalClient<String, String> redisClient) {
		this(redisClient, new JavaStringConverter(), new JavaStringConverter());
	}
	
	/**
	 * <p>
	 * Creates a versioned Redis configuration source with the specified redis client, string value encoder and decoder.
	 * </p>
	 *
	 * @param redisClient a redis client
	 * @param encoder     a string encoder
	 * @param decoder     a string decoder
	 */
	public VersionedRedisConfigurationSource(RedisTransactionalClient<String, String> redisClient, JoinablePrimitiveEncoder<String> encoder, SplittablePrimitiveDecoder<String> decoder) {
		super(encoder, decoder);
		this.redisClient = redisClient;
		this.defaultingStrategy = DefaultingStrategy.noOp();
		this.setKeyPrefix(DEFAULT_KEY_PREFIX);
	}
	
	/**
	 * <p>
	 * Creates a versioned redis configuration source from the specified initial source and using the specified defaulting strategy.
	 * </p>
	 *
	 * @param initial            the initial configuration source.
	 * @param defaultingStrategy a defaulting strategy
	 */
	private VersionedRedisConfigurationSource(VersionedRedisConfigurationSource initial, DefaultingStrategy defaultingStrategy) {
		super(initial.encoder, initial.decoder);
		this.initial = initial;
		this.redisClient = initial.redisClient;
		this.defaultingStrategy = defaultingStrategy;
		this.setKeyPrefix(initial.keyPrefix);
	}
	
	/**
	 * <p>
	 * Returns the prefix that is prepended to configuration source keys.
	 * </p>
	 * 
	 * <ul>
	 * <li>A configuration property key is of the form: {@code keyPrefix ":V:PROP:" propertyName "[" [ key "=" value [ "," key "=" value ]* "]"}.</li>
	 * <li>A configuration metadata key is of the form: {@code keyPrefix ":V:META:" propertyName "[" [ key "=" value [ "," key "=" value ]* "]"}.</li>
	 * <li>The configuration metadata control key is of the form: {@code keyPrefix ":V:META:CTRL"}.</li>
	 * </ul>
	 * 
	 * @return the key prefix
	 */
	public final String getKeyPrefix() {
		return keyPrefix;
	}

	/**
	 * <p>
	 * Sets the prefix that is prepended to configuration source keys.
	 * </p>
	 * 
	 * <ul>
	 * <li>A configuration property key is of the form: {@code keyPrefix ":V:PROP:" propertyName "[" [ key "=" value [ "," key "=" value ]* "]"}.</li>
	 * <li>A configuration metadata key is of the form: {@code keyPrefix ":V:META:" propertyName "[" [ key "=" value [ "," key "=" value ]* "]"}.</li>
	 * <li>The configuration metadata control key is of the form: {@code keyPrefix ":V:META:CTRL"}.</li>
	 * </ul>
	 * 
	 * @param keyPrefix the key prefix to set
	 */
	public final void setKeyPrefix(String keyPrefix) {
		this.keyPrefix = StringUtils.isNotBlank(keyPrefix) ? keyPrefix : DEFAULT_KEY_PREFIX;
		this.propertyKeyPrefix = keyPrefix + ":V:PROP:";
		this.metadataKeyPrefix = keyPrefix + ":V:META:";
		this.metadataControlKey = this.metadataKeyPrefix + "CTRL";
	}

	@Override
	public VersionedRedisConfigurationSource withDefaultingStrategy(DefaultingStrategy defaultingStrategy) {
		return new VersionedRedisConfigurationSource(this.initial != null ? this.initial : this, defaultingStrategy);
	}

	@Override
	public VersionedRedisConfigurationSource unwrap() {
		return this.initial != null ? this.initial : this;
	}
	
	@Override
	public VersionedRedisExecutableConfigurationQuery get(String... names) throws IllegalArgumentException {
		return new VersionedRedisExecutableConfigurationQuery(this).and().get(names);
	}
	
	@Override
	public VersionedRedisListConfigurationQuery list(String name) throws IllegalArgumentException {
		return new VersionedRedisListConfigurationQuery(this, name);
	}

	@Override
	public VersionedRedisExecutableConfigurationUpdate set(Map<String, Object> values) throws IllegalArgumentException {
		return new VersionedRedisExecutableConfigurationUpdate(this).and().set(values);
	}
	
	/**
	 * <p>
	 * Returns the list of metadata parameters.
	 * </p>
	 * 
	 * @param operations The Redis operations used to query the data store
	 * 
	 * @return a mono emitting the list of metadata parameters
	 */
	protected Mono<List<List<String>>> getMetaDataParameterSets(RedisOperations<String, String> operations) {
		return operations.smembers(this.metadataControlKey)
			.map(value -> Arrays.stream(value.split(",")).filter(s -> !s.isEmpty()).collect(Collectors.toList()))
			.collectList();
	}
	
	/**
	 * <p>
	 * Returns the configuration metadata for the specified list of parameters extracted form a configuration query.
	 * </p>
	 *
	 * @param operations a Redis connection
	 * @param metaDataParameters a list of parameters
	 *
	 * @return a mono emitting the metadata
	 */
	protected Mono<VersionedRedisConfigurationMetaData> getMetaData(RedisOperations<String, String> operations, List<Parameter> metaDataParameters) {
		String metaDataKey = asMetaDataKey(metaDataParameters);
		return operations.hgetall(metaDataKey)
			.collectMap(e -> e.getKey(), e -> e.getValue().get())
			.doOnNext(data -> {
				if(!data.containsKey(METADATA_FIELD_WORKING_REVISION)) {
					throw new IllegalStateException("Invalid meta data found for key " + metaDataKey + ": Missing " + METADATA_FIELD_WORKING_REVISION);
				}
			})
			.map(data -> new VersionedRedisConfigurationMetaData(metaDataParameters, data));
	}
	
	/**
	 * 
	 * @param operations
	 * @param parameters
	 * @param metaDataParameterSets
	 * @return 
	 */
	private Mono<VersionedRedisConfigurationMetaData> getMetaData(RedisOperations<String, String> operations, List<Parameter> parameters, List<List<String>> metaDataParameterSets) {
		Map<String, Parameter> parametersByKey = parameters.stream().collect(Collectors.toMap(Parameter::getKey, Function.identity()));
		return Flux.fromStream(metaDataParameterSets.stream()
				.filter(set -> parametersByKey.keySet().containsAll(set))
				.map(parametersSet -> parametersSet.stream().map(parametersByKey::get).collect(Collectors.toList()))
			)
			.groupBy(queryParameters -> queryParameters.size())
			.sort(Collections.reverseOrder(Comparator.comparing(cardGroup -> cardGroup.key())))
			.flatMap(cardGroup -> cardGroup
				.flatMap(queryParameters -> this.getMetaData(operations, queryParameters))
				.singleOrEmpty()
				// TODO make this conflict explicit what are the problematic metadata
				.onErrorMap(IndexOutOfBoundsException.class, ex -> new IllegalStateException("A conflict of MetaData has been detected when considering parameters [" + parameters.stream().map(Parameter::toString).collect(Collectors.joining(", ")) + "]"))
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
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<VersionedRedisConfigurationMetaData> getMetaData(Parameter... parameters) throws IllegalArgumentException {
		Set<String> parameterKeys = new HashSet<>();
		List<String> duplicateParameters = new LinkedList<>();
		for(Parameter parameter : parameters) {
			if(!parameterKeys.add(parameter.getKey())) {
				duplicateParameters.add(parameter.getKey());
			}
		}
		if(!duplicateParameters.isEmpty()) {
			throw new IllegalArgumentException("The following parameters were specified more than once: " + duplicateParameters.stream().collect(Collectors.joining(", ")));
		}
		
		List<Parameter> parametersList = parameters != null ? Arrays.asList(parameters) : List.of();
		
		return Mono.from(this.redisClient.connection(operations -> this.getMetaDataParameterSets(operations)
			.flatMap(metaDataParameterSets -> this.getMetaData(operations, parametersList, metaDataParameterSets))));
	}
	
	/**
	 * <p>
	 * Activates the working revision for the properties defined with the specified parameters.
	 * </p>
	 *
	 * @param parameters an array of parameters
	 *
	 * @return a mono that completes when the operation succeeds or fails
	 *
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
		if(!duplicateParameters.isEmpty()) {
			throw new IllegalArgumentException("The following parameters were specified more than once: " + duplicateParameters.stream().collect(Collectors.joining(", ")));
		}
		
		List<Parameter> parametersList = parameters != null ? Arrays.asList(parameters) : List.of();
		String metaDataKey = asMetaDataKey(parametersList);
		
		return Mono.from(this.redisClient.connection(operations -> this.getMetaDataParameterSets(operations)
			.flatMap(metaDataParameterSets -> this.getMetaData(operations, parametersList, metaDataParameterSets))
			.defaultIfEmpty(new VersionedRedisConfigurationMetaData(null, 1))
			.flatMap(metaData -> {
				int workingRevision = metaData.getWorkingRevision().get();
				return this.redisClient.multi(ops -> {
						return Flux.just(
							// TODO Parameter key should be a valid Java identifier, idem for property name actually
							ops.sadd(this.metadataControlKey, parametersList.stream().map(Parameter::getKey).sorted().collect(Collectors.joining(","))),
							// We always set the working revision since metadata might not exist for the specified parameters
							ops.hset(metaDataKey, entries -> entries.entry(METADATA_FIELD_ACTIVE_REVISION, Integer.toString(workingRevision)).entry(METADATA_FIELD_WORKING_REVISION, Integer.toString(workingRevision + 1)))
						);
					})
					.map(transactionResult -> {
						if(transactionResult.wasDiscarded()) {
							return Mono.error(new RuntimeException("Error activating revision " + workingRevision + " for key " + metaDataKey + ": Transaction was discarded"));
						}
						return Mono.empty();
					});
			})
			.then()));
	}
	
	/**
	 * <p>
	 * Activates the specified revisions for the properties defined with the specified parameters.
	 * </p>
	 *
	 * @param revision   the revision to activate
	 * @param parameters an array of parameters
	 *
	 * @return a mono that completes when the operation succeeds or fails
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once or if the specified revision is greater than the current working revision
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
		if(!duplicateParameters.isEmpty()) {
			throw new IllegalArgumentException("The following parameters were specified more than once: " + duplicateParameters.stream().collect(Collectors.joining(", ")));
		}
		
		List<Parameter> parametersList = parameters != null ? Arrays.asList(parameters) : List.of();
		String metaDataKey = asMetaDataKey(parametersList);
		
		return Mono.from(this.redisClient.connection(operations -> this.getMetaDataParameterSets(operations)
			.flatMap(metaDataParameterSets -> this.getMetaData(operations, parametersList, metaDataParameterSets))
			.defaultIfEmpty(new VersionedRedisConfigurationMetaData(null, 1))
			.flatMap(metaData -> {
				int workingRevision = metaData.getWorkingRevision().get();
				if(revision > workingRevision) {
					return Mono.error(new IllegalArgumentException("The revision to activate: " + revision + " can't be greater than the current working revision: " + workingRevision));
				}
				else if(metaData.getActiveRevision().filter(activeRevision -> activeRevision  == revision).isPresent()) {
					return Mono.empty();
				}
				else {
					return this.redisClient.multi(ops -> {
							return Flux.just(
								// TODO Parameter key should be a valid Java identifier, idem for property name actually
								ops.sadd(this.metadataControlKey, parametersList.stream().map(Parameter::getKey).sorted().collect(Collectors.joining(","))),
								// We always set the working revision since metadata might not exist for the specified parameters
								ops.hset(metaDataKey, entries -> entries.entry(METADATA_FIELD_ACTIVE_REVISION, Integer.toString(revision)).entry(METADATA_FIELD_WORKING_REVISION, Integer.toString(revision == workingRevision ? workingRevision + 1 : workingRevision)))
							);
						})
						.map(transactionResult -> {
							if(transactionResult.wasDiscarded()) {
								return Mono.error(new RuntimeException("Error activating revision " + revision + " for key " + metaDataKey + ": Transaction was discarded"));
							}
							return Mono.empty();
						});
				}
			})
			.then()));
	}
	
	/**
	 * <p>
	 * Provides information about a particular configuration branch.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see VersionedRedisConfigurationSource
	 */
	public static class VersionedRedisConfigurationMetaData {
		
		private List<Parameter> parameters;
		
		private Optional<Integer> activeRevision;
		
		private Optional<Integer> workingRevision;
		
		private VersionedRedisConfigurationMetaData(List<Parameter> parameters, Map<String, String> data) throws IllegalArgumentException {
			this.parameters = parameters;
			this.activeRevision = Optional.ofNullable(data.get(METADATA_FIELD_ACTIVE_REVISION)).map(Integer::parseInt);
			this.workingRevision = Optional.ofNullable(data.get(METADATA_FIELD_WORKING_REVISION)).map(Integer::parseInt);
		}
		
		private VersionedRedisConfigurationMetaData(Integer activeRevision, Integer workingRevision) throws IllegalArgumentException {
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
		 * @return an optional returning the working revision, or an empty optional if no working revision has been defined
		 */
		public Optional<Integer> getWorkingRevision() {
			return this.workingRevision;
		}
		
		/**
		 * <p>
		 * Returns the active revision of the configuration branch.
		 * </p>
		 *
		 * @return an optional returning the active revision, or an empty optional if no revision has been activated so far
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
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see ConfigurationKey
	 */
	public static class VersionedRedisConfigurationKey extends GenericConfigurationKey {

		private Optional<VersionedRedisConfigurationMetaData> metaData;
		private Optional<Integer> revision;
		
		private VersionedRedisConfigurationKey(String name, Collection<Parameter> parameters) {
			super(name, parameters);
			this.metaData = Optional.empty();
			this.revision = Optional.empty();
		}
		
		private VersionedRedisConfigurationKey(String name, VersionedRedisConfigurationMetaData metaData, Integer revision, Collection<Parameter> parameters) {
			super(name, parameters);
			this.metaData = Optional.ofNullable(metaData);
			this.revision = Optional.ofNullable(revision);
		}
		
		/**
		 * <p>
		 * Returns the meta data associated with the key.
		 * </p>
		 * 
		 * @return the meta data
		 */
		public Optional<VersionedRedisConfigurationMetaData> getMetaData() {
			return metaData;
		}
		
		private void setMedataData(VersionedRedisConfigurationMetaData metaData) {
			this.metaData = Optional.ofNullable(metaData);
		}
		
		/**
		 * <p>
		 * Returns revision of the property identified by the key.
		 * </p>
		 *
		 * @return an optional returning the revision or an empty optional if there's no revision
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
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see ConfigurationQuery
	 */
	public static class VersionedRedisConfigurationQuery implements ConfigurationQuery<VersionedRedisConfigurationQuery, VersionedRedisExecutableConfigurationQuery> {

		private final VersionedRedisExecutableConfigurationQuery executableQuery;
		
		private final List<String> names;
		
		private final LinkedList<Parameter> parameters;
		
		private VersionedRedisConfigurationMetaData metaData;
		
		private VersionedRedisConfigurationQuery(VersionedRedisExecutableConfigurationQuery executableQuery) {
			this.executableQuery = executableQuery;
			this.names = new LinkedList<>();
			this.parameters = new LinkedList<>();
		}
		
		@Override
		public VersionedRedisExecutableConfigurationQuery get(String... names) throws IllegalArgumentException {
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
	 * @since 1.0
	 * 
	 * @see ExecutableConfigurationQuery
	 */
	public static class VersionedRedisExecutableConfigurationQuery implements ExecutableConfigurationQuery<VersionedRedisConfigurationQuery, VersionedRedisExecutableConfigurationQuery> {

		private VersionedRedisConfigurationSource source;
		
		private LinkedList<VersionedRedisConfigurationQuery> queries;
		
		private VersionedRedisExecutableConfigurationQuery(VersionedRedisConfigurationSource source) {
			this.source = source;
			this.queries = new LinkedList<>();
		}
		
		@Override
		public VersionedRedisConfigurationQuery and() {
			this.queries.add(new VersionedRedisConfigurationQuery(this));
			return this.queries.peekLast();
		}

		@Override
		public VersionedRedisExecutableConfigurationQuery withParameters(List<Parameter> parameters) throws IllegalArgumentException {
			VersionedRedisConfigurationQuery currentQuery = this.queries.peekLast();
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
		public VersionedRedisExecutableConfigurationQuery atRevision(int revision) throws IllegalArgumentException {
			if(revision < 1) {
				throw new IllegalArgumentException("Revision must be a positive integer");
			}
			VersionedRedisConfigurationQuery currentQuery = this.queries.peekLast();
			currentQuery.metaData = new VersionedRedisConfigurationMetaData(revision, null);
			return this;
		}
		
		@Override
		public Flux<ConfigurationQueryResult> execute() {
			List<VersionedRedisConfigurationQueryResult> results = this.queries.stream()
				.flatMap(query -> query.names.stream().map(name -> new VersionedRedisConfigurationKey(name, query.metaData != null ? query.metaData : null, null, query.parameters)))
				.map(queryKey -> {
					List<ConfigurationKey> defaultingKeys;
					if(this.source.defaultingStrategy != null) {
						defaultingKeys = this.source.defaultingStrategy.getDefaultingKeys(queryKey);
					}
					else {
						defaultingKeys = List.of(queryKey);
					}
					return new VersionedRedisConfigurationQueryResult(queryKey, defaultingKeys.stream().map(defaultingKey -> new VersionedRedisConfigurationKey(defaultingKey.getName(), defaultingKey.getParameters())).collect(Collectors.toList()));
				})
				.collect(Collectors.toList());
			
			return Mono.when(this.source.redisClient.connection(operations -> {
				return this.source.getMetaDataParameterSets(operations)
					.flatMapMany(metaDataParameterSets -> {
						Map<Integer, Map<List<Parameter>, List<VersionedRedisConfigurationKey>>> queriesByMetaDataByCard = new TreeMap<>(Comparator.reverseOrder());
						for(VersionedRedisConfigurationQueryResult result : results) {
							VersionedRedisConfigurationMetaData providedMetaData = result.getQueryKey().metaData.orElse(null);
							for(VersionedRedisConfigurationKey defaultingKey : result.defaultingKeys) {
								if(providedMetaData != null) {
									defaultingKey.setMedataData(providedMetaData);
									continue;
								}
								Map<String, Parameter> parametersByKey = defaultingKey.getParameters().stream().collect(Collectors.toMap(Parameter::getKey, Function.identity()));
								metaDataParameterSets.stream()
									.filter(set -> parametersByKey.keySet().containsAll(set))
									.map(parametersSet -> parametersSet.stream().map(parametersByKey::get).collect(Collectors.toList()))
									.forEach(metaKeyParameters -> {
										if(queriesByMetaDataByCard.get(metaKeyParameters.size()) == null) {
											queriesByMetaDataByCard.put(metaKeyParameters.size(), new HashMap<>());
										}
										Map<List<Parameter>, List<VersionedRedisConfigurationKey>> currentCard = queriesByMetaDataByCard.get(metaKeyParameters.size());
										if(!currentCard.containsKey(metaKeyParameters)) {
											currentCard.put(metaKeyParameters, new ArrayList<>());
										}
										currentCard.get(metaKeyParameters).add(defaultingKey);
									});
							}
						}
						
						return Flux.fromIterable(queriesByMetaDataByCard.values())
							.concatMap(queriesByMetaData -> Flux.fromStream(() -> queriesByMetaData.entrySet().stream().filter(e -> {
										List<VersionedRedisConfigurationKey> queryKeys = e.getValue();
										for(Iterator<VersionedRedisConfigurationKey> queriyKeysIterator = queryKeys.iterator(); queriyKeysIterator.hasNext();) {
											if(queriyKeysIterator.next().metaData.isPresent()) {
												queriyKeysIterator.remove();
											}
										}
										return !queryKeys.isEmpty();
									})
								)
								.flatMap(e -> this.source.getMetaData(operations, e.getKey())
									.doOnNext(metaData -> {
										for(VersionedRedisConfigurationKey queryKey : e.getValue()) {
											queryKey.metaData.ifPresentOrElse(
												queryKeyMetaData -> {
													throw new IllegalStateException("MetaData " + this.source.asMetaDataKey(e.getKey()) + " is conflicting with " + this.source.asMetaDataKey(queryKeyMetaData.getParameters()) + " when considering parameters [" + queryKey.getParameters().stream().map(Parameter::toString).collect(Collectors.joining(", ")) + "]"); // TODO create an adhoc exception?
												},
												() -> {
													queryKey.setMedataData(metaData);
												}
											);
										}
									})
								)
							);
					});
			}))
			.thenMany(this.source.redisClient.batch(operations -> Flux.fromStream(results.stream()
				.map(result -> Flux.fromIterable(result.defaultingKeys)
					.flatMap(queryKey -> {
						if(queryKey.metaData.isPresent() && !queryKey.metaData.get().getActiveRevision().isPresent()) {
							// property is managed (ie. we found metadata) but there's no active or selected revision
							return Mono.empty();
						}

						return operations
							.zrangeWithScores()
							.reverse()
							.byScore()
							.limit(0, 1)
							.build(this.source.asPropertyKey(queryKey), Bound.inclusive(0), queryKey.metaData.flatMap(VersionedRedisConfigurationMetaData::getActiveRevision).map(Bound::inclusive).orElse(Bound.unbounded()))
							.next()
							.mapNotNull(scoredMember -> {
								if(result.resolved) {
									return null;
								}
								try {
									Optional<String> actualValue = new ConfigurationOptionParser<VersionedRedisConfigurationSource>(new StringProvider(scoredMember.getValue())).StartValueRevision();
									if(actualValue != null) {
										result.setResult(new GenericConfigurationProperty<ConfigurationKey, VersionedRedisConfigurationSource, String>(new VersionedRedisConfigurationKey(queryKey.getName(), queryKey.metaData.orElse(null), (int)scoredMember.getScore(), queryKey.getParameters()), actualValue.orElse(null), this.source));
									}
									else {
										// unset
										result.setResult(new GenericConfigurationProperty<ConfigurationKey, VersionedRedisConfigurationSource, String>(new VersionedRedisConfigurationKey(queryKey.getName(), queryKey.metaData.orElse(null), (int)scoredMember.getScore(), queryKey.getParameters()), this.source));
									}
								}
								catch (ParseException e) {
									result.setError(this.source, new IllegalStateException("Invalid value found for key " + queryKey.toString() + " at revision " + (int)scoredMember.getScore(), e));
								}
								return (ConfigurationQueryResult)result;
							})
							.doOnError(error -> result.setError(this.source, error))
							.onErrorResume(error -> Mono.just(result));
					})
					.defaultIfEmpty((ConfigurationQueryResult)result)
				)
			)));
		}
	}
	
	/**
	 * <p>
	 * The configuration query result returned by the Redis configuration source.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see ConfigurationQueryResult
	 */
	public static class VersionedRedisConfigurationQueryResult extends GenericConfigurationQueryResult {
	
		private final List<VersionedRedisConfigurationKey> defaultingKeys;
		
		private boolean resolved;
		
		private VersionedRedisConfigurationQueryResult(VersionedRedisConfigurationKey queryKey, List<VersionedRedisConfigurationKey> defaultingKeys) {
			super(queryKey, null);
			this.defaultingKeys = defaultingKeys;
		}
		
		/**
		 * <p>
		 * Returns the versioned configuration key corresponding to the query that was executed.
		 * </p>
		 * 
		 * <p>
		 * The meta data in the key reflects the revision that was set when building the query (see {@link VersionedRedisExecutableConfigurationQuery#atRevision(int)}), if no revision has been
		 * specified query key's meta data are are unset. The actual revision of a result property is provided by the meta data in the result the property key.
		 * </p>
		 * 
		 * @return a versioned configuration key
		 */
		@Override
		public VersionedRedisConfigurationKey getQueryKey() {
			return (VersionedRedisConfigurationKey)super.getQueryKey();
		}
		
		private void setResult(ConfigurationProperty queryResult) {
			this.queryResult = Optional.ofNullable(queryResult);
			this.resolved = true;
		}
		
		private void setError(VersionedRedisConfigurationSource errorSource, Throwable error) {
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
	 * @since 1.0
	 * 
	 * @see ConfigurationUpdate
	 */
	public static class VersionedRedisConfigurationUpdate implements ConfigurationUpdate<VersionedRedisConfigurationUpdate, VersionedRedisExecutableConfigurationUpdate> {

		private VersionedRedisExecutableConfigurationUpdate executableQuery;
		
		private Map<String, Object> values;
		
		private LinkedList<Parameter> parameters;
		
		private VersionedRedisConfigurationMetaData metaData;
		
		private VersionedRedisConfigurationUpdate(VersionedRedisExecutableConfigurationUpdate executableQuery) {
			this.executableQuery = executableQuery;
			this.values = Map.of();
			this.parameters = new LinkedList<>();
		}
		
		@Override
		public VersionedRedisExecutableConfigurationUpdate set(Map<String, Object> values) {
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
	 * @since 1.0
	 * 
	 * @see ExecutableConfigurationUpdate
	 */
	public static class VersionedRedisExecutableConfigurationUpdate implements ExecutableConfigurationUpdate<VersionedRedisConfigurationUpdate, VersionedRedisExecutableConfigurationUpdate> {

		private VersionedRedisConfigurationSource source;
		
		private LinkedList<VersionedRedisConfigurationUpdate> updates;
		
		private VersionedRedisExecutableConfigurationUpdate(VersionedRedisConfigurationSource source) {
			this.source = source;
			this.updates = new LinkedList<>();
		}
		
		@Override
		public VersionedRedisConfigurationUpdate and() {
			this.updates.add(new VersionedRedisConfigurationUpdate(this));
			return this.updates.peekLast();
		}
		
		@Override
		public VersionedRedisExecutableConfigurationUpdate withParameters(List<Parameter> parameters) throws IllegalArgumentException {
			VersionedRedisConfigurationUpdate currentQuery = this.updates.peekLast();
			currentQuery.parameters.clear();
			if(parameters != null && !parameters.isEmpty()) {
				Set<String> parameterKeys = new HashSet<>();
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
		public Flux<ConfigurationUpdateResult> execute() {
			return Flux.from(this.source.redisClient.connection(operations -> {
				return this.source.getMetaDataParameterSets(operations)
					.flatMapMany(metaDataParameterSets -> {
						Map<Integer, Map<List<Parameter>, List<VersionedRedisConfigurationUpdate>>> updatesByMetaDataByCard = new TreeMap<>(Comparator.reverseOrder());
						for(VersionedRedisConfigurationUpdate update : this.updates) {
							Map<String, Parameter> parametersByKey = update.parameters.stream().collect(Collectors.toMap(Parameter::getKey, Function.identity()));
							metaDataParameterSets.stream()
								.filter(set -> parametersByKey.keySet().containsAll(set))
								.map(parametersSet -> parametersSet.stream().map(parametersByKey::get).collect(Collectors.toList()))
								.forEach(metaKeyParameters -> {
									if(updatesByMetaDataByCard.get(metaKeyParameters.size()) == null) {
										updatesByMetaDataByCard.put(metaKeyParameters.size(), new HashMap<>());
									}
									Map<List<Parameter>, List<VersionedRedisConfigurationUpdate>> currentCard = updatesByMetaDataByCard.get(metaKeyParameters.size());
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
										for(Iterator<VersionedRedisConfigurationUpdate> updatesIterator = updates.iterator(); updatesIterator.hasNext();) {
											if(updatesIterator.next().metaData != null) {
												updatesIterator.remove();
											}
										}
									});
								})
								.flatMapIterable(Map::entrySet)
								.filter(e -> !e.getValue().isEmpty())
								.flatMap(e -> this.source.getMetaData(operations, e.getKey())
									.doOnNext(metaData -> {
										for(VersionedRedisConfigurationUpdate update : e.getValue()) {
											if(update.metaData != null) {
												throw new IllegalStateException("MetaData " + this.source.asMetaDataKey(e.getKey()) + " is conflicting with " + this.source.asMetaDataKey(update.metaData.getParameters()) + " when considering parameters [" + update.parameters.stream().map(Parameter::toString).collect(Collectors.joining(", ")) + "]"); // TODO create an adhoc exception?, propagate error in the query result?
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
							VersionedRedisConfigurationKey updateKey = new VersionedRedisConfigurationKey(valueEntry.getKey(), update.metaData != null ? update.metaData : new VersionedRedisConfigurationMetaData(null, 1), null, update.parameters);

							String redisKey = this.source.asPropertyKey(updateKey);
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
									return Mono.just(new VersionedRedisConfigurationUpdateResult(updateKey, this.source, new IllegalStateException("Error setting key " + updateKey.toString() + " at revision " + updateKey.revision, e)));
								}
							}
							redisEncodedValue.append("}");

							// TODO we should put all updates in a single multi
							return this.source.redisClient.multi(ops -> {
									return Flux.just(
										ops.zremrangebyscore(redisKey, Bound.inclusive(workingRevision), Bound.inclusive(workingRevision)),
										ops.zadd(redisKey, workingRevision, redisEncodedValue.toString())
									);
								})
								.map(transactionResult -> {
									if(transactionResult.wasDiscarded()) {
										return new VersionedRedisConfigurationUpdateResult(updateKey, this.source, new RuntimeException("Error setting key " + updateKey.toString() + " at revision " + workingRevision + ": Transaction was discarded"));
									}
									return new VersionedRedisConfigurationUpdateResult(updateKey);
								});
						})
					);
			}));
		}
	}
	
	/**
	 * <p>
	 * The configuration update result returned by the Redis configuration source.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see ConfigurationUpdateResult
	 */
	public static class VersionedRedisConfigurationUpdateResult extends GenericConfigurationUpdateResult {

		private VersionedRedisConfigurationUpdateResult(VersionedRedisConfigurationKey updateKey) {
			super(updateKey);
		}
		
		private VersionedRedisConfigurationUpdateResult(VersionedRedisConfigurationKey updateKey, ConfigurationSource<?,?,?> source, Throwable error) {
			super(updateKey, source, error);
		}
	}
	
	/**
	 * <p>
	 * The list configuration query used by the Redis configuration source.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	public static class VersionedRedisListConfigurationQuery implements ListConfigurationQuery<VersionedRedisListConfigurationQuery> {

		private final VersionedRedisConfigurationSource source;
		
		private final String name;
		
		private final LinkedList<Parameter> parameters;
		
		/**
		 * 
		 * @param source
		 * @param name
		 */
		private VersionedRedisListConfigurationQuery(VersionedRedisConfigurationSource source, String name) {
			this.source = source;
			this.name = name;
			this.parameters = new LinkedList<>();
		}
		
		@Override
		public VersionedRedisListConfigurationQuery withParameters(List<Parameter> parameters) throws IllegalArgumentException {
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
		
		/**
		 * 
		 * @param exact
		 * @return 
		 */
		private Flux<ConfigurationProperty> execute(boolean exact) {
			String propertiesPattern = this.source.propertyKeyPrefix + this.name + "*";
			List<ConfigurationKey> defaultingMatchingKeys = this.source.defaultingStrategy.getListDefaultingKeys(new GenericConfigurationKey(this.name, this.parameters));
			
			return Mono.from(this.source.redisClient.connection(operations -> {
				// 1. Scan + filter keys
				return operations.scan()
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
					.flatMapIterable(result -> result.getKeys())
					.mapNotNull(rawKey -> {
						try {
							ConfigurationOptionParser<?> parser = new ConfigurationOptionParser<>(new StringProvider(rawKey.substring(this.source.propertyKeyPrefix.length())));
							ConfigurationKey key = parser.StartKey();
							return new VersionedRedisConfigurationKey(key.getName(), null, null, key.getParameters());
						}
						catch (ParseException e) {
							LOGGER.warn(() -> "Ignoring invalid key " + rawKey, e);
							return null;
						}
					})
					.filter(key -> {
						boolean currentExact = exact;
						for(ConfigurationKey machingKey : defaultingMatchingKeys) {
							if(key.matches(machingKey, currentExact)) {
								return true;
							}
							// we only want to include extra parameters for the query key
							currentExact = true;
						}
						return false;
					})
					.collectList()
					.flatMap(keys -> {
						// 2. Get Metadata
						return this.source.getMetaDataParameterSets(operations)
							.flatMap(metaDataParameterSets -> {
								Map<Integer, Map<List<Parameter>, List<VersionedRedisConfigurationKey>>> keysByMetaDataByCard = new TreeMap<>(Comparator.reverseOrder());
								for(VersionedRedisConfigurationKey key : keys) {
									Map<String, Parameter> parametersByKey = key.getParameters().stream().collect(Collectors.toMap(Parameter::getKey, Function.identity()));
									metaDataParameterSets.stream()
										.filter(set -> parametersByKey.keySet().containsAll(set))
										.map(parametersSet -> parametersSet.stream().map(parametersByKey::get).collect(Collectors.toList()))
										.forEach(metaKeyParameters -> {
											if(keysByMetaDataByCard.get(metaKeyParameters.size()) == null) {
												keysByMetaDataByCard.put(metaKeyParameters.size(), new HashMap<>());
											}
											Map<List<Parameter>, List<VersionedRedisConfigurationKey>> currentCard = keysByMetaDataByCard.get(metaKeyParameters.size());
											if(!currentCard.containsKey(metaKeyParameters)) {
												currentCard.put(metaKeyParameters, new ArrayList<>());
											}
											currentCard.get(metaKeyParameters).add(key);
										});
								}
								
								return Flux.fromIterable(keysByMetaDataByCard.values())
									.concatMap(keysByMetaData -> Flux.fromStream(() -> keysByMetaData.entrySet().stream().filter(e -> {
												List<VersionedRedisConfigurationKey> queries = e.getValue();
												for(Iterator<VersionedRedisConfigurationKey> keysIterator = queries.iterator(); keysIterator.hasNext();) {
													if(keysIterator.next().metaData.isPresent()) {
														keysIterator.remove();
													}
												}
												return !queries.isEmpty();
											})
										)
										.flatMap(e -> this.source.getMetaData(operations, e.getKey())
											.doOnNext(metaData -> {
												for(VersionedRedisConfigurationKey key : e.getValue()) {
													if(key.metaData.isPresent()) {
														throw new IllegalStateException("MetaData " + this.source.asMetaDataKey(e.getKey()) + " is conflicting with " + this.source.asMetaDataKey(key.metaData.get().getParameters()) + " when considering parameters [" + key.getParameters().stream().map(Parameter::toString).collect(Collectors.joining(", ")) + "]"); // TODO create an adhoc exception?
													}
													key.metaData = Optional.of(metaData);
												}
											})
										)
									)
									.then(Mono.just(keys));
							});
					});
			}))
			.flatMapMany(keys -> this.source.redisClient
				.batch(operations -> Flux.fromIterable(keys) // 3. Query properties in batch
					.map(key -> operations
						.zrangeWithScores()
						.reverse()
						.byScore()
						.limit(0, 1)
						.build(this.source.asPropertyKey(key), Bound.inclusive(0), key.metaData.flatMap(VersionedRedisConfigurationMetaData::getActiveRevision).map(Bound::inclusive).orElse(Bound.unbounded()))
						.next()
						.mapNotNull(result -> {
							key.revision = Optional.of((int)result.getScore());
							try {
								Optional<String> actualValue = new ConfigurationOptionParser<VersionedRedisConfigurationSource>(new StringProvider(result.getValue())).StartValueRevision();
								if(actualValue != null) {
									return new GenericConfigurationProperty<ConfigurationKey, VersionedRedisConfigurationSource, String>(key, actualValue.orElse(null), this.source);
								}
								else {
									// unset
									return new GenericConfigurationProperty<ConfigurationKey, VersionedRedisConfigurationSource, String>(key, this.source);
								}
							} 
							catch (ParseException e) {
								LOGGER.warn(() -> "Ignoring invalid value found for key " + key.toString() + " at revision " + (int)result.getScore(), e);
							}
							return null;
						})
					)
				)
			);
		}
	}
	
	/**
	 * <p>
	 * Converts the specified list of parameters to Redis MetaData key.
	 * </p>
	 *
	 * @param parameters a list of parameters
	 *
	 * @return a Redis MetaData key
	 */
	private String asMetaDataKey(List<Parameter> parameters) {
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(this.metadataKeyPrefix);
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
	public Mono<VersionedRedisConfigurationMetaData> getMetaData(String k1, Object v1) {
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
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<VersionedRedisConfigurationMetaData> getMetaData(String k1, Object v1, String k2, Object v2) throws IllegalArgumentException {
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
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<VersionedRedisConfigurationMetaData> getMetaData(String k1, Object v1, String k2, Object v2, String k3, Object v3) throws IllegalArgumentException {
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
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<VersionedRedisConfigurationMetaData> getMetaData(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) throws IllegalArgumentException {
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
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<VersionedRedisConfigurationMetaData> getMetaData(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5) throws IllegalArgumentException {
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
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<VersionedRedisConfigurationMetaData> getMetaData(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6) throws IllegalArgumentException {
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
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<VersionedRedisConfigurationMetaData> getMetaData(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7) throws IllegalArgumentException {
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
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<VersionedRedisConfigurationMetaData> getMetaData(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8) throws IllegalArgumentException {
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
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<VersionedRedisConfigurationMetaData> getMetaData(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9) throws IllegalArgumentException {
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
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<VersionedRedisConfigurationMetaData> getMetaData(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9, String k10, Object v10) throws IllegalArgumentException {
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
	 *
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
	 *
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
	 *
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
	 *
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
	 *
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
	 *
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
	 *
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
	 *
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
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	public Mono<Void> activate(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9, String k10, Object v10) throws IllegalArgumentException {
		return this.activate(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8), Parameter.of(k9, v9), Parameter.of(k10, v10));
	}

	/**
	 * <p>
	 * Activates the specified revisions for the properties defined with the specified parameter.
	 * </p>
	 *
	 * @param revision the revision to activate
	 * @param k1       the parameter name
	 * @param v1       the parameter value
	 *
	 * @return a mono that completes when the operation succeeds or fails
	 *
	 * @throws IllegalArgumentException if the specified revision is greater than the current working revision
	 */
	public Mono<Void> activate(int revision, String k1, Object v1) {
		return this.activate(revision, Parameter.of(k1, v1));
	}

	/**
	 * <p>
	 * Activates the specified revisions for the properties defined with the two specified parameters.
	 * </p>
	 *
	 * @param revision the revision to activate
	 * @param k1       the first parameter name
	 * @param v1       the first parameter value
	 * @param k2       the second parameter name
	 * @param v2       the second parameter value
	 *
	 * @return a mono that completes when the operation succeeds or fails
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once or if the specified revision is greater than the current working revision
	 */
	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2));
	}
	
	/**
	 * <p>
	 * Activates the specified revisions for the properties defined with the three specified parameters.
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
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once or if the specified revision is greater than the current working revision
	 */
	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2, String k3, Object v3) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3));
	}
	
	/**
	 * <p>
	 * Activates the specified revisions for the properties defined with the four specified parameters.
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
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once or if the specified revision is greater than the current working revision
	 */
	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4));
	}
	
	/**
	 * <p>
	 * Activates the specified revisions for the properties defined with the five specified parameters.
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
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once or if the specified revision is greater than the current working revision
	 */
	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5));
	}
	
	/**
	 * <p>
	 * Activates the specified revisions for the properties defined with the six specified parameters.
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
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once or if the specified revision is greater than the current working revision
	 */
	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6));
	}
	
	/**
	 * <p>
	 * Activates the specified revisions for the properties defined with the seven specified parameters.
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
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once or if the specified revision is greater than the current working revision
	 */
	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7));
	}
	
	/**
	 * <p>
	 * Activates the specified revisions for the properties defined with the eight specified parameters.
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
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once or if the specified revision is greater than the current working revision
	 */
	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8));
	}
	
	/**
	 * <p>
	 * Activates the specified revisions for the properties defined with the nine specified parameters.
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
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once or if the specified revision is greater than the current working revision
	 */
	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8), Parameter.of(k9, v9));
	}
	
	/**
	 * <p>
	 * Activates the specified revisions for the properties defined with the ten specified parameters.
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
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once or if the specified revision is greater than the current working revision
	 */
	public Mono<Void> activate(int revision, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9, String k10, Object v10) {
		return this.activate(revision, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5),  Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8), Parameter.of(k9, v9), Parameter.of(k10, v10));
	}
}
