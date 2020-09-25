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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.winterframework.mod.configuration.ConfigurationEntry;
import io.winterframework.mod.configuration.ConfigurationKey;
import io.winterframework.mod.configuration.ConfigurationQuery;
import io.winterframework.mod.configuration.ConfigurationQueryResult;
import io.winterframework.mod.configuration.ConfigurationSource;
import io.winterframework.mod.configuration.ExecutableConfigurationQuery;
import io.winterframework.mod.configuration.internal.GenericConfigurationKey;
import io.winterframework.mod.configuration.internal.GenericConfigurationQueryResult;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

/**
 * @author jkuhn
 *
 */
public class CompositeConfigurationSource implements ConfigurationSource<CompositeConfigurationSource.CompositeConfigurationQuery, CompositeConfigurationSource.CompositeExecutableConfigurationQuery, CompositeConfigurationSource.CompositeConfigurationQueryResult> {

	private List<ConfigurationSource<?,?,?>> sources;
	
	private CompositeConfigurationStrategy strategy;
	
	public CompositeConfigurationSource(List<ConfigurationSource<?,?,?>> sources) {
		this.sources = sources != null ? Collections.unmodifiableList(sources) : List.of();
		this.strategy = new DefaultCompositeConfigurationStrategy();
	}
	
	public List<ConfigurationSource<?, ?, ?>> getSources() {
		return sources;
	}
	
	public CompositeConfigurationStrategy getStrategy() {
		return strategy;
	}
	
	public void setStrategy(CompositeConfigurationStrategy strategy) throws NullPointerException {
		if(strategy == null) {
			throw new NullPointerException("Strategy can't be null");
		}
		this.strategy = strategy;
	}
	
	@Override
	public CompositeExecutableConfigurationQuery get(String... names) throws IllegalArgumentException {
		return new CompositeExecutableConfigurationQuery(this).and().get(names);
	}
	
	@SuppressWarnings("rawtypes")
	private static class SourceConfigurationQueryWrapper implements ConfigurationQuery {

		private int counter;
		
		private ConfigurationSource source;
		
		private ConfigurationQuery query;
		
		private SourceExecutableConfigurationQueryWrapper executableQueryWrapper;
		
		private SourceConfigurationQueryWrapper(ConfigurationSource<?,?,?> source) {
			this.source = source;
		}
		
		@SuppressWarnings("unchecked")
		public Mono<List<ConfigurationQueryResult<?,?>>> execute() {
			if(this.executableQueryWrapper != null) {
				return this.executableQueryWrapper.query.execute();
			}
			else {
				return Mono.just(List.of());
			}
		}
		
		public int getCounter() {
			return this.counter;
		}
		
		public void reset() {
			this.counter = 0;
			if(this.executableQueryWrapper != null) {
				this.executableQueryWrapper.and();
			}
		}
		
		@Override
		public ExecutableConfigurationQuery get(String... names) {
			this.counter += names.length;
			ExecutableConfigurationQuery executableQuery;
			if(this.query == null) {
				executableQuery = this.source.get(names);
			}
			else {
				executableQuery = this.query.get(names);
			}
			
			if(this.executableQueryWrapper == null) {
				this.executableQueryWrapper = new SourceExecutableConfigurationQueryWrapper(this);
			}
			this.executableQueryWrapper.query = executableQuery;
			return this.executableQueryWrapper;
		}
	}
	
	@SuppressWarnings("rawtypes")
	private static class SourceExecutableConfigurationQueryWrapper implements ExecutableConfigurationQuery {

		private ExecutableConfigurationQuery query;
		
		private SourceConfigurationQueryWrapper queryWrapper;
		
		private SourceExecutableConfigurationQueryWrapper(SourceConfigurationQueryWrapper queryWrapper) {
			this.queryWrapper = queryWrapper;
		}
		
		@Override
		public ExecutableConfigurationQuery withParameters(Parameter... parameters) throws IllegalArgumentException {
			this.query = this.query.withParameters(parameters);
			return this;
		}
		
		@Override
		public ConfigurationQuery and() {
			this.queryWrapper.query = this.query.and();
			return this.queryWrapper;
		}

		@Override
		public Mono execute() {
			throw new IllegalStateException("You can't execute a query provided to a composite configuration strategy.");
		}
	}
	
	public static class CompositeConfigurationQuery implements ConfigurationQuery<CompositeConfigurationQuery, CompositeExecutableConfigurationQuery, CompositeConfigurationQueryResult> {

		private CompositeExecutableConfigurationQuery executableQuery;
		
		private List<String> names;
		
		private LinkedHashMap<String, Object> parameters;
		
