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
package io.inverno.mod.security.jose.jwe;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.inverno.mod.security.jose.JOSEHeader;

/**
 * <p>
 * JSON Web Encryption JOSE header as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7516#section-4">RFC7516 Section 4</a>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface JWEHeader extends JOSEHeader {

	/**
	 * <p>
	 * Returns the encryption algorithm.
	 * </p>.
	 * 
	 * @return the encryption algorithm
	 */
	@JsonProperty("enc")
	String getEncryptionAlgorithm();
	
	/**
	 * <p>
	 * Returns the compression algorithm.
	 * </p>
	 * 
	 * @return the compression algorithm or null
	 */
	@JsonProperty("zip")
	String getCompressionAlgorithm();

	@Override
	int hashCode();
	
	@Override
	boolean equals(Object obj);
}
