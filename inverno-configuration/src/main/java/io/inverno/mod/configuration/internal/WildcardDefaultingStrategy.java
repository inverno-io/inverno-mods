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
package io.inverno.mod.configuration.internal;

import io.inverno.mod.configuration.ConfigurationKey;
import io.inverno.mod.configuration.DefaultingStrategy;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>
 * A {@link DefaultingStrategy} that allows to return the most precise result which is the one that defines the most query parameters.
 * </p>
 * 
 * <p>
 * If we consider query key {@code property[p1=v1,...pn=vn]}, the most precise result is the one defining parameters {@code [p1=v1,...pn=vn]}, it supersedes results that define {@code n-1} query
 * parameters, which supersedes results that define {@code n-2} query parameters... which supersedes results that define no query parameter. Conflicts may arise when a source defines a property with
 * different set of query parameters with the same cardinality (e.g. when it defines properties {@code property[p1=v1,p2=v2]} and {@code property[p1=v1,p3=v3]}). In such situation, priority is always
 * given to parameters from left to right (therefore {@code property[p1=v1,p2=v2]} supersedes {@code property[p1=v1,p3=v3]})
 * </p>
 * 
 * <p>
 * An original query with {@code n} parameters then results in {@code 2^n} potential queries to execute for the source (the sum of the k-combinations from 0 to n). The query result is the first
 * non-empty result or the empty result.
 * </p>
 * 
 * <p>
 * The order into which parameters are defined in the original query is then significant: {@code property[p1=v1,p2=v2] != property[p2=v2,p1=v1]}.
 * </p>
 * 
 * <p>
 * When listing properties, this strategy includes all properties that could match the query parameters.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class WildcardDefaultingStrategy implements DefaultingStrategy {

	/**
	 * The wildcard defaulting strategy singleton.
	 */
	public static final WildcardDefaultingStrategy INSTANCE = new WildcardDefaultingStrategy();
	
	@Override
	public List<ConfigurationKey> getDefaultingKeys(ConfigurationKey queryKey) {
		List<ConfigurationKey.Parameter> queryParameters = new LinkedList<>(queryKey.getParameters());
		
		List<LinkedList<List<ConfigurationKey.Parameter>>> queryParametersByCard = new ArrayList<>();
		queryParametersByCard.add(new LinkedList<>());
		queryParametersByCard.get(0).add(List.of());
		for(int i = queryParameters.size() - 1;i >= 0;i--) {
			queryParametersByCard.add(new LinkedList<>());
			
			for(int j = queryParametersByCard.size() - 2;j >= 0;j--) {
				LinkedList<List<ConfigurationKey.Parameter>> currentCardQueries = queryParametersByCard.get(j);
				int currentCardQueriesLength = currentCardQueries.size();
				for(int k = 0; k < currentCardQueriesLength;k++) {
					List<ConfigurationKey.Parameter> query = new LinkedList<>();
					query.add(queryParameters.get(i));
					query.addAll(currentCardQueries.get(k));
					
					queryParametersByCard.get(query.size()).add(query);
				}
			}
		}
		
		return queryParametersByCard.stream().reduce(
			new LinkedList<>(), 
			(result, element) -> {
				element.forEach(parameters -> {
					result.addFirst(ConfigurationKey.of(queryKey.getName(), parameters.toArray(ConfigurationKey.Parameter[]::new)));
				});
				return result;
			},
			(r1, r2) -> {
				// since we are not parallel this must never happen
				throw new IllegalStateException();
			});
	}

	
	@Override
	public List<ConfigurationKey> getListDefaultingKeys(ConfigurationKey queryKey) {
		List<ConfigurationKey.Parameter> queryParameters = new LinkedList<>(queryKey.getParameters());
		int keyLength = queryParameters.size();
		
		List<LinkedList<List<Integer>>> queryParameterIndicesByCard = new ArrayList<>();
		queryParameterIndicesByCard.add(new LinkedList<>());
		queryParameterIndicesByCard.get(0).add(List.of());
		for(int i = keyLength - 1;i >= 0;i--) {
			queryParameterIndicesByCard.add(new LinkedList<>());
			
			for(int j = queryParameterIndicesByCard.size() - 2;j >= 0;j--) {
				LinkedList<List<Integer>> currentCardQueries = queryParameterIndicesByCard.get(j);
				int currentCardQueriesLength = currentCardQueries.size();
				for(int k = 0; k < currentCardQueriesLength;k++) {
					List<Integer> query = new LinkedList<>();
					query.add(i);
					query.addAll(currentCardQueries.get(k));
					
					queryParameterIndicesByCard.get(query.size()).add(query);
				}
			}
		}
		
		return queryParameterIndicesByCard.stream().reduce(
			new LinkedList<>(),
			(result, element) -> {
				element.forEach(parameterIndices -> {
					ConfigurationKey.Parameter[] parameters = new ConfigurationKey.Parameter[keyLength];
					boolean[] keptIndices = new boolean[parameters.length];
					for(int index : parameterIndices) {
						keptIndices[index] = true;
					}
					for(int i = 0;i < keyLength;i++) {
						ConfigurationKey.Parameter parameter = queryParameters.get(i);
						parameters[i] = keptIndices[i] ? parameter : ConfigurationKey.Parameter.undefined(parameter.getKey());
					}
					result.addFirst(ConfigurationKey.of(queryKey.getName(), parameters));
				});
				return result;
			},
			(r1, r2) -> {
				// since we are not parallel this must never happen
				throw new IllegalStateException();
			});
	}
}
