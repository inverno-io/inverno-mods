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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.inverno.mod.security.jose.JOSEHeader;

/**
 * <p>
 * JSON Web Signature JOSE header as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7516#section-4">RFC7516 Section 4</a>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface JWSHeader extends JOSEHeader {

	/**
	 * <p>
	 * Determines whether the payload should be encoded as Base64URL or processed and serialized unencoded as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7797#section-5">RFC7797
	 * Section 5</a>.
	 * </p>
	 * 
	 * @return true or null if the payload must be encoded as Base64URL, false otherwise
	 */
	@JsonProperty("b64")
	Boolean isBase64EncodePayload();
	
	@Override
	int hashCode();
	
	@Override
	boolean equals(Object obj);
}
