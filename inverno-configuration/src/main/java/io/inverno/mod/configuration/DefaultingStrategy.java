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
package io.inverno.mod.configuration;

import io.inverno.mod.configuration.internal.LookupDefaultingStrategy;
import io.inverno.mod.configuration.internal.NoOpDefaultingStrategy;
import io.inverno.mod.configuration.internal.WildcardDefaultingStrategy;
import java.util.List;

/**
 * <p>
 * A defaulting strategy is used in a {@link DefaultableConfigurationSource} to implement defaulting configuration mechanism.
 * </p>
 * 
 * <p>
 * A defaulting strategy derives a list of queries from an original query whose order specifies result priorities. The source shall retain the first non-empty result from that list or the empty result
 * if no result is returned for any of the queries returned by the strategy.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface DefaultingStrategy {
	
	/**
	 * <p>
	 * Returns a NoOp defaulting strategy.
	 * </p>
	 * 
	 * <p>
	 * The NoOp defaulting strategy does not support defaulting and simply returns the original query.
	 * </p>
	 * 
	 * @return a NoOp defaulting strategy
	 */
	static DefaultingStrategy noOp() {
		return NoOpDefaultingStrategy.INSTANCE;
	}
	
	/**
	 * <p>
	 * Returns a lookup defaulting strategy.
	 * </p>
	 * 
	 * <p>
	 * A lookup defaulting strategy support defaulting by prioritizing parameters from left to right: the best matching property is the one matching the most continuous parameters from left to right.
	 * </p>
	 * 
	 * <p>
	 * If we consider query key {@code property[p1=v1,...pn=vn]}, it supersedes key {@code property[p1=v1,...pn-1=vn-1]} which supersedes key {@code property[p1=v1,...pn-2=vn-2]}... which supersedes key
	 * {@code property[]}. It basically tells the source to lookup by successively removing the rightmost parameter if no exact result exists for a particular query.
	 * </p>
	 * 
	 * <p>
	 * As a result, an original query with {@code n} parameters results in {@code n+1} potential queries to execute for the source. The query result is then the first non-empty result or the empty result.
	 * </p>
	 *
	 * <p>
	 * The order into which parameters are defined in the original query is then significant: {@code property[p1=v1,p2=v2] != property[p2=v2,p1=v1]}.
	 * </p>
	 * 
	 * <p>
	 * When listing properties, this strategy includes the <i>parent</i> properties which are obtained by successively removing the rightmost parameter from the original query.
	 * </p>
	 * 
	 * @return a lookup defaulting strategy
	 */
	static DefaultingStrategy lookup() {
		return LookupDefaultingStrategy.INSTANCE;
	}
	
	/**
	 * <p>
	 * Returns a wildcard defaulting strategy.
	 * </p>
	 *
	 * <p>
	 * If we consider query key {@code property[p1=v1,...pn=vn]}, the most precise result is the one defining parameters {@code [p1=v1,...pn=vn]}, it supersedes results that define {@code n-1} query
	 * parameters, which supersedes results that define {@code n-2} query parameters... which supersedes results that define no query parameters. Conflicts may arise when a source defines a property
	 * with different set of query parameters with the same cardinality (e.g. when it defines properties {@code property[p1=v1,p2=v2]} and {@code property[p1=v1,p3=v3]}). In such situation, priority
	 * is always given to parameters from left to right (therefore {@code property[p1=v1,p2=v2]} supersedes {@code property[p1=v1,p3=v3]})
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
	 * @return a wildcard defaulting strategy
	 */
	static DefaultingStrategy wildcard() {
		return WildcardDefaultingStrategy.INSTANCE;
	}
	
	/**
	 * <p>
	 * Derives a list of queries from the specified original query.
	 * </p>
	 * 
	 * <p>
	 * The order of the returned queries must determine the result priorities. The source must consider queries in that order to determine the best matching result for the original query.
	 * </p>
	 * 
	 * @param queryKey the original query
	 * 
	 * @return a list of configuration queries
	 */
	List<ConfigurationKey> getDefaultingKeys(ConfigurationKey queryKey);
	
	/**
	 * <p>
	 * Derives a list of keys that defines the set of properties that must be retained in a list query.
	 * </p>
	 * 
	 * <p>
	 * A source shall use these keys to determine whether a given property must be included in the results of a list query.
	 * </p>
	 * 
	 * <p>
	 * Note that in case of a {@link ListConfigurationQuery#executeAll()} operation, the source shall only include properties with extra parameters that only match the first key so as to exclude
	 * properties inconsistent with the original query.
	 * </p>
	 * 
	 * @param queryKey the original query
	 * 
	 * @return a list of configuration keys
	 */
	List<ConfigurationKey> getListDefaultingKeys(ConfigurationKey queryKey);
}
