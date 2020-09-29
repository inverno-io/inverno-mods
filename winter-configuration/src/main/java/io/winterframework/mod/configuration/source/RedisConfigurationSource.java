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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.lettuce.core.Limit;
import io.lettuce.core.Range;
import io.lettuce.core.Range.Boundary;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.winterframework.mod.configuration.AbstractConfigurationSource;
import io.winterframework.mod.configuration.ConfigurationEntry;
import io.winterframework.mod.configuration.ConfigurationKey;
import io.winterframework.mod.configuration.ConfigurationQuery;
import io.winterframework.mod.configuration.ExecutableConfigurationQuery;
import io.winterframework.mod.configuration.ValueConverter;
import io.winterframework.mod.configuration.internal.GenericConfigurationEntry;
import io.winterframework.mod.configuration.internal.GenericConfigurationKey;
import io.winterframework.mod.configuration.internal.GenericConfigurationQueryResult;
import io.winterframework.mod.configuration.internal.parser.option.ConfigurationOptionParser;
import io.winterframework.mod.configuration.internal.parser.option.ParseException;
import io.winterframework.mod.configuration.internal.parser.option.StringProvider;
import reactor.core.publisher.Flux;

/**
 * @author jkuhn
 *
 */
public class RedisConfigurationSource extends AbstractConfigurationSource<RedisConfigurationSource.RedisConfigurationQuery, RedisConfigurationSource.RedisExecutableConfigurationQuery, RedisConfigurationSource.RedisConfigurationQueryResult, String> {

	private RedisReactiveCommands<String, String> commands;
	
	public RedisConfigurationSource(ValueConverter<String> converter, RedisClient redisClient) {
		super(converter);
		this.commands = redisClient.connect().reactive();
	}
	
	@Override
	public RedisExecutableConfigurationQuery get(String... names) throws IllegalArgumentException {
		return new RedisExecutableConfigurationQuery(this).and().get(names);
	}

	public static class RedisConfigurationKey extends GenericConfigurationKey {

		private int revision;
		
		private RedisConfigurationKey(String name, int revision) {
			this(name, revision, null);
		}
		
		private RedisConfigurationKey(String name, int revision, Map<String, Object> parameters) {
			super(name, parameters);
			this.revision = revision;
		}
		
		public int getRevision() {
			return revision;
		}
	}
	
	public static class RedisConfigurationQuery implements ConfigurationQuery<RedisConfigurationQuery, RedisExecutableConfigurationQuery, RedisConfigurationQueryResult> {

		private RedisExecutableConfigurationQuery executableQuery;
		
		private List<String> names;
		
		private LinkedHashMap<String, Object> parameters;
		
		private int revision = -1;
		
		private RedisConfigurationQuery(RedisExecutableConfigurationQuery executableQuery) {
			this.executableQuery = executableQuery;
			this.names = new LinkedList<>();
			this.parameters = new LinkedHashMap<>();
		}
		
		@Override
		public RedisExecutableConfigurationQuery get(String... names) throws IllegalArgumentException {
			if(names == null || names.length == 0) {
				throw new IllegalArgumentException("You can't query an empty list of configuration entries");
			}
			this.names.addAll(Arrays.asList(names));
			return this.executableQuery;
		}
	}
	
	public static class RedisExecutableConfigurationQuery implements ExecutableConfigurationQuery<RedisConfigurationQuery, RedisExecutableConfigurationQuery, RedisConfigurationQueryResult> {

		private RedisConfigurationSource source;
		
		private LinkedList<RedisConfigurationQuery> queries;
		
		public RedisExecutableConfigurationQuery(RedisConfigurationSource source) {
			this.source = source;
			this.queries = new LinkedList<>();
		}
		
		@Override
		public RedisConfigurationQuery and() {
			this.queries.add(new RedisConfigurationQuery(this));
			return this.queries.peekLast();
		}

