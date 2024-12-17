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
package io.inverno.mod.http.base.header;

/**
 * <p>
 * Represents an HTTP cookie as defined by <a href="https://tools.ietf.org/html/rfc6265#section-4.2">RFC 6265 Section 4.2</a>
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see CookieParameter
 * @see SetCookie
 */
public interface Cookie {

	/**
	 * <p>
	 * Returns the name of the cookie.
	 * </p>
	 * 
	 * @return a cookie name
	 */
	String getName();
	
	/**
	 * <p>
	 * Returns the value of the cookie
	 * </p>
	 * 
	 * @return a cookie value
	 */
	String getValue();
}
