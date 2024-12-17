/*
 * Copyright 2021 Jeremy KUHN
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

import io.inverno.mod.configuration.ConfigurationKey.WildcardParameter;
import java.util.Arrays;
import java.util.List;
import reactor.core.publisher.Flux;

/**
 * <p>
 * A list configuration query can be executed to retrieve all configuration properties defined with a specific set of parameters.
 * </p>
 * 
 * <p>
 * This type of query supports {@link WildcardParameter} in order to list properties defined with a specific parameter regardless of the value.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A> the query type
 */
public interface ListConfigurationQuery<A extends ListConfigurationQuery<A>> {

	/**
	 * <p>
	 * Defines one parameter that specifies the context in which configuration properties are to be retrieved.
	 * </p>
	 *
	 * @param k1 the parameter name
	 * @param v1 the parameter value
	 *
	 * @return the configuration query
	 */
	default A withParameters(String k1, Object v1) {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1)));
	}
	
	/**
	 * <p>
	 * Defines two parameters that specify the context in which configuration properties are to be retrieved.
	 * </p>
	 *
	 * @param k1 the first parameter name
	 * @param v1 the first parameter value
	 * @param k2 the second parameter name
	 * @param v2 the second parameter value
	 *
	 * @return the configuration query
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	default A withParameters(String k1, Object v1, String k2, Object v2) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2)));
	}
	
	/**
	 * <p>
	 * Defines three parameters that specify the context in which configuration properties are to be retrieved.
	 * </p>
	 *
	 * @param k1 the first parameter name
	 * @param v1 the first parameter value
	 * @param k2 the second parameter name
	 * @param v2 the second parameter value
	 * @param k3 the third parameter name
	 * @param v3 the third parameter value
	 *
	 * @return the configuration query
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	default A withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2), ConfigurationKey.Parameter.of(k3, v3)));
	}
	
	/**
	 * <p>
	 * Defines four parameters that specify the context in which configuration properties are to be retrieved.
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
	 * @return the configuration query
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	default A withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2), ConfigurationKey.Parameter.of(k3, v3), ConfigurationKey.Parameter.of(k4, v4)));
	}
	
	/**
	 * <p>
	 * Defines five parameters that specify the context in which configuration properties are to be retrieved.
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
	 * @return the configuration query
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	default A withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2), ConfigurationKey.Parameter.of(k3, v3), ConfigurationKey.Parameter.of(k4, v4), ConfigurationKey.Parameter.of(k5, v5)));
	}
	
	/**
	 * <p>
	 * Defines six parameters that specify the context in which configuration properties are to be retrieved.
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
	 * @return the configuration query
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	default A withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2), ConfigurationKey.Parameter.of(k3, v3), ConfigurationKey.Parameter.of(k4, v4), ConfigurationKey.Parameter.of(k5, v5), ConfigurationKey.Parameter.of(k6, v6)));
	}
	
	/**
	 * <p>
	 * Defines seven parameters that specify the context in which configuration properties are to be retrieved.
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
	 * @return the configuration query
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	default A withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2), ConfigurationKey.Parameter.of(k3, v3), ConfigurationKey.Parameter.of(k4, v4), ConfigurationKey.Parameter.of(k5, v5), ConfigurationKey.Parameter.of(k6, v6), ConfigurationKey.Parameter.of(k7, v7)));
	}
	
	/**
	 * <p>
	 * Defines eighth parameters that specify the context in which configuration properties are to be retrieved.
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
	 * @return the configuration query
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	default A withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2), ConfigurationKey.Parameter.of(k3, v3), ConfigurationKey.Parameter.of(k4, v4), ConfigurationKey.Parameter.of(k5, v5), ConfigurationKey.Parameter.of(k6, v6), ConfigurationKey.Parameter.of(k7, v7), ConfigurationKey.Parameter.of(k8, v8)));
	}
	
	/**
	 * <p>
	 * Defines nine parameters that specify the context in which configuration properties are to be retrieved.
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
	 * @return the configuration query
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	default A withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2), ConfigurationKey.Parameter.of(k3, v3), ConfigurationKey.Parameter.of(k4, v4), ConfigurationKey.Parameter.of(k5, v5), ConfigurationKey.Parameter.of(k6, v6), ConfigurationKey.Parameter.of(k7, v7), ConfigurationKey.Parameter.of(k8, v8), ConfigurationKey.Parameter.of(k9, v9)));
	}
	
	/**
	 * <p>
	 * Defines ten parameters that specify the context in which configuration properties are to be retrieved.
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
	 * @return the configuration query
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	default A withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9, String k10, Object v10) throws IllegalArgumentException {
		return this.withParameters(List.of(ConfigurationKey.Parameter.of(k1, v1), ConfigurationKey.Parameter.of(k2, v2), ConfigurationKey.Parameter.of(k3, v3), ConfigurationKey.Parameter.of(k4, v4), ConfigurationKey.Parameter.of(k5, v5), ConfigurationKey.Parameter.of(k6, v6), ConfigurationKey.Parameter.of(k7, v7), ConfigurationKey.Parameter.of(k8, v8), ConfigurationKey.Parameter.of(k9, v9), ConfigurationKey.Parameter.of(k10, v10)));
	}
	
	/**
	 * <p>
	 * Defines parameters that specify the context in which configuration properties are to be retrieved.
	 * </p>
	 *
	 * @param parameters an array of parameters
	 *
	 * @return the configuration query
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	default A withParameters(ConfigurationKey.Parameter... parameters) throws IllegalArgumentException {
		return this.withParameters(parameters != null ? Arrays.asList(parameters) : List.of());
	}
	
	/**
	 * <p>
	 * Defines parameters that specify the context in which configuration properties are to be retrieved.
	 * </p>
	 *
	 * @param parameters a list of parameters
	 *
	 * @return the configuration query
	 *
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	A withParameters(List<ConfigurationKey.Parameter> parameters) throws IllegalArgumentException;
	
	/**
	 * <p>
	 * Lists all properties with the specified property name and whose parameters exactly match the query.
	 * </p>
	 *
	 * <p>
	 * This method returns properties whose parameters exactly match the parameters specified in the query and exclude those defined with extra parameters.
	 * </p>
	 *
	 * @return a stream of configuration properties
	 */
	Flux<ConfigurationProperty> execute();
	
	/**
	 * <p>
	 * Lists all properties with the specified property name and whose parameters match the query.
	 * </p>
	 * 
	 * <p>
	 * This method returns all properties whose parameters match the parameters specified in the query without excluding those defined with extra parameters.
	 * </p>
	 * 
	 * @return a stream of configuration properties
	 */
	Flux<ConfigurationProperty> executeAll();
}
