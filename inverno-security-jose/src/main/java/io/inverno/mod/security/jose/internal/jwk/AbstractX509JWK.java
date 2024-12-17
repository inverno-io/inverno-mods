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
package io.inverno.mod.security.jose.internal.jwk;

import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.X509JWK;
import java.net.URI;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * Base X.509 JSON Web Key implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the public key type
 * @param <B> the private key type 
 */
public abstract class AbstractX509JWK<A extends PublicKey, B extends PrivateKey> extends AbstractJWK implements X509JWK<A, B> {

	/**
	 * The underlying certificate.
	 */
	protected final X509Certificate certificate;
	
	/**
	 * The X.509 URL parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.6">RFC7517 Section 4.6</a>.
	 */
	protected URI x5u;
	
	/**
	 * The X.509 Certificate Chain parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.7">RFC7517 Section 4.7</a>.
	 */
	protected String[] x5c;
	
	/**
	 * The X.509 Certificate SHA-1 Thumbprint parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.8">RFC7517 Section 4.8</a>.
	 */
	protected String x5t;
	
	/**
	 * The X.509 Certificate SHA-256 Thumbprint parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.9">RFC7517 Section 4.9</a>.
	 */
	protected String x5t_S256; // x5t#S256
	
	/**
	 * <p>
	 * Creates an untrusted X.509 JWK.
	 * </p>
	 * 
	 * @param kty the key type
	 * 
	 * @throws JWKProcessingException if the key type is blank
	 */
	protected AbstractX509JWK(String kty) throws JWKProcessingException {
		this(kty, null, null, false);
	}
	
	/**
	 * <p>
	 * Creates a private X.509 JWK with the specified private key.
	 * </p>
	 * 
	 * @param kty the key type
	 * @param key a private key
	 * @param trusted true to create a trusted JWK, false otherwise
	 * 
	 * @throws JWKProcessingException if the key type is blank
	 */
	protected AbstractX509JWK(String kty, PrivateKey key, boolean trusted) throws JWKProcessingException {
		this(kty, key, null, trusted);
	}
	
	/**
	 * <p>
	 * Creates a public X.509 JWK with the specified X.509 certificate.
	 * </p>
	 * 
	 * <p>
	 * The JWK is considered trusted if the specified certificate, which is assumed to be validated, is not null.
	 * </p>
	 * 
	 * @param kty         the key type
	 * @param certificate an X.509 certificate
	 * 
	 * @throws JWKProcessingException if the key type is blank
	 */
	protected AbstractX509JWK(String kty, X509Certificate certificate) throws JWKProcessingException {
		this(kty, null, certificate, certificate != null);
	}
	
	/**
	 * <p>
	 * Creates an X.509 JWK with the specified private key and X.509 certificate.
	 * </p>
	 * 
	 * @param kty the key type
	 * @param key a private key
	 * @param certificate an X.509 certificate
	 * @param trusted true to create a trusted JWK, false otherwise
	 * 
	 * @throws JWKProcessingException if the key type is blank
	 */
	protected AbstractX509JWK(String kty, PrivateKey key, X509Certificate certificate, boolean trusted) throws JWKProcessingException {
		super(kty, key, trusted);
		this.certificate = certificate;
	}
	
	@Override
	public URI getX509CertificateURL() {
		return x5u;
	}

	/**
	 * <p>
	 * Sets the X.509 certificate or certificates chain URL.
	 * </p>
	 * 
	 * <p>
	 * The URI must point to a resource which provides a PEM-encoded representation of the certificate or certificate chain. The key in the first certificate must correspond to the JWK public key.
	 * </p>
	 * 
	 * @param x5u a URI
	 */
	public void setX509CertificateURL(URI x5u) {
		this.x5u = x5u;
	}

	@Override
	public String[] getX509CertificateChain() {
		return x5c;
	}

	/**
	 * <p>
	 * Sets the X.509 certificate chain.
	 * </p>
	 * 
	 * <p>
	 * The elements of the array must be Base64URL encoded DER PKIX certificate values. The key in the first certificate must correspond to the JWK public key.
	 * </p>
	 * 
	 * @param x5c an array of X.509 certificate
	 */
	public void setX509CertificateChain(String[] x5c) {
		this.x5c = x5c != null && x5c.length > 0 ? x5c : null;
	}

	@Override
	public String getX509CertificateSHA1Thumbprint() {
		return x5t;
	}

	/**
	 * <p>
	 * Sets the X.509 SHA1 certificate thumbprint used to identify the key.
	 * </p>
	 * 
	 * @param x5t an X.509 SHA1 certificate thumbprint
	 */
	public void setX509CertificateSHA1Thumbprint(String x5t) {
		this.x5t = x5t;
	}

	@Override
	public String getX509CertificateSHA256Thumbprint() {
		return x5t_S256;
	}

	/**
	 * <p>
	 * Sets the X.509 SHA256 certificate thumbprint used to identify the key.
	 * </p>
	 * 
	 * @param x5t_S256 an X.509 SHA256 certificate thumbprint
	 */
	public void setX509CertificateSHA256Thumbprint(String x5t_S256) {
		this.x5t_S256 = x5t_S256;
	}
	
	@Override
	public Optional<X509Certificate> getX509Certificate() {
		return Optional.ofNullable(certificate);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(x5c);
		result = prime * result + Objects.hash(x5t, x5t_S256, x5u);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractX509JWK<?, ?> other = (AbstractX509JWK<?, ?>) obj;
		return Arrays.equals(x5c, other.x5c) && Objects.equals(x5t, other.x5t)
				&& Objects.equals(x5t_S256, other.x5t_S256) && Objects.equals(x5u, other.x5u);
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		
		str.append("{");
		
		String jwkStr = super.toString();
		str.append(jwkStr, 1, jwkStr.length() - 1);
		
		if(this.x5u != null) {
			str.append(",\"x5u\":\"").append(this.x5u).append("\"");
		}
		if(StringUtils.isNotBlank(this.x5t)) {
			str.append(",\"x5t\":\"").append(this.x5t).append("\"");
		}
		if(StringUtils.isNotBlank(this.x5t_S256)) {
			str.append(",\"x5t#S256\":\"").append(this.x5t_S256).append("\"");
		}
		str.append("}");
		
		return str.toString();
	}
	
	
}
