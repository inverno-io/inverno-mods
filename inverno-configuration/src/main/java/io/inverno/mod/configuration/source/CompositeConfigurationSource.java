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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.inverno.mod.configuration.ConfigurationKey;
import io.inverno.mod.configuration.ConfigurationKey.Parameter;
import io.inverno.mod.configuration.ConfigurationProperty;
import io.inverno.mod.configuration.ConfigurationQuery;
import io.inverno.mod.configuration.ConfigurationQueryResult;
import io.inverno.mod.configuration.ConfigurationSource;
import io.inverno.mod.configuration.ConfigurationSourceException;
import io.inverno.mod.configuration.ExecutableConfigurationQuery;
import io.inverno.mod.configuration.ListConfigurationQuery;
import io.inverno.mod.configuration.internal.GenericConfigurationKey;
import io.inverno.mod.configuration.internal.GenericConfigurationQueryResult;
import reactor.core.publisher.Flux;

/**
 * <p>
 * A composite configuration source that uses multiple configuration sources to resolve configuration properties.
 * </p>
 *
 * <p>
 * A composite configuration source uses a {@link CompositeConfigurationStrategy} to determine the best matching value among the different sources for a given query.
 * </p>
 *
 * <p>
 * This allows to define priorities to the configuration sources so that if two sources define a value for a given property the value of the source with the highest priority is chosen. It also allows
 * to implement more complex resolving strategies to determine the best matching value whose key does not necessarily exactly match the query. This is especially usefull when one needs to define a
 * base configuration and customize it based on a context given by configuration parameters.
 * </p>
 *
 * <p>
 * A composite configuration source queries its sources in sequence.
 * </p>
 *
 * <p>
 * At each round, the actual queries executed on the source are populated by the strategy (@see
 * {@link CompositeConfigurationStrategy#populateSourceQuery(ConfigurationKey, ConfigurationQuery, ConfigurationProperty)}) so that multiple queries can actually be requested for a single original
 * query to the composite source.
 * </p>
 *
 * <p>
 * It then retains the first non-empty result that supersedes the one resolved in previous rounds for that original query (see @link
 * {@link CompositeConfigurationStrategy#isSuperseded(ConfigurationKey, ConfigurationProperty, ConfigurationProperty)}}.
 * </p>
 *
 * <p>
 * A property value is retained and the sequence stops when the query is considered as resolved according to the strategy (see
 * {@link CompositeConfigurationStrategy#isResolved(ConfigurationKey, ConfigurationProperty)}) or if there's no more source to query.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see ConfigurationSource
 * @see CompositeConfigurationStrategy
 */
public class CompositeConfigurationSource implements ConfigurationSource<CompositeConfigurationSource.CompositeConfigurationQuery, CompositeConfigurationSource.CompositeExecutableConfigurationQuery, CompositeConfigurationSource.CompositeListConfigurationQuery> {

	/**
	 * The configuration sources.
	 */
	protected List<ConfigurationSource<?,?,?>> sources;
	
	private CompositeConfigurationStrategy strategy;
	
	/**
	 * <p>
	 * Creates a composite configuration query with the specified list of sources using the default strategy.
	 * </p>
	 *
	 * @param sources a list of configuration sources from the highest priority to the lowest
	 *
	 * @throws NullPointerException if the specified strategy is null
	 *
	 * @see DefaultCompositeConfigurationStrategy
	 */
	public CompositeConfigurationSource(List<ConfigurationSource<?,?,?>> sources) throws NullPointerException {
		this(sources, new DefaultCompositeConfigurationStrategy());
	}
	