		@Override
		public ExecutableConfigurationQuery<RedisConfigurationQuery, RedisExecutableConfigurationQuery, RedisConfigurationQueryResult> withParameters(Parameter... parameters) throws IllegalArgumentException {
			if(parameters != null && parameters.length > 0) {
				RedisConfigurationQuery currentQuery = this.queries.peekLast();
				currentQuery.parameters.clear();
				String duplicateParameters = "";
				for(Parameter parameter : parameters) {
					if(currentQuery.parameters.put(parameter.getName(), parameter.getValue()) != null) {
						duplicateParameters += parameter.getName();
					}
				}
				if(duplicateParameters != null && duplicateParameters.length() > 0) {
					throw new IllegalArgumentException("The following parameters were specified more than once: " + duplicateParameters);
				}
			}
			return this;
		}
		
		@Override
		public Flux<RedisConfigurationQueryResult> execute() {
			return Flux.fromStream(this.queries.stream())
				.flatMap(query -> {
					List<Entry<String, Object>> parameters = new ArrayList<>(query.parameters.entrySet());
					return Flux.fromStream(IntStream.range(0, query.parameters.size() + 1)
							.mapToObj(i -> "WCONF[" + parameters.subList(0, parameters.size() - i).stream()
							.sorted(Comparator.comparing(Entry::getKey))
							.map(e -> ExecutableConfigurationQuery.parameter(e.getKey(), e.getValue()).toString())
							.collect(Collectors.joining(",")) + "]")
						)
						.flatMap(key -> this.source.commands.get(key)
							.map(result -> Optional.of(Integer.parseInt(result)))
							.defaultIfEmpty(Optional.empty())
						)
						.doOnNext(result -> result.ifPresent(revision -> query.revision = revision));
				})
				.thenMany(Flux.fromStream(this.queries.stream()))
				.flatMap(query -> Flux.fromStream(query.names.stream().map(name -> new RedisConfigurationKey(name, query.revision, query.parameters)))
					.flatMap(queryKey -> this.source.commands
						.zrevrangebyscoreWithScores("WCONF:" + queryKey.toString(), Range.from(Boundary.including(0), queryKey.revision != -1 ? Boundary.including(queryKey.revision) : Boundary.unbounded()), Limit.create(0, 1))
						.next()
						.map(result -> Optional.of(result))
						.defaultIfEmpty(Optional.empty())
						.map(result -> result.map(value -> {
								try {
									Optional<String> actualValue = new ConfigurationOptionParser<RedisConfigurationSource>(new StringProvider(value.getValue())).StartValueRevision();
									if(actualValue != null) {
										return new RedisConfigurationQueryResult(queryKey, new GenericConfigurationEntry<ConfigurationKey, RedisConfigurationSource, String>(new RedisConfigurationKey(queryKey.getName(), (int)value.getScore(), queryKey.getParameters()), actualValue.orElse(null), this.source));
									}
									else {
										// unset
										return new RedisConfigurationQueryResult(queryKey, new GenericConfigurationEntry<ConfigurationKey, RedisConfigurationSource, String>(new RedisConfigurationKey(queryKey.getName(), (int)value.getScore(), queryKey.getParameters()), this.source));
									}
								} 
								catch (ParseException e) {
									return new RedisConfigurationQueryResult(queryKey, this.source, new IllegalStateException("Invalid value found for key " + queryKey.toString() + " at revision " + (int)value.getScore(), e));
								}
							})
							.orElse(new RedisConfigurationQueryResult(queryKey, (ConfigurationEntry<?, ?>)null))
						)
					)
				);
		}
	}
	
	public static class RedisConfigurationQueryResult extends GenericConfigurationQueryResult<RedisConfigurationKey, ConfigurationEntry<?, ?>> {

		public RedisConfigurationQueryResult(RedisConfigurationKey queryKey, ConfigurationEntry<?, ?> queryResult) {
			super(queryKey, queryResult);
		}

		public RedisConfigurationQueryResult(RedisConfigurationKey queryKey, RedisConfigurationSource source, Throwable error) {
			super(queryKey, source, error);
		}
	}
}
