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
package io.inverno.mod.security.jose.jwk;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;

/**
 * <p>
 * A JWK Set as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-5">RFC7517 Section 5</a>
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class JWKSet {

	/**
	 * The list of keys contained by the JWK set
	 */
	private final JWK[] keys;

	/**
	 * <p>
	 * Creates a JWK set containing the specified keys.
	 * </p>
	 * 
	 * @param keys a list of keys
	 */
	public JWKSet(JWK... keys) {
		this.keys = keys;
	}
	
	/**
	 * <p>
	 * Returns the JWK set keys.
	 * </p>
	 * 
	 * @return an array of keys
	 */
	@JsonProperty("keys")
	public JWK[] getKeys() {
		return this.keys;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(keys);
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
		JWKSet other = (JWKSet) obj;
		return Arrays.equals(keys, other.keys);
	}
}
