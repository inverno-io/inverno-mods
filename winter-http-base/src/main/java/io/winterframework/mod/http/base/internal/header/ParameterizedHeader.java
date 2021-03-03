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
package io.winterframework.mod.http.base.internal.header;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import io.winterframework.mod.http.base.header.AbstractHeaderBuilder;

/**
 * @author jkuhn
 *
 */
public class ParameterizedHeader extends GenericHeader {

	public static abstract class AbstractBuilder<A extends ParameterizedHeader, B extends AbstractBuilder<A, B>> extends AbstractHeaderBuilder<A, B> {
		
		protected String parameterizedValue;
		
		protected Map<String, String> parameters;
		
		@SuppressWarnings("unchecked")
		public B parameterizedValue(String parameterizedValue) {
			this.parameterizedValue = parameterizedValue;
			return (B)this;
		}

		@SuppressWarnings("unchecked")
		public B parameter(String name, String value) {
			if(this.parameters == null) {
				this.parameters = new LinkedHashMap<>();
			}
			this.parameters.put(name, value);
			return (B)this;
		}
	}
	
	public static class Builder extends ParameterizedHeader.AbstractBuilder<ParameterizedHeader, Builder> {

		@Override
		public ParameterizedHeader build() {
			return new ParameterizedHeader(this.headerName, this.headerValue, this.parameterizedValue, this.parameters);
		}
	}
	
	protected Map<String, String> parameters;
	
	protected String parameterizedValue;
	
	protected ParameterizedHeader(String headerName, String headerValue, String parameterizedValue, Map<String, String> parameters) {
		super(headerName, headerValue);
		this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
		this.parameterizedValue = parameterizedValue;
	}
	
	public String getParameterizedValue() {
		return this.parameterizedValue;
	}
	
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
