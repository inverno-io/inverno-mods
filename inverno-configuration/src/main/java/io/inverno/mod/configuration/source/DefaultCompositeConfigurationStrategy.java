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

import java.util.ArrayList;
import java.util.List;

import io.inverno.mod.configuration.ConfigurationKey;
import io.inverno.mod.configuration.ConfigurationKey.Parameter;
import io.inverno.mod.configuration.ConfigurationProperty;
import io.inverno.mod.configuration.ConfigurationQuery;
import io.inverno.mod.configuration.ConfigurationSourceException;
import io.inverno.mod.configuration.ExecutableConfigurationQuery;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Default {@link CompositeConfigurationStrategy} implementation.
 * </p>
 *
 * <p>
 * This strategy prioritizes sources in the order in which they have been set in the composite configuration source from the highest priority to the lowest.
 * </p>
 *
 * <p>
 * It determines the best matching result for a given original query by prioritizing query parameters from left to right: the best matching property is the one matching the most continuous parameters
 * from right to left. If we consider query key {@code property[p1=v1,...pn=vn]}, it supersedes key {@code property[p2=v2,...pn=vn]} which supersedes key {@code property[p3=v3,...pn=vn]}... which
 * supersedes key {@code property[]}.
 * </p>
 *
 * <p>
 * As a result, an original query with {@code n} parameters results in {@code n+1} queries being populated in the source query when no previous result exists from previous sources and {@code n-p}
 * queries when there was a previous result with {@code p} parameters. A query is then resolved when a result exactly matching the original query is found.
 * </p>
 *
 * <p>
 * The order into which parameters are defined in the original query is then significant: {@code property[p1=v1,p2=v2] != property[p2=v2,p1=v1]}.
 * </p>
 * 
 * <p>
 * When listing properties, this implementation will combine results by retaining properties defined in the source with the highest priority in case of conflict. It will also return properties that
 * are defined with less parameters than the original requests to remain consistent with the reste of the implementation which prioritizes parameters from left to right.
 * </p>
 * * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see CompositeConfigurationSource
 * @see CompositeConfigurationStrategy
 */
public class DefaultCompositeConfigurationStrategy implements CompositeConfigurationStrategy {

	private boolean ignoreFailure = true;
	
	/**
	 * <p>
	 * Enables/disables ignore failure globally.
	 * </p>
	 * 
	 * @param ignoreFailure true to ignore all failure, false otherwise
	 */
	public void setIgnoreFailure(boolean ignoreFailure) {
		this.ignoreFailure = ignoreFailure;
	}

	/**
	 * <p>
	 * Ignores all failure if the strategy is configured to ignore failures globally.
	 * </p>
	 *
	 * @see DefaultCompositeConfigurationStrategy#setIgnoreFailure(boolean)
	 */
	@Override
	public boolean ignoreFailure(ConfigurationSourceException error) {
		return this.ignoreFailure;
	}
	
	@Override
	public boolean isSuperseded(ConfigurationKey queryKey, ConfigurationKey previousKey, ConfigurationKey resultKey) {
		if(resultKey == null) {
			return false;
		}
		if(previousKey == null) {
			return true;
		}

		// non-parameterized sources should always return results corresponding to the query therefore any result they returned should supersede the previous one and eventually get resolved
		return resultKey.getParameters().size() > previousKey.getParameters().size();
	}

	@Override
	public boolean isResolved(ConfigurationKey queryKey, ConfigurationKey resultKey) {
		if(resultKey == null) {
			return false;
		}
		
		// non-parameterized sources should always return results corresponding to the query therefore any result they returned is a resolved result
		return queryKey.getParameters().size() == resultKey.getParameters().size();
	}

	@Override
	public ExecutableConfigurationQuery<?,?> populateSourceQuery(ConfigurationKey queryKey, ConfigurationQuery<?,?> sourceQuery, ConfigurationKey previousKey) {
		ExecutableConfigurationQuery<?,?> resultQuery = null;
		
		// a b c d
		//   b c d
		//     c d
		//       d
		//        

		// This is safe to use with non-parameterized sources as they return the same result regardless of the queried parameters and the first return result is always resolved as it exactly corresponds to the query
		// Here we will create n query whereas for such sources we only need one query for the property name (and no parameters)
		// - we can make things smart in the source implementation
		// - we can be smart here but then we must have a way to determine the nature of the source which is usually the role of types (that should prevent to call withParameters() for a source that doesn't supports it
		
		int depth = previousKey != null ? queryKey.getParameters().size() - previousKey.getParameters().size() : queryKey.getParameters().size() + 1;
		ConfigurationQuery<?,?> currentSourceQuery = sourceQuery;
		
		List<Parameter> parametersList = new ArrayList<>(queryKey.getParameters());
		for(int i=0;i<depth;i++) {
			resultQuery = currentSourceQuery.get(queryKey.getName()).withParameters(parametersList.subList(i, parametersList.size()).stream().toArray(Parameter[]::new));
			if(i < depth-1) {
				currentSourceQuery = resultQuery.and();
			}
		}
		return resultQuery;
	}

