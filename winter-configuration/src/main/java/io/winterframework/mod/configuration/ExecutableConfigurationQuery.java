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

import org.apache.commons.text.StringEscapeUtils;

import reactor.core.publisher.Flux;

/**
 * @author jkuhn
 *
 */
public interface ExecutableConfigurationQuery<A extends ConfigurationQuery<A, B, C>, B extends ExecutableConfigurationQuery<A, B, C>, C extends ConfigurationQueryResult<?,?>> {

	public static class Parameter {
		
		private String name;
		
		private Object value;
		
		private Parameter(String name, Object value) {
			this.name = name;
			if(!(value instanceof Number || value instanceof Boolean || value instanceof Character || value instanceof CharSequence)) {
				throw new IllegalArgumentException("Parameter value can only be a number, a boolean, a character or a string");
			}
			this.value = value;
		}
		
		public String getName() {
			return name;
		}
		
		public Object getValue() {
			return value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
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
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
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
			str.append(this.name)
				.append("=");
			if(this.value instanceof Number || this.value instanceof Boolean) {
				str.append(this.value);
			}
			else {
				str.append("\"").append(StringEscapeUtils.escapeJava(this.value.toString())).append("\"");
			}
			return this.name + "=" + (this.value instanceof Number ? this.value : "\"" + StringEscapeUtils.escapeJava(this.value.toString()) +"\""); 
		}
	}
	
	static Parameter parameter(String name, Object value) {
		return new Parameter(name, value);
	}
	
	default ExecutableConfigurationQuery<A, B, C> withParameters(String k1, Object v1) throws IllegalArgumentException {
		return this.withParameters(parameter(k1, v1));
	}
	
	default ExecutableConfigurationQuery<A, B, C> withParameters(String k1, Object v1, String k2, Object v2) throws IllegalArgumentException {
		return this.withParameters(parameter(k1, v1), parameter(k2, v2));
	}
	
	default ExecutableConfigurationQuery<A, B, C> withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3) throws IllegalArgumentException {
		return this.withParameters(parameter(k1, v1), parameter(k2, v2), parameter(k3, v3));
	}
	
	default ExecutableConfigurationQuery<A, B, C> withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) throws IllegalArgumentException {
		return this.withParameters(parameter(k1, v1), parameter(k2, v2), parameter(k3, v3), parameter(k4, v4));
	}
	
	default ExecutableConfigurationQuery<A, B, C> withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5) throws IllegalArgumentException {
		return this.withParameters(parameter(k1, v1), parameter(k2, v2), parameter(k3, v3), parameter(k4, v4), parameter(k5, v5));
	}
	
	default ExecutableConfigurationQuery<A, B, C> withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6) throws IllegalArgumentException {
		return this.withParameters(parameter(k1, v1), parameter(k2, v2), parameter(k3, v3), parameter(k4, v4), parameter(k5, v5), parameter(k6, v6));
	}
	
	default ExecutableConfigurationQuery<A, B, C> withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7) throws IllegalArgumentException {
		return this.withParameters(parameter(k1, v1), parameter(k2, v2), parameter(k3, v3), parameter(k4, v4), parameter(k5, v5), parameter(k6, v6), parameter(k7, v7));
	}
	
	default ExecutableConfigurationQuery<A, B, C> withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8) throws IllegalArgumentException {
		return this.withParameters(parameter(k1, v1), parameter(k2, v2), parameter(k3, v3), parameter(k4, v4), parameter(k5, v5), parameter(k6, v6), parameter(k7, v7), parameter(k8, v8));
	}
	
	default ExecutableConfigurationQuery<A, B, C> withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9) throws IllegalArgumentException {
		return this.withParameters(parameter(k1, v1), parameter(k2, v2), parameter(k3, v3), parameter(k4, v4), parameter(k5, v5), parameter(k6, v6), parameter(k7, v7), parameter(k8, v8), parameter(k9, v9));
	}
	
	default ExecutableConfigurationQuery<A, B, C> withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9, String k10, Object v10) throws IllegalArgumentException {
		return this.withParameters(parameter(k1, v1), parameter(k2, v2), parameter(k3, v3), parameter(k4, v4), parameter(k5, v5), parameter(k6, v6), parameter(k7, v7), parameter(k8, v8), parameter(k9, v9), parameter(k10, v10));
	}
	
	ExecutableConfigurationQuery<A, B, C> withParameters(Parameter... parameters) throws IllegalArgumentException;
	
	A and();
	
	Flux<C> execute();
}
