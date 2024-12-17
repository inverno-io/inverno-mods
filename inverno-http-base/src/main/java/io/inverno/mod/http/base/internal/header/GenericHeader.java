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
import java.util.Objects;

/**
 * <p>
 * Generic HTTP {@link Header} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Header
 */
public class GenericHeader implements Header {

	/**
	 * <p>
	 * HTTP {@link HeaderBuilder} implementation that builds {@link SimpleHeader}.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see AbstractHeaderBuilder
	 */
	public static class SimpleHeaderBuilder extends AbstractHeaderBuilder<Header, SimpleHeaderBuilder> {

		@Override
		public Header build() {
			return new GenericHeader.SimpleHeader(this.headerName, this.headerValue);
		}
	}
	
	/**
	 * The header name.
	 */
	protected String headerName;
	
	/**
	 * The header raw value.
	 */
	protected String headerValue;
	
	/**
	 * <p>
	 * Creates a generic header with the specified name and raw value.
	 * </p>
	 * 
	 * @param headerName  the header name
	 * @param headerValue the header raw value
	 */
	public GenericHeader(String headerName, String headerValue) {
		this.headerName = headerName.toLowerCase();
		if(headerValue != null) {
			this.headerValue = headerValue.trim();
		}
	}

	@Override
	public String getHeaderName() {
		return this.headerName;
	}

	/**
	 * <p>
	 * Sets the header raw value.
	 * </p>
	 * 
	 * @param headerValue the header raw value
	 */
	public void setHeaderValue(String headerValue) {
		this.headerValue = headerValue;
	}
	
	@Override
	public String getHeaderValue() {
		return this.headerValue;
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;
		GenericHeader that = (GenericHeader) o;
		return Objects.equals(headerName, that.headerName) && Objects.equals(headerValue, that.headerValue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(headerName, headerValue);
	}

	private static final class SimpleHeader extends GenericHeader {
	
		public SimpleHeader(String headerName, String headerValue) {
			super(headerName, headerValue);
		}
	}
}
