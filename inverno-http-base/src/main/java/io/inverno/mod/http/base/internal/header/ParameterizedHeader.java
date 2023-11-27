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
package io.inverno.mod.http.base.internal.header;

import io.inverno.mod.http.base.header.AbstractHeaderBuilder;
import io.inverno.mod.http.base.header.Header;
import io.inverno.mod.http.base.header.HeaderBuilder;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>
 * A parameterized {@link Header} implementation to represents HTTP headers with parameters.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Header
 */
public class ParameterizedHeader extends GenericHeader {

	/**
	 * <p>
	 * Base implementation for parameterized {@link HeaderBuilder}.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 *
	 * @param <A> the parameterized header type
	 * @param <B> the parmeterized header builder
	 */
	public static abstract class AbstractBuilder<A extends ParameterizedHeader, B extends AbstractBuilder<A, B>> extends AbstractHeaderBuilder<A, B> {
		
		/**
		 * The parameterized value.
		 */
		protected String parameterizedValue;
		
		/**
		 * The parameters.
		 */
		protected Map<String, String> parameters;
		
		/**
		 * <p>
		 * Specifies the parameterized value.
		 * </p>
		 * 
		 * @param parameterizedValue the parameterized value
		 * 
		 * @return the header builder
		 */
		@SuppressWarnings("unchecked")
		public B parameterizedValue(String parameterizedValue) {
			this.parameterizedValue = parameterizedValue;
			return (B)this;
		}

		/**
		 * <p>
		 * Adds a parameter.
		 * </p>
		 * 
		 * @param name  the parameter name
		 * @param value the parameter value
		 * 
		 * @return the header builder
		 */
		@SuppressWarnings("unchecked")
		public B parameter(String name, String value) {
			if(this.parameters == null) {
				this.parameters = new LinkedHashMap<>();
			}
			this.parameters.put(name, value);
			return (B)this;
		}
	}
	
	/**
	 * <p>
	 * Generic parmeterized {@link HeaderBuilder} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see MultiParameterizedHeader.AbstractBuilder
	 */
	public static class Builder extends ParameterizedHeader.AbstractBuilder<ParameterizedHeader, Builder> {

		@Override
		public ParameterizedHeader build() {
			return new ParameterizedHeader(this.headerName, this.headerValue, this.parameterizedValue, this.parameters);
		}
	}
	
	/**
	 * The header parameters.
	 */
	protected Map<String, String> parameters;
	
	/**
	 * The header parameterized value defined before the list of parameters in the header raw value. 
	 */
	protected String parameterizedValue;
	
	/**
	 * <p>
	 * Creates a parameterized header with the specified header name, header raw value, header parameterized value and parameters.
	 * </p>
	 *
	 * @param headerName         the header name
	 * @param headerValue        the header value
	 * @param parameterizedValue the header parameterized value
	 * @param parameters         the header parameters
	 */
	protected ParameterizedHeader(String headerName, String headerValue, String parameterizedValue, Map<String, String> parameters) {
		super(headerName, headerValue);
		this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
		this.parameterizedValue = parameterizedValue;
	}
	
	/**
	 * <p>
	 * Returns the header parameterized value defined before the list of parameters in the header raw value.
	 * </p>
	 * 
	 * @return the header parameterized value
	 */
	public String getParameterizedValue() {
		return this.parameterizedValue;
	}
	
	/**
	 * <p>
	 * Returns the header parameters.
	 * </p>
	 * 
	 * @return a map of parameters
	 */
	public Map<String, String> getParameters() {
		if(this.parameters != null) {
			return Collections.unmodifiableMap(this.parameters);
		}
		return Map.of();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((parameterizedValue == null) ? 0 : parameterizedValue.hashCode());
		result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ParameterizedHeader other = (ParameterizedHeader) obj;
		if (parameterizedValue == null) {
			if (other.parameterizedValue != null)
				return false;
		} else if (!parameterizedValue.equals(other.parameterizedValue))
			return false;
		if (parameters == null) {
			if (other.parameters != null)
				return false;
		} else if (!parameters.equals(other.parameters))
			return false;
		return true;
	}
}
