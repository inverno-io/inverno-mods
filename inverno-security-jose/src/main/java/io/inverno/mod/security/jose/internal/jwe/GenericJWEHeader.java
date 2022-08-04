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
package io.inverno.mod.security.jose.internal.jwe;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.inverno.mod.security.jose.internal.AbstractJOSEHeader;
import io.inverno.mod.security.jose.internal.JOSEUtils;
import io.inverno.mod.security.jose.jwe.JWEHeader;
import io.inverno.mod.security.jose.jwe.JWEHeaderConfigurator;
import java.util.Base64;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Generic JWE header implementation.
 * </p>
 * 
 * <p>
 * It processed the following parameters: {@code enc}, {@code zip}, {@code alg}, {@code jku}, {@code jwk}, {@code kid}, {@code x5u}, {@code x5c}, {@code x5t}, {@code x5t#S256}, {@code typ}, {@code cty}, {@code crit}
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericJWEHeader extends AbstractJOSEHeader<GenericJWEHeader> implements JWEHeader, JWEHeaderConfigurator<GenericJWEHeader> {

	/**
	 * The set of parameters processed in the JWE header.
	 */
	public static final Set<String> PROCESSED_PARAMETERS = Stream.concat(AbstractJOSEHeader.PROCESSED_PARAMETERS.stream(), Stream.of("enc", "zip")).collect(Collectors.toSet());

	/**
	 * The encryption algorithm parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7516#section-4.1.2">RFC7516 Section 4.1.2</a>.
	 */
	protected String enc;
	
	/**
	 * The compression algorithm parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7516#section-4.1.3">RFC7516 Section 4.1.3</a>.
	 */
	protected String zip;
	
	/**
	 * The raw serialized JOSE header.
	 */
	private byte[] raw;
	
	/**
	 * The set of processed parameters (defaults to {@link #PROCESSED_PARAMETERS}.
	 */
	private Set<String> processedParameters;

	/**
	 * <p>
	 * Creates a generic JWE header.
	 * </p>
	 */
	public GenericJWEHeader() {
		this.processedParameters = PROCESSED_PARAMETERS;
	}

	/**
	 * <p>
	 * Creates a generic JWE header.
	 * </p>
	 * 
	 * @param alg the key management JWA algorithm
	 * @param enc the encryption JWA algorithm
	 */
	public GenericJWEHeader(String alg, String enc) {
		super(alg);
		this.enc = enc;
		this.processedParameters = PROCESSED_PARAMETERS;
	}
	
	/**
	 * <p>
	 * Adds extra processed parameters which usually coming from key management and encryption algorithms.
	 * </p>
	 * 
	 * @param extraParameters a set of extra processed parameters
	 */
	public void setExtraProcessedParameters(Set<String> extraParameters) {
		if(extraParameters == null || extraParameters.isEmpty()) {
			this.processedParameters = PROCESSED_PARAMETERS;
		}
		else {
			this.processedParameters = new HashSet<>(PROCESSED_PARAMETERS);
			this.processedParameters.addAll(extraParameters);
		}
	}
	
	@Override
	public void setEncoded(String encoded) {
		super.setEncoded(encoded);
		this.raw = Base64.getUrlDecoder().decode(encoded);
	}

	/**
	 * <p>
	 * Sets the JSON serialized JWE header raw representation.
	 * </p>
	 * 
	 * @param raw the JSON serialized JWE header
	 */
	public void setRaw(byte[] raw) {
		this.raw = raw;
		this.encoded = raw!= null ? JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(raw) : null;
	}

	/**
	 * <p>
	 * Returns the JSON serialized JWE header raw representation.
	 * </p>
	 * 
	 * @return the JSON serialized JWE header
	 */
	@JsonIgnore
	public byte[] getRaw() {
		return raw;
	}

	@Override
	public Set<String> getProcessedParameters() {
		return this.processedParameters;
	}
	
	@Override
	public String getEncryptionAlgorithm() {
		return this.enc;
	}
	
	@Override
	public String getCompressionAlgorithm() {
		return this.zip;
	}

	@Override
	@JsonSetter("enc")
	public GenericJWEHeader encryptionAlgorithm(String enc) {
		this.enc = enc;
		return this;
	}

	@Override
	@JsonSetter("zip")
	public GenericJWEHeader compressionAlgorithm(String zip) {
		this.zip = zip;
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(enc, zip);
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
		GenericJWEHeader other = (GenericJWEHeader) obj;
		return Objects.equals(enc, other.enc) && Objects.equals(zip, other.zip);
	}
}
