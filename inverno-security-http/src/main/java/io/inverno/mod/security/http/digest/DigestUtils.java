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
package io.inverno.mod.security.http.digest;

import java.security.MessageDigest;
import org.apache.commons.codec.binary.Hex;

/**
 * <p>
 * Utilities class for the HTTP digest authentication as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7616">RFC 7616</a>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
final class DigestUtils {
	
	private DigestUtils() {}
	
	/**
	 * <p>
	 * The unkeyed digest function as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.3">RFC 7616 Section 3.3</a>.
	 * </p>
	 * 
	 * @param digest the message digest
	 * @param data the data to digest
	 * 
	 * @return digested data encoded in hexadecimal
	 */
	public static String h(MessageDigest digest, String data) {
		return Hex.encodeHexString(digest.digest(data.getBytes()));
	}
	
	/**
	 * <p>
	 * The keyed digest function as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.3">RFC 7616 Section 3.3</a>.
	 * </p>
	 *
	 * @param digest the message digest
	 * @param secret the secret
	 * @param data   the data to digest
	 *
	 * @return digested data encoded in hexadecimal
	 */
	public static String kd(MessageDigest digest, String secret, String data) {
		return Hex.encodeHexString(digest.digest((secret + ":" + data).getBytes()));
	}
}
