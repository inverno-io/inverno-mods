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
package io.winterframework.mod.web.internal.header;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author jkuhn
 *
 */
public class MultiParameterizedHeader extends ParameterizedHeader {

	public static abstract class AbstractBuilder<A extends MultiParameterizedHeader, B extends AbstractBuilder<A, B>> extends ParameterizedHeader.AbstractBuilder<A, B> {
		
		protected List<ParameterizedHeader> headers;
		
		@SuppressWarnings("unchecked")
		public B parameterizedValue(String parameterizedValue) {
			if(this.parameterizedValue != null || this.parameters != null) {
				if(this.headers == null) {
					this.headers = new LinkedList<>();
				}
				this.headers.add(new ParameterizedHeader(this.headerName, this.headerValue, this.parameterizedValue, this.parameters));
				this.parameters = null;
			}
			this.parameterizedValue = parameterizedValue;
			return (B)this;
		}

		@Override
		public A build() {
			if(this.parameterizedValue != null) {
				if(this.headers == null) {
					this.headers = new LinkedList<>();
				}
				this.headers.add(new ParameterizedHeader(this.headerName, this.headerValue, this.parameterizedValue, this.parameters));
			}
			return this.doBuild();
		}
		
		protected abstract A doBuild();
	}
	
	
	public static class Builder extends MultiParameterizedHeader.AbstractBuilder<MultiParameterizedHeader, Builder> {

		@Override
		protected MultiParameterizedHeader doBuild() {
			return new MultiParameterizedHeader(this.headerName, this.headerValue, this.headers);
		}
	}
	
	protected List<ParameterizedHeader> headers;
	
	/**
	 * @param headerName
	 * @param headerValue
	 */
	public MultiParameterizedHeader(String headerName, String headerValue, List<ParameterizedHeader> headers) {
		super(headerName, headerValue, null, null);
		this.headers = headers != null ? Collections.unmodifiableList(headers): List.of();
	}

	public List<ParameterizedHeader> getHeaders() {
		return this.headers;
	}
	
	@Override
	public String getParameterizedValue() {
		return null;
	}
	
	@Override
	public Map<String, String> getParameters() {
		return null;
	}
}
