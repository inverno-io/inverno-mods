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

import java.security.Key;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A JWK key resolver is used within a {@link JWKBuilder} to resolve keys (symmetric, private or public keys) from key ids, X.509 SHA1 or X.509 SHA256 thumbprints typically specified in JOSE headers.
 * </p>
 * 
 * <p>
 * A typical implementation would rely on a Java {@link KeyStore} to securely resolve keys.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface JWKKeyResolver {
	
	/**
	 * <p>
	 * Resolves a key (symmetric or private) from a key id.
	 * </p>
	 * 
	 * @param kid a key id
	 * 
	 * @return a single key publisher or an empty publisher
	 * 
	 * @throws JWKResolveException if there was an error resolving the key
	 */
	Mono<? extends Key> resolveKeyFromKeyId(String kid) throws JWKResolveException;
	
	/**
	 * <p>
	 * Resolves a key (symmetric or private) from an X.509 SHA1 thumbprint.
	 * </p>
	 * 
	 * @param x5t an X.509 SHA1 thumbprint
	 * 
	 * @return a single key publisher or an empty publisher
	 * 
	 * @throws JWKResolveException if there was an error resolving the key
	 */
	Mono<? extends Key> resolveKeyFromX509CertificateSHA1Thumbprint(String x5t) throws JWKResolveException;
	
	/**
	 * <p>
	 * Resolves a key (symmetric or private) from an X.509 SHA256 thumbprint.
	 * </p>
	 * 
	 * @param x5t_S256 an X.509 SHA256 thumbprint
	 * 
	 * @return a single key publisher or an empty publisher
	 * 
	 * @throws JWKResolveException if there was an error resolving the key
	 */
	Mono<? extends Key> resolveKeyFromX509CertificateSHA256Thumbprint(String x5t_S256) throws JWKResolveException;
	
	/**
	 * <p>
	 * Resolves a certificate (public key) from a key id.
	 * </p>
	 * 
	 * @param kid a key id
	 * 
	 * @return a single X.509 certificate publisher or an empty publisher
	 * 
	 * @throws JWKResolveException if there was an error resolving the certificate
	 */
	Mono<X509Certificate> resolveCertificateFromKeyId(String kid) throws JWKResolveException;

	/**
	 * <p>
	 * Resolves a certificate (public key) from an X.509 SHA1 thumbprint.
	 * </p>
	 * 
	 * @param x5t an X.509 SHA1 thumbprint
	 * 
	 * @return a single X.509 certificate publisher or an empty publisher
	 * 
	 * @throws JWKResolveException if there was an error resolving the certificate
	 */
	Mono<X509Certificate> resolveCertificateFromX509CertificateSHA1Thumbprint(String x5t) throws JWKResolveException;
	
	/**
	 * <p>
	 * Resolves a certificate (public key) from an X.509 SHA256 thumbprint.
	 * </p>
	 * 
	 * @param x5t_S256 an X.509 SHA256 thumbprint
	 * 
	 * @return a single X.509 certificate publisher or an empty publisher
	 * 
	 * @throws JWKResolveException if there was an error resolving the certificate
	 */
	Mono<X509Certificate> resolveCertificateFromX509CertificateSHA256Thumbprint(String x5t_S256) throws JWKResolveException;
}
