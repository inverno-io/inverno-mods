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
package io.inverno.mod.security.jose.internal;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.inverno.mod.security.jose.JOSEHeader;
import io.inverno.mod.security.jose.JOSEHeaderConfigurator;
import io.inverno.mod.security.jose.internal.jwe.GenericJWEHeader;
import io.inverno.mod.security.jose.internal.jws.GenericJWSHeader;
import io.inverno.mod.security.jose.jwk.JWK;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Base JOSE Header implementation.
 * </p>
 * 
 * <p>
 * It processes the following parameters: {@code alg}, {@code jku}, {@code jwk}, {@code kid}, {@code x5u}, {@code x5c}, {@code x5t}, {@code x5t#S256}, {@code typ}, {@code cty}, {@code crit}
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see GenericJWSHeader
 * @see GenericJWEHeader
 * 
 * @param <A> the JOSE header type
 */
public abstract class AbstractJOSEHeader<A extends AbstractJOSEHeader<A>> implements JOSEHeader, JOSEHeaderConfigurator<A> {

	/**
	 * The set of parameters processed in the JOSE header.
	 */
	public static final Set<String> PROCESSED_PARAMETERS = Set.of("alg", "jku", "jwk", "kid", "x5u", "x5c", "x5t", "x5t#S256", "typ", "cty", "crit");
	
	/**
	 * The algorithm parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7515#section-4.1.1">RFC7515 Section 4.1.1</a> and
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7516#section-4.1.1">RFC7516 Section 4.1.1</a>.
	 */
	protected String alg;
	
	/**
	 * The JWK Set URL parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7515#section-4.1.2">RFC7515 Section 4.1.2</a> and
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7516#section-4.1.4">RFC7516 Section 4.1.4</a>.
	 */
	protected URI jku;
	
	/**
	 * The JWK parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7515#section-4.1.3">RFC7515 Section 4.1.3</a> and
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7516#section-4.1.5">RFC7516 Section 4.1.5</a>.
	 */
	protected Map<String, Object> jwk;
	
	/**
	 * The Key id parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7515#section-4.1.4">RFC7515 Section 4.1.4</a> and
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7516#section-4.1.6">RFC7516 Section 4.1.6</a>.
	 */
	protected String kid;
	
	/**
	 * The X.509 URL parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7515#section-4.1.5">RFC7515 Section 4.1.5</a> and
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7516#section-4.1.7">RFC7516 Section 4.1.7</a>.
	 */
	protected URI x5u;
	
	/**
	 * The X.509 Certificate Chain parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7515#section-4.1.6">RFC7515 Section 4.1.6</a> and
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7516#section-4.1.8">RFC7516 Section 4.1.8</a>.
	 */
	protected String[] x5c;
	
	/**
	 * The X.509 Certificate SHA-1 Thumbprint parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7515#section-4.1.7">RFC7515 Section 4.1.7</a> and
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7516#section-4.1.9">RFC7516 Section 4.1.9</a>.
	 */
	protected String x5t;
	
	/**
	 * The X.509 Certificate SHA-256 Thumbprint parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7515#section-4.1.8">RFC7515 Section 4.1.8</a> and
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7516#section-4.1.10">RFC7516 Section 4.1.10</a>.
	 */
	protected String x5t_S256;
	
	/**
	 * The Type parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7515#section-4.1.9">RFC7515 Section 4.1.9</a> and
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7516#section-4.1.11">RFC7516 Section 4.1.11</a>.
	 */
	protected String typ;
	
	/**
	 * The Content Type parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7515#section-4.1.10">RFC7515 Section 4.1.10</a> and
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7516#section-4.1.12">RFC7516 Section 4.1.12</a>.
	 */
	protected String cty;
	
	/**
	 * The Critical parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7515#section-4.1.11">RFC7515 Section 4.1.11</a> and
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7516#section-4.1.13">RFC7516 Section 4.1.13</a>.
	 */
	protected Set<String> crit;
	
	/**
	 * The custom parameters as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7515#section-4.2">RFC7515 Section 4.2</a> and
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7516#section-4.2">RFC7516 Section 4.2</a>.
	 */
	protected Map<String, Object> customParameters;
	
	/**
	 * The JOSE header encoded as Base64URL without padding
	 */
	protected String encoded;
	
	/**
	 * The key that was used to secure the JOSE object.
	 */
	protected JWK key;
	
	/**
	 * <p>
	 * Creates a JOSE header.
	 * </p>
	 */
	public AbstractJOSEHeader() {
		
	}
	
	/**
	 * <p>
	 * Creates a JOSE header with the specified algorithm.
	 * </p>
	 * 
	 * @param alg a JWA algorithm
	 */
	public AbstractJOSEHeader(String alg) {
		this.alg = alg;
	}
	
	/**
	 * <p>
	 * Sets the encoded header as Base64URL.
	 * </p>
	 * 
	 * @param encoded the Base64URL encoded header
	 */
	public void setEncoded(String encoded) {
		this.encoded = encoded;
	}

	@Override
	@JsonIgnore
	public String getEncoded() {
		return encoded;
	}
	
	/**
	 * <p>
	 * Returns the parameters processed by the JOSE header.
	 * </p>
	 * 
	 * @return a set of processed parameters
	 */
	@JsonIgnore
	public Set<String> getProcessedParameters() {
		return PROCESSED_PARAMETERS;
	}
	
	@Override
	public String getAlgorithm() {
		return this.alg;
	}

	@Override
	public URI getJWKSetURL() {
		return this.jku;
	}

	@Override
	public Map<String, Object> getJWK() {
		return this.jwk;
	}
	
	@Override
	public String getKeyId() {
		return this.kid;
	}
	
	@Override
	public URI getX509CertificateURL() {
		return this.x5u;
	}
	
	@Override
	public String[] getX509CertificateChain() {
		return this.x5c;
	}
	
	@Override
	public String getX509CertificateSHA1Thumbprint() {
		return this.x5t;
	}

	@Override
	public String getX509CertificateSHA256Thumbprint() {
		return this.x5t_S256;
	}

	@Override
	public String getType() {
		return this.typ;
	}

	@Override
	public String getContentType() {
		return this.cty;
	}
	
	@Override
	public Set<String> getCritical() {
		return this.crit;
	}
	
	@Override
	public Map<String, Object> getCustomParameters() {
		return this.customParameters;
	}

	/**
	 * <p>
	 * Sets the actual key that was used to secure the JOSE object.
	 * </p>
	 * 
	 * @param key a JWK
	 */
	public void setKey(JWK key) {
		this.key = key;
	}

	@Override
	public JWK getKey() {
		return this.key;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	@JsonSetter("alg")
	public A algorithm(String alg) {
		this.alg = alg;
		return (A)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	@JsonSetter("jku")
	public A jwkSetURL(URI jku) {
		this.jku = jku;
		return (A)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	@JsonSetter("jwk")
	public A jwk(Map<String, Object> jwk) {
		this.jwk = jwk;
		return (A)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	@JsonSetter("kid")
	public A keyId(String kid) {
		this.kid = kid;
		return (A)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	@JsonSetter("x5u")
	public A x509CertificateURL(URI x5u) {
		this.x5u = x5u;
		return (A)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	@JsonSetter("x5c")
	public A x509CertificateChain(String[] x5c) {
		this.x5c = x5c;
		return (A)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	@JsonSetter("x5t")
	public A x509CertificateSHA1Thumbprint(String x5t) {
		this.x5t = x5t;
		return (A)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	@JsonSetter("x5t#S256")
	public A x509CertificateSHA256Thumbprint(String x5t_S256) {
		this.x5t_S256 = x5t_S256;
		return (A)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	@JsonSetter("typ")
	public A type(String typ) {
		this.typ = typ;
		return (A)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	@JsonSetter("cty")
	public A contentType(String cty) {
		this.cty = cty;
		return (A)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	@JsonSetter("crit")
	public A critical(String... crit) {
		if(crit == null) {
			this.crit = null;
		}
		else if(crit.length == 0) {
			throw new IllegalArgumentException("Critical parameters must not be the empty list");
		}
		else {
			List<String> critList = Arrays.asList(crit);
			this.crit = new HashSet<>(critList);
			
			if(this.crit.size() != critList.size()) {
				throw new IllegalArgumentException("Critical parameters must not contain duplicates: " + critList.stream()
						.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
						.entrySet()
						.stream()
						.filter(e -> e.getValue() > 1)
						.map(Map.Entry::getKey)
						.collect(Collectors.joining(", "))
					);
			}
		}
		return (A)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	@JsonAnySetter
	public A addCustomParameter(String key, Object value) {
		if(value != null) {
			if(this.customParameters == null) {
				this.customParameters = new HashMap<>();
			}
			this.customParameters.put(key, value);
		}
		return (A)this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(x5c);
		result = prime * result
				+ Objects.hash(alg, crit, cty, customParameters, jku, jwk, kid, typ, x5t, x5t_S256, x5u);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractJOSEHeader<?> other = (AbstractJOSEHeader<?>) obj;
		return Objects.equals(alg, other.alg) && Objects.equals(crit, other.crit) && Objects.equals(cty, other.cty)
				&& Objects.equals(customParameters, other.customParameters) && Objects.equals(jku, other.jku)
				&& Objects.equals(jwk, other.jwk) && Objects.equals(kid, other.kid) && Objects.equals(typ, other.typ)
				&& Arrays.equals(x5c, other.x5c) && Objects.equals(x5t, other.x5t)
				&& Objects.equals(x5t_S256, other.x5t_S256) && Objects.equals(x5u, other.x5u);
	}
}
