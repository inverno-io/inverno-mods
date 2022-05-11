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

import java.security.cert.X509Certificate;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An X.509 certificates path validator is used by a {@link JWKBuilder} to validate a certificates chain.
 * </p>
 * 
 * <p>
 * A public JSON Web Key defining a valid certificate will be considered as trusted.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface X509JWKCertPathValidator {
	
	/**
	 * <p>
	 * Validates the specified certificates chain.
	 * </p>
	 * 
	 * @param certificates the certificates chain to validate
	 * 
	 * @return a single publisher emitting the validated certificate (the first certificate in the chain)
	 * 
	 * @throws JWKResolveException if the certificates chain is invalid
	 */
	Mono<X509Certificate> validate(List<X509Certificate> certificates) throws JWKResolveException;
}
