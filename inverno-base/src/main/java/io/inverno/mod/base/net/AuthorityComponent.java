/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.base.net;

import java.nio.charset.Charset;

/**
 * <p>
 * An authority component representing the fragment part of an URI as defined by <a href="https://tools.ietf.org/html/rfc3986#section-3.2">RFC 3986 Section 3.2</a>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
class AuthorityComponent extends AbstractParameterizedURIComponent {

	/**
	 * <p>
	 * Creates an authority component with the specified flags, charset and raw value.
	 * </p>
	 *
	 * @param flags    URI flags
	 * @param charset  a charset
	 * @param rawValue a raw value
	 */
	public AuthorityComponent(URIFlags flags, Charset charset, String rawValue) {
		super(flags, charset, rawValue);
	}

	/**
	 * <p>
	 * Determines whether the authority is absolute (i.e. starts with {@code /}).
	 * </p>
	 *
	 * <p>
	 * An absolute authority leads to a standard hierarchical URI. This implementation can handle opaque URIs whose scheme-specific part does not begin with {@code /} but which can also be
	 * hierarchical (i.e. having path, query parameters in addition to fragment).
	 * </p>
	 *
	 * @return true if the authority is absolute, false otherwise
	 */
	public boolean isAbsolute() {
		return this.rawValue.startsWith("/");
	}
}