	/**
	 * <p>
	 * Creates a composite configuration query with the specified list of sources using the specified strategy.
	 * </p>
	 *
	 * @param sources  a list of configuration sources from the highest priority to the lowest
	 * @param strategy a composite configuration strategy
	 *
	 * @throws NullPointerException if the specified strategy is null
	 *
	 * @see CompositeConfigurationStrategy
	 */
	public CompositeConfigurationSource(List<ConfigurationSource<?,?,?>> sources, CompositeConfigurationStrategy strategy) throws NullPointerException {
		this.sources = sources != null ? sources.stream().filter(Objects::nonNull).collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList)) : List.of();
		this.strategy = Objects.requireNonNull(strategy, "strategy");
	}
	
	/**
	 * <p>
	 * Returns the list of configuration sources from the highest priority to the lowest.
	 * </p>
	 * 
	 * @return a list of configuration sources
	 */
	public List<ConfigurationSource<?,?,?>> getSources() {
		return sources;
	}
	
	/**
	 * <p>
	 * Returns the composite configuration strategy.
	 * </p>
	 * 
	 * @return the composite configuration strategy
	 */
	public CompositeConfigurationStrategy getStrategy() {
		return strategy;
	}
	
	/**
	 * <p>
	 * Sets the composite configuration strategy.
	 * </p>
	 * 
	 * @param strategy a strategy
	 * 
	 * @throws NullPointerException if the strategy is null
	 */
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

	@Override
	public CompositeListConfigurationQuery list(String name) throws IllegalArgumentException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
	@SuppressWarnings("rawtypes")
	private static class SourceConfigurationQueryWrapper implements ConfigurationQuery {

		private final ConfigurationSource source;
		
		private int counter;
		
		private ConfigurationQuery query;
		
		private SourceExecutableConfigurationQueryWrapper executableQueryWrapper;
		
		private SourceConfigurationQueryWrapper(ConfigurationSource<?,?,?> source) {
			this.source = source;
		}
		
		@SuppressWarnings("unchecked")
		public Flux<ConfigurationQueryResult> execute() {
			if(this.executableQueryWrapper != null) {
				return this.executableQueryWrapper.query.execute();
			}
			else {
				return Flux.empty();
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

		private final SourceConfigurationQueryWrapper queryWrapper;

		private ExecutableConfigurationQuery query;
		
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
		public Flux execute() {
			throw new IllegalStateException("You can't execute a query provided to a composite configuration strategy.");
		}
	}
	
	/**
	 * <p>
	 * The configuration query used by the composite configuration source.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see ConfigurationQuery
	 */
	public static class CompositeConfigurationQuery implements ConfigurationQuery<CompositeConfigurationQuery, CompositeExecutableConfigurationQuery> {

		private final CompositeExecutableConfigurationQuery executableQuery;
		
		private final List<String> names;
		
		private final LinkedList<Parameter> parameters;
		
		private CompositeConfigurationQuery(CompositeExecutableConfigurationQuery executableQuery) {
			this.executableQuery = executableQuery;
			this.names = new LinkedList<>();
			this.parameters = new LinkedList<>();
		}
		
		@Override
		public CompositeExecutableConfigurationQuery get(String... names) throws IllegalArgumentException {
			if(names == null || names.length == 0) {
				throw new IllegalArgumentException("You can't query an empty list of configuration properties");
			}
			this.names.addAll(Arrays.asList(names));
			return this.executableQuery;
		}
	}
	
	/**
	 * <p>
	 * The executable configuration query used by the composite configuration source.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see ExecutableConfigurationQuery
	 */
	public static class CompositeExecutableConfigurationQuery implements ExecutableConfigurationQuery<CompositeConfigurationQuery, CompositeExecutableConfigurationQuery> {

		private final CompositeConfigurationSource source;
		
		private final LinkedList<CompositeConfigurationQuery> queries;
		
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
		public CompositeExecutableConfigurationQuery withParameters(Parameter... parameters) throws IllegalArgumentException {
			if(parameters != null && parameters.length > 0) {
				CompositeConfigurationQuery currentQuery = this.queries.peekLast();
				Set<String> parameterKeys = new HashSet<>();
				currentQuery.parameters.clear();
				List<String> duplicateParameters = new LinkedList<>();
				for(Parameter parameter : parameters) {
					currentQuery.parameters.add(parameter);
					if(!parameterKeys.add(parameter.getKey())) {
						duplicateParameters.add(parameter.getKey());
					}
				}
				if(duplicateParameters.size() > 0) {
					throw new IllegalArgumentException("The following parameters were specified more than once: " + duplicateParameters.stream().collect(Collectors.joining(", ")));
				}
			}
			return this;
		}

		@Override
		public Flux<ConfigurationQueryResult> execute() {
			return Flux.create(sink -> {
				LinkedList<CompositeConfigurationQueryResult> results = this.queries.stream()
					.flatMap(query -> query.names.stream().map(name -> new CompositeConfigurationQueryResult(this.source.strategy, new GenericConfigurationKey(name, query.parameters))))
					.collect(Collectors.toCollection(LinkedList::new));
				
				 Flux.fromIterable(this.source.sources)
					.flatMapSequential(source -> {
						SourceConfigurationQueryWrapper wrappedQuery = new SourceConfigurationQueryWrapper(source);
						List<CompositeConfigurationQueryResult> unresolvedResults = results.stream().filter(result -> !result.isResolved()).collect(Collectors.toList());
						unresolvedResults.forEach(result -> {
							wrappedQuery.reset();
							result.populateSourceQuery(wrappedQuery);
						});
						
						ListIterator<CompositeConfigurationQueryResult> unresolvedResultsIterator = unresolvedResults.listIterator();
						return wrappedQuery.execute()
							.map(sourceResult -> {
								CompositeConfigurationQueryResult currentResult = unresolvedResultsIterator.next();
								if(!currentResult.consumeResult(sourceResult)) {
									unresolvedResultsIterator.previous();
								}
								return currentResult;
							})
							.doOnComplete(() -> {
								while(!results.isEmpty() && results.peek().isResolved()) {
									sink.next(results.poll());
								}
							});
					})
				 	.subscribe(
				 		ign -> {},
				 		e -> sink.error(e),
				 		() -> {
				 			results.forEach(sink::next);
				 			sink.complete();
				 		}
				 	);
			});
		}
	}
	
	/**
	 * <p>
	 * The configuration query result returned by a composite configuration source.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see ConfigurationQueryResult
	 */
	public static class CompositeConfigurationQueryResult extends GenericConfigurationQueryResult {

		private final CompositeConfigurationStrategy strategy;
		
		private int counter;
		
		private boolean resolved;
		
		private CompositeConfigurationQueryResult(CompositeConfigurationStrategy strategy, ConfigurationKey queryKey) {
			super(queryKey, (ConfigurationProperty)null);
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
		
		private int consumeCounter = 0;
		
		private boolean consumeResult(ConfigurationQueryResult result) {
			if(this.consumeCounter == 0) {
				this.consumeCounter = this.counter;
			}
			if(this.consumeCounter > 0) {
				try {
					if(!this.resolved) {
						result.getResult().ifPresent(property -> {
							if(this.strategy.isSuperseded(this.queryKey, this.queryResult.orElse(null), property)) {
								this.queryResult = property.isUnset() ? Optional.empty() : Optional.of(property);
								this.resolved = this.strategy.isResolved(this.queryKey, property);
							}
						});
					}
				} 
				catch (ConfigurationSourceException e) {
					// We have two choices:
					// - set the failed query result which can possibly be overridden by subsequent sources
					// - report the failure and resume the defaulting mechanism 
					// The issue is that we don't know whether the error is related to an invalid value or an unreachable configuration source, in the latter case, we can't assume 
					// We should delegate this to the strategy so it can decide what to do
					// if the strategy does not ignore failure then if an error occurs the result is set to fail otherwise the failed result is ignored and the process continue 
					if(!this.strategy.ignoreFailure(e)) {
						this.error = e.getCause();
						this.errorSource = e.getSource();
					}
				}
				finally {
					this.consumeCounter--;
				}
			}
			return this.consumeCounter == 0;
		}
	}
	
	public static class CompositeListConfigurationQuery implements ListConfigurationQuery<CompositeListConfigurationQuery> {

		@Override
		public CompositeListConfigurationQuery withParameters(Parameter... parameters) throws IllegalArgumentException {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public List<ConfigurationProperty> execute() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public List<ConfigurationProperty> executeAll() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}
	}
}
