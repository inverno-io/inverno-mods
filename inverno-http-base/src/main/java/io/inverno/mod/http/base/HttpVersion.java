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

package io.inverno.mod.http.base;

/**
 * <p>
 * Represents HTTP protocol versions.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public enum HttpVersion {

	/**
	 * HTTP protocol with undefined version.
	 */
	HTTP(0, 0, "HTTP", false),
	
	/**
	 * HTTP/1.0 protocol as defined by <a href="https://tools.ietf.org/html/rfc1945">RFC 1945</a>
	 */
	HTTP_1_0(1, 0, "HTTP/1.0", false), 
	/**
	 * HTTP/1.1 protocol as defined by <a href="https://www.rfc-editor.org/rfc/rfc7230">RFC 7230</a>
	 */
	HTTP_1_1(1, 1, "HTTP/1.1", true), 
	/**
	 * HTTP/2.0 protocol as defined by <a href="https://tools.ietf.org/html/rfc7540">RFC 7540</a>
	 */
	HTTP_2_0(2, 0, "h2", true);
	
	private final int major;
	private final int minor;
	private final String code;
	private final boolean alpn;
	
	private HttpVersion(int major, int minor, String code, boolean alpn) {
		this.major = major;
		this.minor = minor;
		this.code = code;
		this.alpn = alpn;
	}

	/**
	 * <p>
	 * Returns protocol's major version.
	 * </p>
	 * 
	 * @return the major version
	 */
	public int getMajor() {
		return major;
	}

	/**
	 * <p>
	 * Returns protocol's minor version.
	 * </p>
	 * 
	 * @return the minor version
	 */
	public int getMinor() {
		return minor;
	}

	/**
	 * <p>
	 * Returns protocol's code.
	 * </p>
	 * 
	 * @return the code
	 */
	public String getCode() {
		return code;
	}
	
	/**
	 * <p>
	 * Determines whether the protocol is supported by ALPN as specified by <a href="https://tools.ietf.org/html/rfc7301">RFC 7301</a>
	 * </p>
	 * 
	 * @return true if the protocol is supportd by ALPN, false otherwise
	 */
	public boolean isAlpn() {
		return alpn;
	}
}
