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
	GET,
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-4.3.2">RFC 7231 Section 4.3.2</a>
	 */
	HEAD,
	/**
	 * <a href="https://tools.ietf.org/html/rfc5789">RFC 5789</a>
	 */
	PATCH,
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-4.3.3">RFC 7231 Section 4.3.1</a>
	 */
	POST,
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-4.3.4">RFC 7231 Section 4.3.4</a>
	 */
	PUT,
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-4.3.5">RFC 7231 Section 4.3.5</a>
	 */
	DELETE,
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-4.3.6">RFC 7231 Section 4.3.6</a>
	 */
	CONNECT,
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-4.3.7">RFC 7231 Section 4.3.7</a>
	 */
	OPTIONS,
	/**
	 * <a href="https://tools.ietf.org/html/rfc7231#section-4.3.8">RFC 7231 Section 4.3.8</a>
	 */
	TRACE,
	/**
	 * Describes an unknown or unsupported HTTP method
	 */
	UNKNOWN;
}
