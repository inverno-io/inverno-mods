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

import java.util.Optional;
import javax.crypto.SecretKey;

/**
 * <p>
 * A symmetric JSON Web Key based on a symmetric key.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface SymmetricJWK extends JWK {
	
	/**
	 * <p>
	 * Converts the JWK to its corresponding secret key.
	 * </p>
	 * 
	 * @return an optional containing the secret key or an empty optional if the key does not contain secret information
	 * 
	 * @throws JWKProcessingException if there was an error converting the JWK to a secret key
	 */
	Optional<SecretKey> toSecretKey() throws JWKProcessingException;
	
	@Override
	SymmetricJWK toPublicJWK();
	
	@Override
	SymmetricJWK trust();
}
