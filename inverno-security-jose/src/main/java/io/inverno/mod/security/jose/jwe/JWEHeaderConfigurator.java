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

import io.inverno.mod.security.jose.JOSEHeaderConfigurator;

/**
 * <p>
 * A JWE JOSE header configurator is used in {@link JWEBuilder} or {@link JsonJWEBuilder} to configure JWE JOSE headers when building JWE objects.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the JWE JOSE header configurator type
 */
public interface JWEHeaderConfigurator<A extends JWEHeaderConfigurator<A>> extends JOSEHeaderConfigurator<A> {

	/**
	 * <p>
	 * Specifies the encryption algorithm.
	 * </p>
	 * 
	 * @param enc the encryption algorithm
	 * 
	 * @return the JWE JOSE header configurator
	 */
	A encryptionAlgorithm(String enc);

	/**
	 * <p>
	 * Specifies the compression algorithm.
	 * </p>
	 * 
	 * @param zip the compression algorithm
	 * 
	 * @return the JWE JOSE header configurator
	 */
	A compressionAlgorithm(String zip);
}
