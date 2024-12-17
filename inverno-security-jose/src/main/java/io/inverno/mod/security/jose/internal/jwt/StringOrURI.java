/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.security.jose.internal.jwt;

import java.net.URI;
import java.util.Objects;

/**
 * <p>
 * Represents a URI as a String or as a URI.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class StringOrURI {

	private String str;
	private URI uri;
	
	/**
	 * <p>
	 * Creates a StringOrURI from the specified String.
	 * </p>
	 * 
	 * @param str a URI as a String
	 */
	public StringOrURI(String str) {
		Objects.requireNonNull(str);
		this.str = str;
	}
	
	/**
	 * <p>
	 * Creates a StringOrURI from the specified URI.
	 * </p>
	 * 
	 * @param uri a URI
	 */
	public StringOrURI(URI uri) {
		Objects.requireNonNull(uri);
		this.uri = uri;
	}

	/**
	 * <p>
	 * Returns the URI as a String.
	 * </p>
	 * 
	 * @return the URI as a String
	 */
	public String asString() {
		if(this.str == null) {
			this.str = this.uri.toString();
		}
		return this.str;
	}
	
	/**
	 * <p>
	 * Returns the URI
	 * </p>
	 * 
	 * @return the URI
	 */
	public URI asURI() {
		if(this.uri == null) {
			this.uri = URI.create(this.str);
		}
		return this.uri;
	}

	@Override
	public int hashCode() {
		return Objects.hash(str, uri);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StringOrURI other = (StringOrURI) obj;
		return Objects.equals(str, other.str) && Objects.equals(uri, other.uri);
	}
}
