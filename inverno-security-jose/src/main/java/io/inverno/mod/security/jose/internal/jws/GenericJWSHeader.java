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
package io.inverno.mod.security.jose.internal.jws;

import com.fasterxml.jackson.annotation.JsonSetter;
import io.inverno.mod.security.jose.internal.AbstractJOSEHeader;
import io.inverno.mod.security.jose.jws.JWSHeader;
import io.inverno.mod.security.jose.jws.JWSHeaderConfigurator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Generic JWS header implementation.
 * </p>
 * 
 * <p>
 * It processed the following parameters: {@code b64}, {@code alg}, {@code jku}, {@code jwk}, {@code kid}, {@code x5u}, {@code x5c}, {@code x5t}, {@code x5t#S256}, {@code typ}, {@code cty}, {@code crit}
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericJWSHeader extends AbstractJOSEHeader<GenericJWSHeader> implements JWSHeader, JWSHeaderConfigurator<GenericJWSHeader> {

	/**
	 * The set of parameters processed in the JWS header.
	 */
	public static final Set<String> PROCESSED_PARAMETERS = Stream.concat(AbstractJOSEHeader.PROCESSED_PARAMETERS.stream(), Stream.of("b64")).collect(Collectors.toSet());
	
	/**
	 * The base64url-encode payload parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7797#section-3">RFC7797 Section 3</a>.
	 */
	protected Boolean b64;

	/**
	 * <p>
	 * Creates a generic JWS header.
	 * </p>
	 */
	public GenericJWSHeader() {
	}

	/**
	 * <p>
	 * Creates a generic JWS header.
	 * </p>
	 * 
	 * @param alg the signature JWA algorithm
	 */
	public GenericJWSHeader(String alg) {
		super(alg);
	}
	
	@Override
	public Set<String> getProcessedParameters() {
		return GenericJWSHeader.PROCESSED_PARAMETERS;
	}

	@Override
	public Boolean isBase64EncodePayload() {
		return this.b64;
	}

	@Override
	@JsonSetter("b64")
	public GenericJWSHeader base64EncodePayload(Boolean b64) {
		this.b64 = b64;
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(b64);
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
		GenericJWSHeader other = (GenericJWSHeader) obj;
		return Objects.equals(b64, other.b64);
	}
}
