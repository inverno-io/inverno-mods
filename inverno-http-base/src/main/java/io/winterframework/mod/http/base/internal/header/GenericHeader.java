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
	 * Generic HTTP {@link HeaderBuilder} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see AbstractHeaderBuilder
	 */
	public static class Builder extends AbstractHeaderBuilder<GenericHeader, Builder> {

		@Override
		public GenericHeader build() {
			return new GenericHeader(this.headerName, this.headerValue);
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
		this.headerValue = headerValue;
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((headerName == null) ? 0 : headerName.hashCode());
		result = prime * result + ((headerValue == null) ? 0 : headerValue.hashCode());
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
		GenericHeader other = (GenericHeader) obj;
		if (headerName == null) {
			if (other.headerName != null)
				return false;
		} else if (!headerName.equals(other.headerName))
			return false;
		if (headerValue == null) {
			if (other.headerValue != null)
				return false;
		} else if (!headerValue.equals(other.headerValue))
			return false;
		return true;
	}
}
