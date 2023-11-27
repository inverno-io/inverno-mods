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

import io.inverno.mod.http.base.header.HeaderBuilder;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * <p>
 * A parameterized header implementation that supports multiple values as defined by <a href="https://tools.ietf.org/html/rfc7230#section-3.2.2">RFC 7230 Section 3.2.2</a>.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ParameterizedHeader
 */
public class MultiParameterizedHeader extends ParameterizedHeader {

	/**
	 * <p>
	 * Base implementation for multi-parameterized {@link HeaderBuilder}.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 *
	 * @param <A> the multi-parameterized header type
	 * @param <B> the multi-parameterized header builder
	 */
	public static abstract class AbstractBuilder<A extends MultiParameterizedHeader, B extends AbstractBuilder<A, B>> extends ParameterizedHeader.AbstractBuilder<A, B> {
		
		/**
		 * The list of headers.
		 */
		protected List<ParameterizedHeader> headers;
		
		@Override
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
		
		/**
		 * <p>Builds the header.</p>
		 * 
		 * @return a multi-parameterized header
		 */
		protected abstract A doBuild();
	}
	
	/**
	 * <p>
	 * Generic multi parmeterized {@link HeaderBuilder} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see MultiParameterizedHeader.AbstractBuilder
	 */
	public static class Builder extends MultiParameterizedHeader.AbstractBuilder<MultiParameterizedHeader, Builder> {

		@Override
		protected MultiParameterizedHeader doBuild() {
			return new MultiParameterizedHeader(this.headerName, this.headerValue, this.headers);
		}
	}
	
	/**
	 * The list of headers in the multi-parameterized header.
	 */
	protected List<ParameterizedHeader> headers;
	
	/**
	 * <p>
	 * Creates a multi-parameterized header with the specified header name, header raw value and list of headers.
	 * </p>
	 *
	 * @param headerName  the header name
	 * @param headerValue the header value
	 * @param headers     the list of headers in the multi-parameterized header
	 */
	public MultiParameterizedHeader(String headerName, String headerValue, List<ParameterizedHeader> headers) {
		super(headerName, headerValue, null, null);
		this.headers = headers != null ? Collections.unmodifiableList(headers): List.of();
	}

	/**
	 * <p>
	 * Returns the list of headers in the multi-parameterized header.
	 * </p>
	 * 
	 * @return a list of parameterized headers
	 */
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
