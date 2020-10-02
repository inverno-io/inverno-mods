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
 * @author jkuhn
 *
 */
public interface ConfigurationKey {

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
		
		public String getKey() {
			return key;
		}
		
		public Object getValue() {
			return value;
		}
		
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
	
	String getName();
	
	Collection<Parameter> getParameters();
}
