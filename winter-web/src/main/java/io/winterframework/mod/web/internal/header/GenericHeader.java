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

import io.winterframework.mod.web.header.AbstractHeaderBuilder;
import io.winterframework.mod.web.header.Header;

/**
 * @author jkuhn
 *
 */
public class GenericHeader implements Header {

	public static class Builder extends AbstractHeaderBuilder<GenericHeader, Builder> {

		@Override
		public GenericHeader build() {
			return new GenericHeader(this.headerName, this.headerValue);
		}
	}
	
	protected String headerName;
	
	protected String headerValue;
	
	public GenericHeader(String headerName, String headerValue) {
		this.headerName = headerName;
		this.headerValue = headerValue;
	}

	@Override
	public String getHeaderName() {
		return this.headerName;
	}

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