	@Override
	public Flux<ConfigurationProperty> combineList(ConfigurationKey queryKey, Iterable<Flux<ConfigurationProperty>> sources) {
		return Mono.fromSupplier(() -> new ListCombinationNode(null, new LinkedList<>(queryKey.getParameters())))
			.flatMapMany(rootNode -> Flux.mergeSequential(sources)
				.doOnNext(property -> {
					rootNode.insert(property);
				})
				.thenMany(Flux.fromStream(() -> rootNode.getProperties().stream()))
			);
	}

	@Override
	public Flux<ConfigurationProperty> combineListAll(ConfigurationKey queryKey, Iterable<Flux<ConfigurationProperty>> sources) {
		return Mono.fromSupplier(() -> new ListAllCombinationNode(null, new LinkedList<>(queryKey.getParameters())))
			.flatMapMany(rootNode -> Flux.mergeSequential(sources)
				.doOnNext(property -> {
					rootNode.insert(property);
				})
				.thenMany(Flux.fromStream(() -> rootNode.getProperties().stream()))
			);
	}
	
	/**
	 * <p>
	 * Base list combination node used to build the graph of configuration properties.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> the type of node
	 */
	private static abstract class BaseCombinationNode<A extends BaseCombinationNode<A>> {
		
		protected final List<Parameter> queryParameters;
		
		protected final Parameter levelQueryParameter;
		
		protected final Parameter parameter;
		
		protected Map<Parameter, A> childs;

		/**
		 * 
		 * @param parameter
		 * @param queryParameters 
		 */
		public BaseCombinationNode(Parameter parameter, List<Parameter> queryParameters) {
			this.parameter = parameter;
			this.queryParameters = queryParameters;
			this.levelQueryParameter = this.queryParameters.isEmpty() ? null : this.queryParameters.get(0);
		}
		
		/**
		 * <p>
		 * Inserts the specified configuration property into the graph.
		 * </p>
		 * 
		 * @param property the property to insert
		 */
		void insert(ConfigurationProperty property) {
			Map<String, Parameter> propertyParametersByKey = property.getKey().getParameters().stream().collect(Collectors.toMap(p -> p.getKey(), p -> p));
			LinkedList<Parameter> propertyQueryParameters = new LinkedList<>();
			
			// property parameters must match the query: if query is k1,k2,k3, we must have (k1,k2,k3), (k1,k2), (k1) or () but not (k2,k3), (k2), (k3), (k1,k3)
			for(int i=this.queryParameters.size()-1;i>=0;i--) {
				Parameter queryParameter = this.queryParameters.get(i);
				Parameter propertyParameter = propertyParametersByKey.remove(queryParameter.getKey());
				if(propertyParameter != null) {
					propertyQueryParameters.addFirst(propertyParameter);
				}
				else if(!propertyQueryParameters.isEmpty()) {
					// we don't have a continuous series of parameters from right to left corresponding to the query so we must not include result
					return;
				}
			}
			// Add remaining parameters
			propertyQueryParameters.addAll(propertyParametersByKey.values());

			this.insert(property, propertyQueryParameters);
		}
		
		/**
		 * <p>
		 * Returns the combined configuration properties defined in the node.
		 * </p>
		 * 
		 * @return a list of configuration properties
		 */
		List<ConfigurationProperty> getProperties() {
			List<ConfigurationProperty> result = new LinkedList<>();
			this.getProperties(result);
			return result;
		}
		
		/**
		 * <p>
		 * Inserts the specified configuration property into the graph.
		 * </p>
		 * 
		 * @param property the property to insert
		 * @param propertyQueryParameters the current list of property parameters corresponding to the list query
		 */
		abstract void insert(ConfigurationProperty property, Deque<Parameter> propertyQueryParameters);
		
