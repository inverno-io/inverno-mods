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
package io.winterframework.mod.configuration;

import java.util.Collection;

import org.apache.commons.text.StringEscapeUtils;

/**
 * <p>
 * A configuration key uniquely identifies a configuration property in a
 * configuration source. It is composed of a name and a collection of
 * parameters.
 * </p>
 * 
 * <p>
 * The use of parameters in the key makes it possible to define different values
 * for the same configuration property name and retrieve them within a
 * particular context. For instance, it is possible to define a value in a test
 * environment and different value for the same property in a production
 * environment.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ExecutableConfigurationQuery
 * @see ConfigurationQueryResult
 */
public interface ConfigurationKey {

	/**
	 * <p>
	 * A parameter is used to specify the context in which a property value is
	 * defined.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	public static class Parameter {
		
		private String key;
		
		private Object value;
		
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
		 * Creates a parameter with the specified key and value.
		 * </p>
		 * 
		 * @param key   the parameter key
		 * @param value the parameter value
		 * 
		 * @return a parameter
		 * @throws IllegalArgumentException if the key is null or empty of if the value
		 *                                  is null or not a string, nor a primitive
		 */
		public static Parameter of(String key, Object value) throws IllegalArgumentException {
			return new Parameter(key, value);
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
			return this.key + "=" + (this.value instanceof Number ? this.value : "\"" + StringEscapeUtils.escapeJava(this.value.toString()) +"\""); 
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
	 * Returns the list of parameters specifying the context in which a property
	 * value is defined.
	 * </p>
	 * 
	 * @return a collection of parameters or an empty collection if the key does not
	 *         define any parameter
	 */
	Collection<Parameter> getParameters();
}