		private CompositeConfigurationQuery(CompositeExecutableConfigurationQuery executableQuery) {
			this.executableQuery = executableQuery;
			this.names = new LinkedList<>();
			this.parameters = new LinkedHashMap<>();
		}
		
		@Override
		public CompositeExecutableConfigurationQuery get(String... names) throws IllegalArgumentException {
			if(names == null || names.length == 0) {
				throw new IllegalArgumentException("You can't query an empty list of configuration entries");
			}
			this.names.addAll(Arrays.asList(names));
			return this.executableQuery;
		}
	}
	
	public static class CompositeExecutableConfigurationQuery implements ExecutableConfigurationQuery<CompositeConfigurationQuery, CompositeExecutableConfigurationQuery, CompositeConfigurationQueryResult> {

		private CompositeConfigurationSource source;
		
		private LinkedList<CompositeConfigurationQuery> queries;
		
		private CompositeExecutableConfigurationQuery(CompositeConfigurationSource source) {
			this.source = source;
			this.queries = new LinkedList<>();
		}
		
		@Override
		public CompositeConfigurationQuery and() {
			this.queries.add(new CompositeConfigurationQuery(this));
			return this.queries.peekLast();
		}
		
		@Override
		public ExecutableConfigurationQuery<CompositeConfigurationQuery, CompositeExecutableConfigurationQuery, CompositeConfigurationQueryResult> withParameters(Parameter... parameters) throws IllegalArgumentException {
			if(parameters != null && parameters.length > 0) {
				CompositeConfigurationQuery currentQuery = this.queries.peekLast();
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
		public Mono<List<CompositeConfigurationQueryResult>> execute() {
			return Mono.defer(() -> {
				return Mono.just(this.queries.stream()
						.flatMap(query -> query.names.stream().map(name -> new CompositeConfigurationQueryResult(this.source.strategy, new GenericConfigurationKey(name, query.parameters))))
						.collect(Collectors.toList())
					)
					.zipWhen(results -> {
						Mono<List<CompositeConfigurationQueryResult>> resultMono = Mono.just(results);
						for(ConfigurationSource<?,?,?> source : this.source.sources) {
							SourceConfigurationQueryWrapper wrappedQuery = new SourceConfigurationQueryWrapper(source);
							resultMono = resultMono
								.map(l -> l.stream()
									.filter(result -> !result.isResolved())
									.map(result -> {
										wrappedQuery.reset();
										result.populateSourceQuery(wrappedQuery);
										return result;
									})
									.collect(Collectors.toList())
								)
								.zipWhen(result -> wrappedQuery.execute()).map(tuple -> {
									Iterator<? extends ConfigurationQueryResult<?,?>> sourceResultIterator = tuple.getT2().iterator();
									for(CompositeConfigurationQueryResult queryResult : tuple.getT1()) {
										queryResult.consumeResults(sourceResultIterator);
									}
									return tuple.getT1();
								});
						}
						return resultMono;
					})
					.map(Tuple2::getT1);
			});
		}
	}
	
	public static class CompositeConfigurationQueryResult extends GenericConfigurationQueryResult<ConfigurationKey, ConfigurationEntry<?, ?>> {

		private CompositeConfigurationStrategy strategy;
		
		private int counter;
		
		private boolean resolved;
		
		private CompositeConfigurationQueryResult(CompositeConfigurationStrategy strategy, ConfigurationKey queryKey) {
			super(queryKey, null);
			this.strategy = strategy;
		}
		
		private boolean isResolved() {
			return this.resolved;
		}
		
		private SourceExecutableConfigurationQueryWrapper populateSourceQuery(SourceConfigurationQueryWrapper queryWrapper) {
			SourceExecutableConfigurationQueryWrapper executableQueryWrapper = (SourceExecutableConfigurationQueryWrapper)this.strategy.populateSourceQuery(this.queryKey, queryWrapper, this.queryResult.orElse(null));
			if(executableQueryWrapper != null && executableQueryWrapper.queryWrapper != queryWrapper) {
				// Make sure the strategy respects the rules
				throw new IllegalStateException("Broken query chain");
			}
			this.counter = queryWrapper.getCounter();
			return executableQueryWrapper;
		}
		
		private void consumeResults(Iterator<? extends ConfigurationQueryResult<?,?>> results) {
			for(int i=0;i<this.counter;i++) {
				results.next().getResult().ifPresent(result -> {
					if(this.strategy.isSuperseded(this.queryKey, this.queryResult.orElse(null), result)) {
						this.queryResult = Optional.of(result);
						this.resolved = this.strategy.isResolved(this.queryKey, result);
					}
				});
			}
		}
	}
}
