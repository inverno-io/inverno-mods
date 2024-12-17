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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Optional;

/**
 * <p>
 * An asymmetric JSON Web key that supports X.509 JOSE header parameters: x5u, x5c, x5t and x5t#S256.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the public key type
 * @param <B> the private key type
 */
public interface X509JWK<A extends PublicKey, B extends PrivateKey> extends AsymmetricJWK<A, B> {

	/**
	 * <p>
	 * Returns the X.509 certificate or certificates chain URL parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.6">RFC7517 Section 4.6</a>.
	 * </p>
	 * 
	 * <p>
	 * The certificates chain located at the URL must be in PEM format. The certificate containing the public key must be the first certificate. The key in the first certificate MUST match the public
	 * key represented by other members of the JWK.
	 * </p>
	 * 
	 * @return the X.509 URI or null
	 */
	@JsonProperty("x5u")
	URI getX509CertificateURL();

	/**
	 * <p>
	 * Returns the X.509 certificate chain as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.7">RFC7517 Section 4.7</a>.
	 * </p>
	 * 
	 * <p>
	 * Each certificate is encoded in Base64 DER PKIX certificate value. The certificate containing the public key must be the first certificate. The key in the first certificate MUST match the public
	 * key represented by other members of the JWK.
	 * </p>
	 * 
	 * @return the X.509 certificate chain or null
	 */
	@JsonProperty("x5c")
	String[] getX509CertificateChain();

	/**
	 * <p>
	 * Returns the X.509 SHA1 certificate thumbprint as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.8">RFC7517 Section 4.8</a>.
	 * </p>
	 * 
	 * <p>
	 * This fingerprint can be used to identify the actual certificate which must MUST match the public key represented by other members of the JWK.
	 * </p>
	 * 
	 * @return the X.509 SHA1 thumbprint or null
	 */
	@JsonProperty("x5t")
	String getX509CertificateSHA1Thumbprint();

	/**
	 * <p>
	 * Returns the X.509 SHA256 certificate thumbprint as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.9">RFC7517 Section 4.9</a>.
	 * </p>
	 * 
	 * <p>
	 * This fingerprint can be used to identify the actual certificate which must MUST match the public key represented by other members of the JWK.
	 * </p>
	 * 
	 * @return the X.509 SHA1 thumbprint or null
	 */
	@JsonProperty("x5t#S256")
	String getX509CertificateSHA256Thumbprint();
	
	/**
	 * <p>
	 * Returns the resolved certificate defining the public key.
	 * </p>
	 * 
	 * @return an optional containing the resolved certificate or an empty certificate if no certificate could have been resolved when building or reading the key
	 */
	@JsonIgnore
	Optional<X509Certificate> getX509Certificate();
	
	@Override
	X509JWK<A, B> toPublicJWK();
	
	@Override
	X509JWK<A, B> trust();
}
