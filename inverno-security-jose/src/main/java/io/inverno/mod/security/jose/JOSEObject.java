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
package io.inverno.mod.security.jose;

import io.inverno.mod.security.jose.jwe.JWE;
import io.inverno.mod.security.jose.jws.JWS;

/**
 * <p>
 * A JOSE object composed of a JOSE header and a payload and secured through cryptographic operations.
 * </p>
 * 
 * <p>
 * A JOSE object is serialized to a compact representation as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7515#section-7.1">RFC7515 Section 7.1</a> and
 * <a href="https://datatracker.ietf.org/doc/html/rfc7516#section-7.1">RFC7516 Section 7.1</a>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the payload type
 * @param <B> the JOSE header type
 * 
 * @see JWS
 * @see JWE
 */
public interface JOSEObject<A, B extends JOSEHeader> {
	
	/**
	 * <p>
	 * Returns the JOSE header describing the cryptographic operations and parameters employed to secure the JOSE object.
	 * </p>
	 * 
	 * @return the JOSE header
	 */
	B getHeader();
	
	/**
	 * <p>
	 * Returns the JOSE object payload.
	 * </p>
	 * 
	 * @return the payload
	 */
	A getPayload();
	
	/**
	 * <p>
	 * Serializes the JOSE object to a compact representation.
	 * </p>
	 * 
	 * @return the compact representation of the JOSE object
	 */
	String toCompact();
	
	@Override
	int hashCode();
	
	@Override
	boolean equals(Object obj);
}
