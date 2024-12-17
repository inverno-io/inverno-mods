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
package io.inverno.mod.http.base;

/**
 * <p>
 * Enumeration of HTTP methods as defined by <a href="https://tools.ietf.org/html/rfc7231#section-4.3">RFC 7231 Section 4.3</a>.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public enum Method {
	
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-4.3.1">RFC 7231 Section 4.3.1</a>
	 */
	GET(false),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-4.3.2">RFC 7231 Section 4.3.2</a>
	 */
	HEAD(false),
	/**
	 * <a href="https://tools.ietf.org/html/rfc5789">RFC 5789</a>
	 */
	PATCH(true),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-4.3.3">RFC 7231 Section 4.3.1</a>
	 */
	POST(true),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-4.3.4">RFC 7231 Section 4.3.4</a>
	 */
	PUT(true),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-4.3.5">RFC 7231 Section 4.3.5</a>
	 */
	DELETE(true),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-4.3.6">RFC 7231 Section 4.3.6</a>
	 */
	CONNECT(false),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-4.3.7">RFC 7231 Section 4.3.7</a>
	 */
	OPTIONS(false),
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-4.3.8">RFC 7231 Section 4.3.8</a>
	 */
	TRACE(false),
	/**
	 * Describes an unknown or unsupported HTTP method
	 */
	UNKNOWN(false);

	private final boolean bodyAllowed;

	/**
	 * <p>
	 * Creates a method.
	 * </p>
	 *
	 * @param bodyAllowed true to indicate that a body is allowed in the request, false otherwise
	 */
	Method(boolean bodyAllowed) {
		this.bodyAllowed = bodyAllowed;
	}

	/**
	 * <p>
	 * Determines whether a body can be generated in the request as defined by <a href="https://datatracker.ietf.org/doc/html/rfc9110#name-method-definitions">RFC 91110 Section 9.3</a>.
	 * </p>
	 *
	 * @return true if the method allows content in the request, false otherwise
	 */
	public boolean isBodyAllowed() {
		return bodyAllowed;
	}
}
