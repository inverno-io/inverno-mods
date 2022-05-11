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

import java.net.URI;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * <p>
 * An X.509 JWK builder is used to build X.509 asymetric JSON Web Keys that support X.509 JOSE header parameters: x5u, x5c, x5t and x5t#S256.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the public key type
 * @param <B> the private key type
 * @param <C> the X.509 JWK type
 * @param <D> the X.509 JWK builder type
 */
public interface X509JWKBuilder<A extends PublicKey, B extends PrivateKey, C extends X509JWK<A, B>, D extends X509JWKBuilder<A, B, C, D>> extends JWKBuilder<C, D> {

	/**
	 * <p>
	 * Specifies the X.509 certificate or certificates chain URL.
	 * </p>
	 * 
	 * @param x5u the X.509 certificate or certificate chain URL
	 * 
	 * @return this builder
	 */
	D x509CertificateURL(URI x5u);

	/**
	 * <p>
	 * specifies the X.509 certificates chain.
	 * </p>
	 * 
	 * @param x5c the X.509 certificates chain
	 * 
	 * @return this builder
	 */
	D x509CertificateChain(String[] x5c);

	/**
	 * <p>
	 * Specifies the X.509 certificate SHA1 thumbprint.
	 * </p>
	 * 
	 * @param x5t the X.509 certificate SHA1 thumbprint
	 * 
	 * @return this builder
	 */
	D x509CertificateSHA1Thumbprint(String x5t);

	/**
	 * <p>
	 * Specifies the X.509 certificate SHA256 thumbprint.
	 * </p>
	 * 
	 * @param x5t_S256 the X.509 certificate SHA256 thumbprint
	 * 
	 * @return this builder
	 */
	D x509CertificateSHA256Thumbprint(String x5t_S256);
}
