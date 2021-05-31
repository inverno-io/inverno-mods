/*
 * Copyright 2021 Jeremy KUHN
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
import java.util.function.Predicate;

/**
 * <p>
 * A URI component representing the user information part of an URI as defined
 * by <a href="https://tools.ietf.org/html/rfc3986#section-3.2.1">RFC 3986
 * Section 3.2.1</a>.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ParameterizedURIComponent
 */
class UserInfoComponent extends AbstractParameterizedURIComponent {
	
	private static final Predicate<Integer> ESCAPED_CHARACTERS =  b -> {
		return !(Character.isLetterOrDigit(b) || b == '-' || b == '.' || b == '_' || b == '~' || b == '!' || b == '$' || b == '&' || b == '\'' || b == '(' || b == ')' || b == '*' || b == '+' || b == ',' || b == ';' || b == '=' || b == ':');
	};
	
	/**
	 * <p>
	 * Creates a user information component with the specified flags, charset and
	 * raw value.
	 * </p>
	 * 
	 * @param flags    URI flags
	 * @param charset  a charset
	 * @param rawValue a raw value
	 */
	public UserInfoComponent(URIFlags flags, Charset charset, String rawValue) {
		super(flags, charset, rawValue, ESCAPED_CHARACTERS, null);
	}
}
