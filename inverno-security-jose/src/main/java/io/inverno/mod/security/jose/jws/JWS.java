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
package io.inverno.mod.security.jose.jws;

import io.inverno.mod.security.jose.JOSEObject;

/**
 * <p>
 * A JSON Web Signature object as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7515">RFC7515</a>.
 * </p>
 * 
 * <p>
 * This represents a single JWS object serialized using the compact representation as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7515#section-7.1">RFC7515 Section 7.1</a>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the payload type
 */
public interface JWS<A> extends JOSEObject<A, JWSHeader> {

	/**
	 * <p>
	 * Returns the signature encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded signature with no padding
	 */
	String getSignature();
	
	/**
	 * <p>
	 * Returns the the detached compact representation of the JWS as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7797">RFC7797</a>.
	 * </p>
	 * 
	 * <p>
	 * The resulting compact representation basically misses the payload part which must be communicated to the recipient by external means.
	 * </p>
	 * 
	 * @return a detached compact representation of the JWS
	 */
	String toDetachedCompact();
	
	@Override
	int hashCode();
	
	@Override
	boolean equals(Object obj);
}
