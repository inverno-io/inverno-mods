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
package io.inverno.mod.configuration;

import io.inverno.mod.configuration.ConfigurationKey.Parameter;
import reactor.core.publisher.Flux;

/**
 * <p>
 * An executable configuration update can be executed to set configuration
 * properties for a particular context in a configuration source.
 * </p>
 * 
 * <p>
 * Multiple configuration updates can be chained by calling the
 * {@link ExecutableConfigurationUpdate#and} method to be executed in a batch
 * when the {@link ExecutableConfigurationUpdate#execute} method is invoked on
 * the last query.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ConfigurationUpdate
 * @see ConfigurationUpdateResult
 *
 * @param <A> the update type
 * @param <B> the executable update type
 * @param <C> the update result type
 */
public interface ExecutableConfigurationUpdate<A extends ConfigurationUpdate<A, B, C>, B extends ExecutableConfigurationUpdate<A, B, C>, C extends ConfigurationUpdateResult<?>> {

	/**
	 * <p>
	 * Defines one parameter that specifies the context in which configuration
	 * properties are to be retrieved.
	 * </p>
	 * 
	 * @param k1 the parameter name
	 * @param v1 the parameter value
	 * 
	 * @return the executable configuration update
	 */
	default B withParameters(String k1, Object v1) {
		return this.withParameters(Parameter.of(k1, v1));
	}
	
	/**
	 * <p>
	 * Defines two parameters that specify the context in which configuration
	 * properties are to be retrieved.
	 * </p>
	 * 
	 * @param k1 the first parameter name
	 * @param v1 the first parameter value
	 * @param k2 the second parameter name
	 * @param v2 the second parameter value
	 * 
	 * @return the executable configuration update
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	default B withParameters(String k1, Object v1, String k2, Object v2) throws IllegalArgumentException {
		return this.withParameters(Parameter.of(k1, v1), Parameter.of(k2, v2));
	}
	
	/**
	 * <p>
	 * Defines three parameters that specify the context in which configuration
	 * properties are to be retrieved.
	 * </p>
	 * 
	 * @param k1 the first parameter name
	 * @param v1 the first parameter value
	 * @param k2 the second parameter name
	 * @param v2 the second parameter value
	 * @param k3 the third parameter name
	 * @param v3 the third parameter value
	 * 
	 * @return the executable configuration update
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	default B withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3) throws IllegalArgumentException {
		return this.withParameters(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3));
	}
	
	/**
	 * <p>
	 * Defines four parameters that specify the context in which configuration
	 * properties are to be retrieved.
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
	 * @return the executable configuration update
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	default B withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) throws IllegalArgumentException {
		return this.withParameters(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4));
	}
	
	/**
	 * <p>
	 * Defines five parameters that specify the context in which configuration
	 * properties are to be retrieved.
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
	 * @return the executable configuration update
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	default B withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5) throws IllegalArgumentException {
		return this.withParameters(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5));
	}
	
	/**
	 * <p>
	 * Defines six parameters that specify the context in which configuration
	 * properties are to be retrieved.
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
	 * @return the executable configuration update
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	default B withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6) throws IllegalArgumentException {
		return this.withParameters(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5), Parameter.of(k6, v6));
	}
	
	/**
	 * <p>
	 * Defines seven parameters that specify the context in which configuration
	 * properties are to be retrieved.
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
	 * @return the executable configuration update
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	default B withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7) throws IllegalArgumentException {
		return this.withParameters(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5), Parameter.of(k6, v6), Parameter.of(k7, v7));
	}
	
	/**
	 * <p>
	 * Defines eigth parameters that specify the context in which configuration
	 * properties are to be retrieved.
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
	 * @return the executable configuration update
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	default B withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8) throws IllegalArgumentException {
		return this.withParameters(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5), Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8));
	}
	
	/**
	 * <p>
	 * Defines nine parameters that specify the context in which configuration
	 * properties are to be retrieved.
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
	 * @return the executable configuration update
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	default B withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9) throws IllegalArgumentException {
		return this.withParameters(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5), Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8), Parameter.of(k9, v9));
	}
	
	/**
	 * <p>
	 * Defines ten parameters that specify the context in which configuration
	 * properties are to be retrieved.
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
	 * @return the executable configuration update
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	default B withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9, String k10, Object v10) throws IllegalArgumentException {
		return this.withParameters(Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5), Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8), Parameter.of(k9, v9), Parameter.of(k10, v10));
	}
	
	/**
	 * <p>
	 * Defines parameters that specify the context in which configuration properties
	 * are to be retrieved.
	 * </p>
	 * 
	 * @param parameters an array of parameters
	 * 
	 * @return the executable configuration update
	 * @throws IllegalArgumentException if parameters were specified more than once
	 */
	B withParameters(Parameter... parameters) throws IllegalArgumentException;
	
	/**
	 * <p>
	 * Chains another update.
	 * </p>
	 * 
	 * @return a configuration update
	 */
	A and();
	
	/**
	 * <p>
	 * Executes the chain of configuration updates.
	 * </p>
	 * 
	 * <p>
	 * Results are emitted in the same order as the queries.
	 * </p>
	 * 
	 * @return a stream of configuration update results
	 */
	Flux<C> execute();
}
