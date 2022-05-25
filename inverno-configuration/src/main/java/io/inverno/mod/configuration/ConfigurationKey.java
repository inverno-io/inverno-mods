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

import io.inverno.mod.configuration.internal.GenericConfigurationKey;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;

/**
 * <p>
 * A configuration key uniquely identifies a configuration property in a configuration source. It is composed of a name and a collection of parameters.
 * </p>
 *
 * <p>
 * The use of parameters in the key makes it possible to define different values for the same configuration property name and retrieve them within a particular context. For instance, it is possible to
 * define a value in a test environment and different value for the same property in a production environment.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see ExecutableConfigurationQuery
 * @see ConfigurationQueryResult
 */
public interface ConfigurationKey {

	/**
	 * <p>
	 * A parameter is used to specify the context in which a property value is defined.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	public static class Parameter {
		
		private final String key;
		
		private final Object value;
		
		/**
		 * <p>
		 * Creates a configuration property parameter with a null value corresponding to a wildcard parameter. 
		 * </p>
		 * 
		 * @param key the parameter key
		 * 
		 * @throws IllegalArgumentException if the key is null or empty
		 */
		private Parameter(String key) throws IllegalArgumentException {
			if(key == null || key.equals("")) {
				throw new IllegalArgumentException("Parameter key can't be null or empty");
			}
			this.key = key;
			this.value = null;
		}
		
		/**
		 * <p>
		 * Creates a basic configuration property parameter.
		 * </p>
		 * 
		 * @param key   the parameter key
		 * @param value the parameter value
		 * 
		 * @throws IllegalArgumentException if the key is null or empty or if the value is null or not a string, nor a primitive
		 */
		private Parameter(String key, Object value) throws IllegalArgumentException {
			if(key == null || key.equals("")) {
				throw new IllegalArgumentException("Parameter key can't be null or empty");
			}
			this.key = key;
			if(value == null) {
				throw new IllegalArgumentException("Parameter value can't be null");
			}
			if(!(value instanceof Number || value instanceof Boolean || value instanceof Character || value instanceof CharSequence)) {
				throw new IllegalArgumentException("Parameter value can only be a number, a boolean, a character or a string");
			}
			this.value = value;
		}
		
		/**
		 * <p>
		 * Returns the parameter name.
		 * </p>
		 * 
		 * @return the name of the parameter
		 */
		public String getKey() {
			return key;
		}

		/**
		 * <p>
		 * Returns the parameter value.
		 * </p>
		 * 
		 * @return the value of the parameter
		 */
		public Object getValue() {
			return value;
		}
		
		/**
		 * <p>
		 * Determines whether the parameter is a wildcard parameter.
		 * </p>
		 * 
		 * @return true if the parameter is a wildcard parameter, false otherwise
		 */
		public boolean isWildcard() {
			return false;
		}
		
		/**
		 * <p>
		 * Determines whether the parameter is an undefined parameter.
		 * </p>
		 * 
		 * @return true if the parameter is an undefined parameter, false otherwise
		 */
		public boolean isUndefined() {
			return false;
		}
		
		/**
		 * <p>
		 * Creates a parameter with the specified key and value.
		 * </p>
		 *
		 * @param key   the parameter key
		 * @param value the parameter value
		 *
		 * @return a parameter
		 *
		 * @throws IllegalArgumentException if the key is null or empty or if the value is null or not a string, nor a primitive
		 */
		public static Parameter of(String key, Object value) throws IllegalArgumentException {
			return new Parameter(key, value);
		}
		
		/**
		 * <p>
		 * Creates a wildcard parameter with the specified key.
		 * </p>
		 * 
		 * @param key the parameter key
		 * 
		 * @return a wildcard parameter
		 * 
		 * @throws IllegalArgumentException if the key is null or empty
		 */
		public static Parameter wildcard(String key) throws IllegalArgumentException {
			return new WildcardParameter(key);
		}
		
