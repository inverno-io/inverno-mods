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

import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.internal.AbstractJOSEObject;
import io.inverno.mod.security.jose.internal.AbstractJOSEPayload;
import io.inverno.mod.security.jose.jws.JWS;
import io.inverno.mod.security.jose.jws.JWSHeader;
import java.util.Objects;

/**
 * <p>
 * Generic JSON Web Signature implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the payload type
 */
public class GenericJWS<A> extends AbstractJOSEObject<A, JWSHeader, AbstractJOSEPayload<A>> implements JWS<A> {

	private final String signature;
	
	private String compact;
	private String detachedCompact;
	
	/**
	 * <p>
	 * Creates a generic JWS.
	 * </p>
	 *
	 * @param header    the JWS header
	 * @param payload   the JWS payload
	 * @param signature the Base64URL encoded signature without padding
	 */
	public GenericJWS(GenericJWSHeader header, GenericJWSPayload<A> payload, String signature) {
		super(header, payload);
		this.signature = signature;
	}

	@Override
	public String getSignature() {
		return this.signature;
	}
	
	@Override
	public String toCompact() throws JOSEProcessingException {
		if(this.compact == null) {
			StringBuilder compactBuilder = new StringBuilder();
			compactBuilder.append(this.header.getEncoded()).append(".");
			if(this.payload.getEncoded() != null) {
				compactBuilder.append(this.payload.getEncoded());
			}
			else if(this.payload.getRaw() != null) {
				if(this.payload.getRaw().contains(".")) {
					throw new JOSEProcessingException("Unencoded payloads containing '.' characters must not be used with JWS compact representation");
				}
				compactBuilder.append(this.payload.getRaw());
			}
			else {
				compactBuilder.append("");
			}
			compactBuilder.append(".");
			if(this.signature != null) {
				compactBuilder.append(this.signature);
			}
			this.compact = compactBuilder.toString();
		}
		return this.compact;
	}

	@Override
	public String toDetachedCompact() throws JOSEProcessingException {
		if(this.detachedCompact == null) {
			this.detachedCompact =  this.header.getEncoded() + ".." + (this.signature != null ? this.signature : "");
		}
		return this.detachedCompact;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(signature);
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
		GenericJWS<?> other = (GenericJWS<?>) obj;
		return Objects.equals(signature, other.signature);
	}
}