		/**
		 * <p>
		 * Populates the specified list of configuration properties with the combined properties defined in the node.
		 * </p>
		 * 
		 * @param properties a mutable list of properties
		 */
		abstract void getProperties(List<ConfigurationProperty> properties);
	}
	
	/**
	 * <p>
	 * List combination node used to build the graph of configuration properties during a {@link DefaultCompositeConfigurationStrategy#combineList()} operation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	private static class ListCombinationNode extends BaseCombinationNode<ListCombinationNode> {
		
		ConfigurationProperty property;

		/**
		 * 
		 * @param parameter
		 * @param queryParameters 
		 */
		ListCombinationNode(Parameter parameter, List<Parameter> queryParameters) {
			super(parameter, queryParameters);
		}
		
		@Override
		void insert(ConfigurationProperty property, Deque<Parameter> propertyQueryParameters) {
			if(propertyQueryParameters.isEmpty()) {
				// we consider sources from the highest priority to the lowest so we only set it once
				if(this.property == null) {
					this.property = property;
				}
			}
			else {
				// property to insert has parameters
				if(this.levelQueryParameter != null) {
					Parameter currentPropertyParameter = propertyQueryParameters.poll();
					if(this.levelQueryParameter.getKey().equals(currentPropertyParameter.getKey()) && (this.levelQueryParameter.isWildcard() || this.levelQueryParameter.getValue().equals(currentPropertyParameter.getValue()))) {
						if(this.childs == null) {
							this.childs = new HashMap<>();
						}
						ListCombinationNode childNode = this.childs.computeIfAbsent(currentPropertyParameter, k -> new ListCombinationNode(k, this.queryParameters.subList(1, this.queryParameters.size())));
						childNode.insert(property, propertyQueryParameters);
					}
				}
				// we reach the end of the query with property parameters left
				// list() => we filter all as we want exact result => does nothing
			}
		}
		
		@Override
		void getProperties(List<ConfigurationProperty> properties) {
			if(this.property != null) {
				properties.add(this.property);
			}
			if(this.childs != null) {
				this.childs.values().stream().forEach(childNode -> childNode.getProperties(properties));
			}
		}
	}
	
	/**
	 * <p>
	 * List combination node used to build the graph of configuration properties during a {@link DefaultCompositeConfigurationStrategy#combineListAll()} operation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	private static class ListAllCombinationNode extends BaseCombinationNode<ListAllCombinationNode> {
		
		Map<ConfigurationKey, ConfigurationProperty> properties;

		/**
		 * 
		 * @param parameter
		 * @param queryParameters 
		 */
		ListAllCombinationNode(Parameter parameter, List<Parameter> queryParameters) {
			super(parameter, queryParameters);
		}
		
		@Override
		void insert(ConfigurationProperty property, Deque<Parameter> propertyQueryParameters) {
			if(propertyQueryParameters.isEmpty()) {
				// we consider sources from the highest priority to the lowest so we should only include result if it hasn't been already found
				if(this.properties == null) {
					this.properties = new HashMap<>();
				}
				this.properties.putIfAbsent(property.getKey(), property);
			}
			else {
				// property to insert has parameters
				if(this.levelQueryParameter != null) {
					Parameter currentPropertyParameter = propertyQueryParameters.poll();
					if(this.levelQueryParameter.getKey().equals(currentPropertyParameter.getKey()) && (this.levelQueryParameter.isWildcard() || this.levelQueryParameter.getValue().equals(currentPropertyParameter.getValue()))) {
						if(this.childs == null) {
							this.childs = new HashMap<>();
						}
						ListAllCombinationNode childNode = this.childs.computeIfAbsent(currentPropertyParameter, k -> new ListAllCombinationNode(k, this.queryParameters.subList(1, this.queryParameters.size())));
						childNode.insert(property, propertyQueryParameters);
					}
				}
				else {
					// we reach the end of the query with property parameters left
					// listAll() => we include all as we want all results => add to the property set

					if(this.properties == null) {
						this.properties = new HashMap<>();
					}
					this.properties.putIfAbsent(property.getKey(), property);
				}
			}
		}
		
		@Override
		void getProperties(List<ConfigurationProperty> properties) {
			if(this.properties != null) {
				properties.addAll(this.properties.values());
			}
			if(this.childs != null) {
				this.childs.values().stream().forEach(childNode -> childNode.getProperties(properties));
			}
		}
	}
}