		/**
		 * <p>
		 * Creates an undefined parameter with the specified key.
		 * </p>
		 * 
		 * @param key the parameter key
		 * 
		 * @return an undefined parameter
		 * 
		 * @throws IllegalArgumentException if the key is null or empty
		 */
		public static Parameter undefined(String key) throws IllegalArgumentException {
			return new UndefinedParameter(key);
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((key == null) ? 0 : key.hashCode());
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Parameter other = (Parameter) obj;
			if (key == null) {
				if (other.key != null)
					return false;
			} else if (!key.equals(other.key))
				return false;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder str = new StringBuilder();
			str.append(this.key)
				.append("=");
			if(this.value instanceof Number || this.value instanceof Boolean) {
				str.append(this.value);
			}
			else {
				str.append("\"").append(StringEscapeUtils.escapeJava(this.value.toString())).append("\"");
			}
			return str.toString();
		}
	}
	
	/**
	 * <p>
	 * A wildcard parameter is used to match all values for a parameter in a list configuration query.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @see ListConfigurationQuery
	 */
	public static class WildcardParameter extends Parameter {
		
		/**
		 * <p>
		 * Creates a wildcard configuration property parameter.
		 * </p>
		 * 
		 * @param key the parameter key
		 */
		private WildcardParameter(String key) {
			super(key);
		}

		@Override
		public boolean isWildcard() {
			return true;
		}
	}
	
	/**
	 * <p>
	 * An undefined parameter is used to match properties that do not defined a particular parameter in a list configuration query.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 * 
	 * @see ListConfigurationQuery
	 */
	public static class UndefinedParameter extends Parameter {
		
		/**
		 * <p>
		 * Creates an undefined configuration property parameter.
		 * </p>
		 * 
		 * @param key the parameter key
		 */
		private UndefinedParameter(String key) {
			super(key);
		}

		@Override
		public boolean isUndefined() {
			return true;
		}
	}
	

	/**
	 * <p>
	 * Returns the name of the configuration property.
	 * </p>
	 * 
	 * @return a name
	 */
	String getName();
	
	/**
	 * <p>
	 * Returns the list of parameters specifying the context in which a property value is defined.
	 * </p>
	 *
	 * @return a collection of parameters or an empty collection if the key does not define any parameter
	 */
	Collection<Parameter> getParameters();
	
	/**
	 * <p>
	 * Returns the parameter with the specified key.
	 * </p>
	 * 
	 * @param key the parameter key
	 * 
	 * @return an optional returning the parameter or an empty optional if there's no parameter with the specified key
	 */
	Optional<Parameter> getParameter(String key);
	
	/**
	 * <p>
	 * Determines whether this configuration key macthes the other key.
	 * </p>
	 * 
	 * <p>
	 * When the match is exact, this method returns true only if the this key defines the exact same parameters as the other key with matching values.
	 * </p>
	 * 
	 * <p>
	 * When the match is not exact, it returns true when the other key defines the same parameters as the other key with matching values, it can define other parameters.
	 * </p>
	 * 
	 * @param other the other key to match
	 * @param exact true for exact matching, false otherwise
	 * 
	 * @return true if this key matches the other key, false otherwise
	 */
	boolean matches(ConfigurationKey other, boolean exact);
	
	/**
	 * <p>
	 * Creates a configuration key with the specified name and list of parameters specifying the context in which a property value is defined.
	 * </p>
	 * 
	 * @param name       the configuration property name
	 * @param parameters the configuration property parameters
	 * 
	 * @return a configuration key
	 */
	public static ConfigurationKey of(String name, Parameter... parameters) {
		if(parameters != null) {
			Set<String> parameterKeys = new HashSet<>();
			List<Parameter> parametersList = new LinkedList<>();
			List<String> duplicateParameters = new LinkedList<>();
			for(Parameter parameter : parameters) {
				parametersList.add(parameter);
				if(!parameterKeys.add(parameter.getKey())) {
					duplicateParameters.add(parameter.getKey());
				}
			}
			if(!duplicateParameters.isEmpty()) {
				throw new IllegalArgumentException("The following parameters were specified more than once: " + duplicateParameters.stream().collect(Collectors.joining(", ")));
			}
			return new GenericConfigurationKey(name, parametersList);
		}
		else {
			return new GenericConfigurationKey(name);
		}
	}
	
	/**
	 * <p>
	 * Creates a configuration key with the specified name and parameter specifying the context in which a property value is defined.
	 * </p>
	 *
	 * @param name the configuration property name
	 * @param k1   the parameter name
	 * @param v1   the parameter value
	 *
	 * @return a configuration key
	 */
	public static ConfigurationKey of(String name, String k1, Object v1) {
		return of(name, Parameter.of(k1, v1));
	}
	
	/**
	 * <p>
	 * Creates a configuration key with the specified name and parameters specifying the context in which a property value is defined.
	 * </p>
	 *
	 * @param name the configuration property name
	 * @param k1   the first parameter name
	 * @param v1   the first parameter value
	 * @param k2   the second parameter name
	 * @param v2   the second parameter value
	 *
	 * @return a configuration key
	 */
	public static ConfigurationKey of(String name, String k1, Object v1, String k2, Object v2) {
		return of(name, Parameter.of(k1, v1), Parameter.of(k2, v2));
	}
	
	/**
	 * <p>
	 * Creates a configuration key with the specified name and parameters specifying the context in which a property value is defined.
	 * </p>
	 *
	 * @param name the configuration property name
	 * @param k1   the first parameter name
	 * @param v1   the first parameter value
	 * @param k2   the second parameter name
	 * @param v2   the second parameter value
	 * @param k3   the third parameter name
	 * @param v3   the third parameter value
	 * 
	 * @return a configuration key
	 */
	public static ConfigurationKey of(String name, String k1, Object v1, String k2, Object v2, String k3, Object v3) {
		return of(name, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3));
	}
	
	/**
	 * <p>
	 * Creates a configuration key with the specified name and parameters specifying the context in which a property value is defined.
	 * </p>
	 *
	 * @param name the configuration property name
	 * @param k1   the first parameter name
	 * @param v1   the first parameter value
	 * @param k2   the second parameter name
	 * @param v2   the second parameter value
	 * @param k3   the third parameter name
	 * @param v3   the third parameter value
	 * @param k4   the fourth parameter name
	 * @param v4   the fourth parameter value
	 * 
	 * @return a configuration key
	 */
	public static ConfigurationKey of(String name, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) {
		return of(name, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4));
	}
	
	/**
	 * <p>
	 * Creates a configuration key with the specified name and parameters specifying the context in which a property value is defined.
	 * </p>
	 *
	 * @param name the configuration property name
	 * @param k1   the first parameter name
	 * @param v1   the first parameter value
	 * @param k2   the second parameter name
	 * @param v2   the second parameter value
	 * @param k3   the third parameter name
	 * @param v3   the third parameter value
	 * @param k4   the fourth parameter name
	 * @param v4   the fourth parameter value
	 * @param k5   the fifth parameter name
	 * @param v5   the fifth parameter value
	 * 
	 * @return a configuration key
	 */
	public static ConfigurationKey of(String name, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5) {
		return of(name, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5));
	}
	
	/**
	 * <p>
	 * Creates a configuration key with the specified name and parameters specifying the context in which a property value is defined.
	 * </p>
	 *
	 * @param name the configuration property name
	 * @param k1   the first parameter name
	 * @param v1   the first parameter value
	 * @param k2   the second parameter name
	 * @param v2   the second parameter value
	 * @param k3   the third parameter name
	 * @param v3   the third parameter value
	 * @param k4   the fourth parameter name
	 * @param v4   the fourth parameter value
	 * @param k5   the fifth parameter name
	 * @param v5   the fifth parameter value
	 * @param k6   the sixth parameter name
	 * @param v6   the sixth parameter value
	 * 
	 * @return a configuration key
	 */
	public static ConfigurationKey of(String name, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6) {
		return of(name, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5), Parameter.of(k6, v6));
	}
	
	/**
	 * <p>
	 * Creates a configuration key with the specified name and parameters specifying the context in which a property value is defined.
	 * </p>
	 *
	 * @param name the configuration property name
	 * @param k1   the first parameter name
	 * @param v1   the first parameter value
	 * @param k2   the second parameter name
	 * @param v2   the second parameter value
	 * @param k3   the third parameter name
	 * @param v3   the third parameter value
	 * @param k4   the fourth parameter name
	 * @param v4   the fourth parameter value
	 * @param k5   the fifth parameter name
	 * @param v5   the fifth parameter value
	 * @param k6   the sixth parameter name
	 * @param v6   the sixth parameter value
	 * @param k7   the seventh parameter name
	 * @param v7   the seventh parameter value
	 * 
	 * @return a configuration key
	 */
	public static ConfigurationKey of(String name, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7) {
		return of(name, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5), Parameter.of(k6, v6), Parameter.of(k7, v7));
	}
	
	/**
	 * <p>
	 * Creates a configuration key with the specified name and parameters specifying the context in which a property value is defined.
	 * </p>
	 *
	 * @param name the configuration property name
	 * @param k1   the first parameter name
	 * @param v1   the first parameter value
	 * @param k2   the second parameter name
	 * @param v2   the second parameter value
	 * @param k3   the third parameter name
	 * @param v3   the third parameter value
	 * @param k4   the fourth parameter name
	 * @param v4   the fourth parameter value
	 * @param k5   the fifth parameter name
	 * @param v5   the fifth parameter value
	 * @param k6   the sixth parameter name
	 * @param v6   the sixth parameter value
	 * @param k7   the seventh parameter name
	 * @param v7   the seventh parameter value
	 * @param k8   the eighth parameter name
	 * @param v8   the eighth parameter value
	 * 
	 * @return a configuration key
	 */
	public static ConfigurationKey of(String name, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8) {
		return of(name, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5), Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8));
	}
	
	/**
	 * <p>
	 * Creates a configuration key with the specified name and parameters specifying the context in which a property value is defined.
	 * </p>
	 *
	 * @param name the configuration property name
	 * @param k1   the first parameter name
	 * @param v1   the first parameter value
	 * @param k2   the second parameter name
	 * @param v2   the second parameter value
	 * @param k3   the third parameter name
	 * @param v3   the third parameter value
	 * @param k4   the fourth parameter name
	 * @param v4   the fourth parameter value
	 * @param k5   the fifth parameter name
	 * @param v5   the fifth parameter value
	 * @param k6   the sixth parameter name
	 * @param v6   the sixth parameter value
	 * @param k7   the seventh parameter name
	 * @param v7   the seventh parameter value
	 * @param k8   the eighth parameter name
	 * @param v8   the eighth parameter value
	 * @param k9   the nineth parameter name
	 * @param v9   the nineth parameter value
	 * 
	 * @return a configuration key
	 */
	public static ConfigurationKey of(String name, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9) {
		return of(name, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5), Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8), Parameter.of(k9, v9));
	}
	
	/**
	 * <p>
	 * Creates a configuration key with the specified name and parameters specifying the context in which a property value is defined.
	 * </p>
	 *
	 * @param name the configuration property name
	 * @param k1   the first parameter name
	 * @param v1   the first parameter value
	 * @param k2   the second parameter name
	 * @param v2   the second parameter value
	 * @param k3   the third parameter name
	 * @param v3   the third parameter value
	 * @param k4   the fourth parameter name
	 * @param v4   the fourth parameter value
	 * @param k5   the fifth parameter name
	 * @param v5   the fifth parameter value
	 * @param k6   the sixth parameter name
	 * @param v6   the sixth parameter value
	 * @param k7   the seventh parameter name
	 * @param v7   the seventh parameter value
	 * @param k8   the eighth parameter name
	 * @param v8   the eighth parameter value
	 * @param k9   the nineth parameter name
	 * @param v9   the nineth parameter value
	 * @param k10  the tenth parameter name
	 * @param v10  the tenth parameter value
	 *
	 * @return a configuration key
	 */
	public static ConfigurationKey of(String name, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9, String k10, Object v10) {
		return of(name, Parameter.of(k1, v1), Parameter.of(k2, v2), Parameter.of(k3, v3), Parameter.of(k4, v4), Parameter.of(k5, v5), Parameter.of(k6, v6), Parameter.of(k7, v7), Parameter.of(k8, v8), Parameter.of(k9, v9), Parameter.of(k10, v10));
	}
}
