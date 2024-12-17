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

import io.inverno.mod.configuration.ConfigurationKey;
import io.inverno.mod.configuration.ConfigurationProperty;
import io.inverno.mod.configuration.ConfigurationQuery;
import io.inverno.mod.configuration.ConfigurationQueryResult;
import io.inverno.mod.configuration.ConfigurationSource;
import io.inverno.mod.configuration.ConfigurationSourceException;
import io.inverno.mod.configuration.DefaultableConfigurationSource;
import io.inverno.mod.configuration.ExecutableConfigurationQuery;
import io.inverno.mod.configuration.ListConfigurationQuery;
import io.inverno.mod.configuration.internal.GenericConfigurationKey;
import io.inverno.mod.configuration.internal.GenericConfigurationQueryResult;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
 * A composite configuration source queries its sources in sequence. It first applies a new {@link CompositeConfigurationStrategy.CompositeDefaultingStrategy} provided by
 * {@link CompositeConfigurationStrategy#createDefaultingStrategy()} to the sources that implement {@link DefaultableConfigurationSource}. This allows to specify a defaulting strategy when
 * executing queries on each sources. The composite defaulting strategy allows to reduce the queries to execute on each source by keeping track of intermediate results in each round.
 * </p>
 * 
 * <p>
 * Configuration sources are ordered from the highest to lowest priority, the strategy allows to specify whether a result returned by a source for a given query supersedes the result returned in a
 * previous round for the same query (see
 * {@link CompositeConfigurationStrategy#isSuperseded(io.inverno.mod.configuration.ConfigurationKey, io.inverno.mod.configuration.ConfigurationKey, io.inverno.mod.configuration.ConfigurationKey)}).
 * </p>
 *
 * <p>
 * At the end of each round, the strategy is used to determine whether a result resolves a query (see
 * {@link CompositeConfigurationStrategy#isResolved(io.inverno.mod.configuration.ConfigurationKey, io.inverno.mod.configuration.ConfigurationKey)}), in which case the query is removed from the list
 * of queries to execute on subsequent sources.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see ConfigurationSource
 * @see CompositeConfigurationStrategy
 */
public class CompositeConfigurationSource implements ConfigurationSource {

	private final CompositeConfigurationSource original;

	/**
	 * The configuration sources.
	 */
	private final List<ConfigurationSource> sources;

	private final List<ConfigurationKey.Parameter> defaultParameters;

	private CompositeConfigurationStrategy strategy;

	/**
	 * <p>
	 * Creates a new composite configuration source with the specified sources ordered from the highest to lowest priority which uses the default {@link CompositeConfigurationStrategy#lookup()}
	 * strategy.
	 * </p>
	 *
	 * @param sources a list of configuration sources
	 */
	public CompositeConfigurationSource(List<ConfigurationSource> sources) {
		this(sources, List.of(), CompositeConfigurationStrategy.lookup());
	}

	/**
	 * <p>
	 * Creates a new composite configuration source with the specified sources ordered from the highest to lowest priority which applies the specified default parameters and uses the default
	 * {@link CompositeConfigurationStrategy#lookup()} strategy.
	 * </p>
	 *
	 * @param sources           a list of configuration sources
	 * @param defaultParameters the default parameters to apply
	 */
	public CompositeConfigurationSource(List<ConfigurationSource> sources, List<ConfigurationKey.Parameter> defaultParameters) {
		this(sources, defaultParameters, CompositeConfigurationStrategy.lookup());
	}

	/**
	 * <p>
	 * Creates a new composite configuration source with the specified sources ordered from the highest to lowest priority which uses the specified strategy.
	 * </p>
	 *
	 * @param sources           a list of configuration sources
	 * @param strategy          a composite configuration strategy
	 */
	public CompositeConfigurationSource(List<ConfigurationSource> sources, CompositeConfigurationStrategy strategy) {
		this(sources, List.of(), strategy);
	}

	/**
	 * <p>
	 * Creates a new composite configuration source with the specified sources ordered from the highest to lowest priority which applies the specified default parameters and uses the specified
	 * strategy.
	 * </p>
	 *
	 * @param sources           a list of configuration sources
	 * @param defaultParameters the default parameters to apply
	 * @param strategy          a composite configuration strategy
	 */
	public CompositeConfigurationSource(List<ConfigurationSource> sources, List<ConfigurationKey.Parameter> defaultParameters, CompositeConfigurationStrategy strategy) {
		this.original = null;
		this.sources = sources;
		this.defaultParameters = Collections.unmodifiableList(GenericConfigurationKey.requireDistinctParameters(defaultParameters));
		this.strategy = strategy;
	}

	/**
	 * <p>
	 * Creates a composite configuration source from the specified original source which applies the specified default parameters.
	 * </p>
	 *
	 * @param original          the original configuration source
	 * @param defaultParameters the default parameters to apply
	 */
	private CompositeConfigurationSource(CompositeConfigurationSource original, List<ConfigurationKey.Parameter> defaultParameters) {
		this.original = original;
		this.sources = original.sources;
		this.defaultParameters = Collections.unmodifiableList(GenericConfigurationKey.requireDistinctParameters(defaultParameters));
		this.strategy = original.strategy;
	}

	@Override
	public final CompositeConfigurationSource withParameters(String k1, Object v1) {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1)));
	}

	@Override
	public final CompositeConfigurationSource withParameters(String k1, Object v1, String k2, Object v2) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2)));
	}

	@Override
	public final CompositeConfigurationSource withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2), ConfigurationKey.Parameter.of(k3, v3)));
	}

	@Override
	public final CompositeConfigurationSource withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2), ConfigurationKey.Parameter.of(k3, v3), ConfigurationKey.Parameter.of(k4, v4)));
	}

	@Override
	public final CompositeConfigurationSource withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2), ConfigurationKey.Parameter.of(k3, v3), ConfigurationKey.Parameter.of(k4, v4), ConfigurationKey.Parameter.of(k5, v5)));
	}

	@Override
	public final CompositeConfigurationSource withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2), ConfigurationKey.Parameter.of(k3, v3), ConfigurationKey.Parameter.of(k4, v4), ConfigurationKey.Parameter.of(k5, v5), ConfigurationKey.Parameter.of(k6, v6)));
	}

	@Override
	public final CompositeConfigurationSource withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2), ConfigurationKey.Parameter.of(k3, v3), ConfigurationKey.Parameter.of(k4, v4), ConfigurationKey.Parameter.of(k5, v5), ConfigurationKey.Parameter.of(k6, v6), ConfigurationKey.Parameter.of(k7, v7)));
	}

	@Override
	public final CompositeConfigurationSource withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2), ConfigurationKey.Parameter.of(k3, v3), ConfigurationKey.Parameter.of(k4, v4), ConfigurationKey.Parameter.of(k5, v5), ConfigurationKey.Parameter.of(k6, v6), ConfigurationKey.Parameter.of(k7, v7), ConfigurationKey.Parameter.of(k8, v8)));
	}

	@Override
	public final CompositeConfigurationSource withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2), ConfigurationKey.Parameter.of(k3, v3), ConfigurationKey.Parameter.of(k4, v4), ConfigurationKey.Parameter.of(k5, v5), ConfigurationKey.Parameter.of(k6, v6), ConfigurationKey.Parameter.of(k7, v7), ConfigurationKey.Parameter.of(k8, v8), ConfigurationKey.Parameter.of(k9, v9)));
	}

	@Override
	public final CompositeConfigurationSource withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9, String k10, Object v10) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2), ConfigurationKey.Parameter.of(k3, v3), ConfigurationKey.Parameter.of(k4, v4), ConfigurationKey.Parameter.of(k5, v5), ConfigurationKey.Parameter.of(k6, v6), ConfigurationKey.Parameter.of(k7, v7), ConfigurationKey.Parameter.of(k8, v8), ConfigurationKey.Parameter.of(k9, v9), ConfigurationKey.Parameter.of(k10, v10)));
	}

	@Override
	public final CompositeConfigurationSource withParameters(ConfigurationKey.Parameter... parameters) throws IllegalArgumentException {
		return this.withParameters(parameters != null ? Arrays.asList(parameters) : List.of());
	}

	@Override
	public CompositeConfigurationSource withParameters(List<ConfigurationKey.Parameter> parameters) throws IllegalArgumentException {
		return new CompositeConfigurationSource(this, parameters);
	}

	@Override
	public CompositeConfigurationSource unwrap() {
		CompositeConfigurationSource current = this;
		while(current.original != null) {
			current = current.original;
		}
		return current;
	}

	/**
	 * <p>
	 * Returns the list of configuration sources from the highest priority to the lowest.
	 * </p>
	 *
	 * @return a list of configuration sources
	 */
	public List<ConfigurationSource> getSources() {
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
		if (strategy == null) {
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
		return new CompositeListConfigurationQuery(this, name);
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
	public static class CompositeConfigurationQuery implements ConfigurationQuery<CompositeConfigurationQuery, CompositeConfigurationSource.CompositeExecutableConfigurationQuery> {

		private final CompositeExecutableConfigurationQuery executableQuery;

		private final List<String> names;

		private List<ConfigurationKey.Parameter> parameters;

		private CompositeConfigurationQuery(CompositeExecutableConfigurationQuery executableQuery) {
			this.executableQuery = executableQuery;
			this.names = new LinkedList<>();
		}

		@Override
		public CompositeExecutableConfigurationQuery get(String... names) throws IllegalArgumentException {
			if (names == null || names.length == 0) {
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
	public static class CompositeExecutableConfigurationQuery implements ExecutableConfigurationQuery<CompositeConfigurationSource.CompositeConfigurationQuery, CompositeExecutableConfigurationQuery> {

		private final CompositeConfigurationSource source;

		private final LinkedList<CompositeConfigurationQuery> queries;

		private CompositeExecutableConfigurationQuery(CompositeConfigurationSource source) {
			this.source = source;
			this.queries = new LinkedList<>();
		}

		@Override
		public CompositeExecutableConfigurationQuery withParameters(List<ConfigurationKey.Parameter> parameters) throws IllegalArgumentException {
			CompositeConfigurationQuery currentQuery = this.queries.peekLast();
			currentQuery.parameters = new LinkedList<>(this.source.defaultParameters);
			if (parameters != null && !parameters.isEmpty()) {
				Set<String> parameterKeys = new HashSet<>();
				List<String> duplicateParameters = new LinkedList<>();
				for (ConfigurationKey.Parameter parameter : parameters) {
					if(parameter.isWildcard() || parameter.isUndefined()) {
						throw new IllegalArgumentException("Query parameter can not be undefined or a wildcard: " + parameter);
					}
					currentQuery.parameters.add(parameter);
					if (!parameterKeys.add(parameter.getKey())) {
						duplicateParameters.add(parameter.getKey());
					}
				}
				if (!duplicateParameters.isEmpty()) {
					throw new IllegalArgumentException("The following parameters were specified more than once: " + String.join(", ", duplicateParameters));
				}
			}
			return this;
		}

		@Override
		public CompositeConfigurationQuery and() {
			CompositeConfigurationQuery nextQuery = new CompositeConfigurationQuery(this);
			nextQuery.parameters = this.source.defaultParameters;
			this.queries.add(nextQuery);
			return nextQuery;
		}

		@Override
		public Flux<ConfigurationQueryResult> execute() {
			return Flux.create(sink -> {
				final LinkedList<CompositeConfigurationQueryResult> results = this.queries.stream()
					.flatMap(query -> query.names.stream().map(name -> new CompositeConfigurationQueryResult(new GenericConfigurationKey(name, query.parameters), this.source.strategy)))
					.collect(Collectors.toCollection(LinkedList::new));

				final CompositeConfigurationStrategy.CompositeDefaultingStrategy defaultingStrategy = this.source.strategy.createDefaultingStrategy();

				Flux.fromIterable(this.source.sources)
						.map(currentSource -> {
							if (currentSource instanceof DefaultableConfigurationSource) {
								return ((DefaultableConfigurationSource) currentSource).withDefaultingStrategy(defaultingStrategy);
							}
							return currentSource;
						})
						.flatMapSequential(currentSource -> {
							List<CompositeConfigurationQueryResult> unresolvedResults = new LinkedList<>();
							ExecutableConfigurationQuery<?, ?> sourceQuery = null;
							for (CompositeConfigurationQueryResult result : results) {
								if (result.isResolved()) {
									continue;
								}
								unresolvedResults.add(result);

								if (sourceQuery == null) {
									sourceQuery = currentSource.get(result.getQueryKey().getName());
								} else {
									sourceQuery = sourceQuery.and().get(result.getQueryKey().getName());
								}
								sourceQuery.withParameters(result.getQueryKey().getParameters().toArray(ConfigurationKey.Parameter[]::new));
							}

							if (sourceQuery == null) {
								return Mono.empty();
							}

							Iterator<CompositeConfigurationQueryResult> iterator = unresolvedResults.iterator();
							return sourceQuery.execute()
									.doOnNext(result -> iterator.next().updateResult(result))
									.doOnComplete(() -> {
										while (!results.isEmpty() && results.peek().isResolved()) {
											sink.next(results.poll());
										}
									});
						})
						.subscribe(
								ign -> {
								},
								sink::error,
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
	 * @since 1.5
	 *
	 * @see ConfigurationQueryResult
	 */
	public static class CompositeConfigurationQueryResult extends GenericConfigurationQueryResult {

		private final CompositeConfigurationStrategy strategy;

		private ConfigurationQueryResult initial;

		private boolean resolved;

		private CompositeConfigurationQueryResult(ConfigurationKey queryKey, CompositeConfigurationStrategy strategy) {
			super(queryKey, null);
			this.strategy = strategy;
		}

		/**
		 * <p>
		 * Determines whether this result is resolved.
		 * </p>
		 * 
		 * @return true if the result is resolved, false otherwise
		 */
		private boolean isResolved() {
			return this.resolved;
		}

		/**
		 * <p>
		 * Returns the original result returned by an underlying source.
		 * </p>
		 * 
		 * @return the original query result
		 */
		public ConfigurationQueryResult unwrap() {
			return this.initial;
		}

		/**
		 * <p>
		 * Updates the result with the specified result returned by an underlying source.
		 * </p>
		 * 
		 * <p>
		 * The method basically retains the result if it supersedes previous result returned in a previous round.
		 * </p>
		 * 
		 * @param result a query result
		 */
		private void updateResult(ConfigurationQueryResult result) {
			if (!this.resolved) {
				try {
					ConfigurationProperty previousProperty = this.initial != null && this.initial.isPresent() ? this.initial.get() : null;
					if (previousProperty != null) {
						// does the new property supersede the old one?
						if(result.isPresent()) {
							ConfigurationProperty property = result.get();
							if (this.strategy.isSuperseded(this.queryKey, previousProperty.getKey(), property.getKey())) {
								this.initial = result;
								this.queryResult = !property.isUnset() ? property : null;
								this.resolved = this.strategy.isResolved(this.queryKey, property.getKey());
							}
						}
					}
					else {
						this.initial = result;
						if(result.isPresent()) {
							ConfigurationProperty property = result.get();
							this.queryResult = !property.isUnset() ? property : null;
							this.resolved = this.strategy.isResolved(this.queryKey, property.getKey());
						}
					}
				}
				catch(ConfigurationSourceException e) {
					if(!this.strategy.ignoreFailure(e)) {
						// We have two choices:
						// - set the failed query result and mark the result as resolved to make sure the error is propagated to the caller as results might not be accurate
						// - report the failure and resume the defaulting mechanism ignoring the failing source
						// The issue is that we don't know whether the error is related to an invalid value or an unreachable configuration source, in the latter case, we can't assume
						// We should delegate this to the strategy so it can decide what to do
						// if the strategy does not ignore failure then if an error occurs the result is set and the result resolved, otherwise the failed result is ignored and the process continue
						this.initial = result;
						this.resolved = true;
					}
				}
			}
		}
	}

	/**
	 * <p>
	 * The list configuration query used by the composite configuration source.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 *
	 * @see ListConfigurationQuery
	 */
	public static class CompositeListConfigurationQuery implements ListConfigurationQuery<CompositeListConfigurationQuery> {

		private final CompositeConfigurationSource source;

		private final String name;

		private List<ConfigurationKey.Parameter> parameters;

		private CompositeListConfigurationQuery(CompositeConfigurationSource source, String name) {
			this.source = source;
			this.name = name;
			this.parameters = this.source.defaultParameters;
		}

		@Override
		public CompositeListConfigurationQuery withParameters(List<ConfigurationKey.Parameter> parameters) throws IllegalArgumentException {
			this.parameters = new LinkedList<>(this.source.defaultParameters);
			if (parameters != null && !parameters.isEmpty()) {
				Set<String> parameterKeys = new HashSet<>();
				List<String> duplicateParameters = new LinkedList<>();
				for (ConfigurationKey.Parameter parameter : parameters) {
					this.parameters.add(parameter);
					if (!parameterKeys.add(parameter.getKey())) {
						duplicateParameters.add(parameter.getKey());
					}
				}
				if (!duplicateParameters.isEmpty()) {
					throw new IllegalArgumentException("The following parameters were specified more than once: " + String.join(", ", duplicateParameters));
				}
			}
			return this;
		}

		@Override
		public Flux<ConfigurationProperty> execute() {
			return Flux.defer(() -> {
				final CompositeConfigurationStrategy.CompositeDefaultingStrategy defaultingStrategy = this.source.strategy.createDefaultingStrategy();

				return Flux.fromIterable(this.source.sources)
						.map(currentSource -> {
							if (currentSource instanceof DefaultableConfigurationSource) {
								return ((DefaultableConfigurationSource) currentSource).withDefaultingStrategy(defaultingStrategy);
							}
							return currentSource;
						})
						.flatMapSequential(currentSource -> currentSource.list(this.name).withParameters(this.parameters.toArray(ConfigurationKey.Parameter[]::new)).execute())
						.reduceWith(
								ConfigurationPropertyNode::new,
								(node, property) -> {
									node.insert(property);
									return node;
								}
						)
						.flatMapIterable(ConfigurationPropertyNode::getProperties);
			});
		}

		@Override
		public Flux<ConfigurationProperty> executeAll() {
			return Flux.defer(() -> {
				final CompositeConfigurationStrategy.CompositeDefaultingStrategy defaultingStrategy = this.source.strategy.createDefaultingStrategy();

				return Flux.fromIterable(this.source.sources)
						.map(currentSource -> {
							if (currentSource instanceof DefaultableConfigurationSource) {
								return ((DefaultableConfigurationSource) currentSource).withDefaultingStrategy(defaultingStrategy);
							}
							return currentSource;
						})
						.flatMapSequential(currentSource -> currentSource.list(this.name).withParameters(this.parameters.toArray(ConfigurationKey.Parameter[]::new)).executeAll())
						.reduceWith(
								ConfigurationPropertyNode::new,
								(node, property) -> {
									node.insert(property);
									return node;
								}
						)
						.flatMapIterable(ConfigurationPropertyNode::getProperties);
			});
		}
	}

	/**
	 * <p>
	 * A configuration property node used to build a graph of configuration properties.
	 * </p>
	 * 
	 * <p>
	 * The resulting graph allows to combine the lists of properties returned by list queries executed on the underlying sources. The graph should be populated by properties from higher to lower
	 * priority sources in order to retain properties with highest priority.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	private static class ConfigurationPropertyNode {

		private final ConfigurationKey.Parameter parameter;

		private ConfigurationProperty property;

		private Map<ConfigurationKey.Parameter, ConfigurationPropertyNode> children;

		/**
		 * <p>
		 * Creates the root node.
		 * </p>
		 */
		public ConfigurationPropertyNode() {
			this.parameter = null;
		}

		/**
		 * <p>
		 * Creates a node with the specified parameter.
		 * </p>
		 * 
		 * @param parameter a parameter
		 */
		public ConfigurationPropertyNode(ConfigurationKey.Parameter parameter) {
			this.parameter = parameter;
		}

		/**
		 * <p>
		 * Returns the parameters associated to the node.
		 * </p>
		 * 
		 * @return a parameter
		 */
		public ConfigurationKey.Parameter getParameter() {
			return this.parameter;
		}

		/**
		 * <p>
		 * Inserts the specified property into the graph.
		 * </p>
		 * 
		 * @param property a property to insert
		 * @throws IllegalStateException if the node is not a root node
		 */
		public void insert(ConfigurationProperty property) throws IllegalStateException {
			if(this.parameter != null) {
				throw new IllegalStateException("Not a root node");
			}
			this.insert(property, new LinkedList<>(property.getKey().getParameters()));
		}

		/**
		 * <p>
		 * Walks through the graph and inserts the specified property.
		 * </p>
		 * 
		 * @param property the property to insert
		 * @param currentPropertyParameters the current list of parameters defining the level graph the level
		 */
		private void insert(ConfigurationProperty property, Deque<ConfigurationKey.Parameter> currentPropertyParameters) {
			if (currentPropertyParameters.isEmpty()) {
				// We only set it once by source priorities
				if (this.property == null) {
					this.property = property;
				}
			} else {
				if (this.children == null) {
					this.children = new HashMap<>();
				}
				ConfigurationKey.Parameter currentPropertyParameter = currentPropertyParameters.poll();
				ConfigurationPropertyNode childNode = this.children.computeIfAbsent(currentPropertyParameter, ConfigurationPropertyNode::new);
				childNode.insert(property, currentPropertyParameters);
			}
		}

		/**
		 * <p>
		 * Returns the list of properties defined in the graph.
		 * </p>
		 * 
		 * @return a list of properties
		 */
		public List<ConfigurationProperty> getProperties() {
			List<ConfigurationProperty> result = new LinkedList<>();
			this.walk(result);
			return result;
		}

		/**
		 * <p>
		 * Walks through the graph and populate the specified list of properties.
		 * </p>
		 * 
		 * @param properties a list of properties to populate
		 */
		private void walk(List<ConfigurationProperty> properties) {
			if (this.property != null) {
				properties.add(this.property);
			}
			if (this.children != null) {
				this.children.values().forEach(childNode -> childNode.walk(properties));
			}
		}
	}
}
