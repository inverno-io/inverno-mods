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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.security.jose.JOSEObjectBuildException;
import io.inverno.mod.security.jose.JOSEObjectReadException;
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.internal.AbstractJsonJOSEObject;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jws.JWS;
import io.inverno.mod.security.jose.jws.JWSBuildException;
import io.inverno.mod.security.jose.jws.JWSHeader;
import io.inverno.mod.security.jose.jws.JWSReadException;
import io.inverno.mod.security.jose.jws.JsonJWS;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic JSON JWS implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the payload type
 * @param <B> the JSON JWS signature type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericJsonJWS<A, B extends JsonJWS.Signature<A>> extends AbstractJsonJOSEObject<A> implements JsonJWS<A, B> {
	
	private final GenericJWSPayload<A> payload;
	
	private final List<B> signatures;

	/**
	 * <p>
	 * Creates a generic JSON JWS.
	 * </p>
	 *
	 * @param mapper     an object mapper
	 * @param payload    the JSON JWS payload
	 * @param signatures a list of JSON JWS signatures
	 */
	public GenericJsonJWS(ObjectMapper mapper, GenericJWSPayload<A> payload, List<B> signatures) {
		super(mapper);
		this.payload = payload;
		this.signatures = signatures != null ? Collections.unmodifiableList(signatures) : List.of();
	}
	
	@Override
	@JsonIgnore
	public A getPayload() {
		return this.payload.getValue();
	}

	/**
	 * <p>
	 * Returns the payload encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded payload
	 */
	@JsonProperty("payload")
	public String getEncodedPayload() {
		if(this.payload.getEncoded() != null) {
			return this.payload.getEncoded();
		}
		else if(this.payload.getRaw() != null) {
			return this.payload.getRaw();
		}
		else {
			return null;
		}
	}
	
	@Override
	@JsonProperty("signatures")
	public List<B> getSignatures() {
		return this.signatures;
	}
	
	@Override
	public String toJson() throws JOSEProcessingException {
		if(this.signatures.size() == 1) {
			AbstractSignature<?> signature = (AbstractSignature<?>)this.signatures.getFirst();
			try {
				return this.mapper.writeValueAsString(Map.of(
					"payload", this.getEncodedPayload(), 
					"protected", signature.getEncodedProtectedHeader(), 
					"header", signature.getUnprotectedHeader(), 
					"signature", signature.getSignature())
				);
			} 
			catch (JsonProcessingException e) {
				throw new JOSEProcessingException(e);
			}
		}
		return super.toJson();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(payload, signatures);
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
		GenericJsonJWS<?, ?> other = (GenericJsonJWS<?, ?>) obj;
		return Objects.equals(payload, other.payload) && Objects.equals(signatures, other.signatures);
	}
	
	/**
	 * <p>
	 * Base {@link JsonJWS.Signature} implementation.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 * 
	 * @param <A> the payload type
	 */
	public static abstract class AbstractSignature<A> implements JsonJWS.Signature<A> {
		
		/**
		 * The signature protected JWS header configurer.
		 */
		protected final JWSHeader protectedHeader;
		
		/**
		 * The signature unprotected JWS header configurer.
		 */
		protected final JWSHeader unprotectedHeader;
		
		/**
		 * <p>
		 * Creates a JSON JWS signature.
		 * </p>
		 * 
		 * @param protectedHeader   the signature specific protected JWS header
		 * @param unprotectedHeader the signature specific unprotected JWS header
		 */
		public AbstractSignature(JWSHeader protectedHeader, JWSHeader unprotectedHeader) {
			this.protectedHeader = protectedHeader;
			this.unprotectedHeader = unprotectedHeader;
		}

		@Override
		@JsonIgnore
		public JWSHeader getProtectedHeader() {
			return this.protectedHeader;
		}
		
		/**
		 * <p>
		 * Returns the protected JWS header encoded as Base64URL.
		 * </p>
		 * 
		 * @return the Base64URL encoded protected JWS header
		 */
		@JsonProperty("protected")
		public String getEncodedProtectedHeader() {
			return this.protectedHeader != null ? this.protectedHeader.getEncoded() : null;
		}

		@Override
		@JsonProperty("header")
		public JWSHeader getUnprotectedHeader() {
			return this.unprotectedHeader;
		}

		@Override
		public int hashCode() {
			return Objects.hash(protectedHeader, unprotectedHeader);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AbstractSignature<?> other = (AbstractSignature<?>) obj;
			return Objects.equals(protectedHeader, other.protectedHeader)
					&& Objects.equals(unprotectedHeader, other.unprotectedHeader);
		}
	}
	
	/**
	 * <p>
	 * Generic built signature implementation.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 * 
	 * @param <A> the payload type
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class GenericBuiltSignature<A> extends AbstractSignature<A> implements JsonJWS.BuiltSignature<A> {

		private final JWS<A> jws;

		/**
		 * <p>
		 * Creates a JSON JWS built signature.
		 * </p>
		 *
		 * @param protectedHeader   the signature specific protected JWS header
		 * @param unprotectedHeader the signature specific unprotected JWS header
		 * @param jws               the signature JWS
		 */
		public GenericBuiltSignature(JWSHeader protectedHeader, JWSHeader unprotectedHeader, JWS<A> jws) {
			super(protectedHeader, unprotectedHeader);
			this.jws = jws;
		}

		@Override
		@JsonProperty("signature")
		public String getSignature() {
			return this.jws.getSignature();
		}

		@Override
		@JsonIgnore
		public JWS<A> getJWS() {
			return this.jws;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(jws);
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
			GenericBuiltSignature<?> other = (GenericBuiltSignature<?>) obj;
			return Objects.equals(jws, other.jws);
		}
	}
	
	/**
	 * <p>
	 * Generic read signature implementation.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 * 
	 * @param <A> the payload type
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class GenericReadSignature<A> extends AbstractSignature<A> implements JsonJWS.ReadSignature<A> {

		private final String signature;
		private final Function<Publisher<? extends JWK>, Mono<JWS<A>>> jwsFactory;

		/**
		 * <p>
		 * Creates a JSON JWS read signature.
		 * </p>
		 *
		 * @param protectedHeader   the signature specific protected JWS header
		 * @param unprotectedHeader the signature specific unprotected JWS header
		 * @param signature         the signature specific Base64URL encoded signature without padding
		 * @param jwsFactory        the signature JWS factory used to verify signature JWS
		 */
		public GenericReadSignature(JWSHeader protectedHeader, JWSHeader unprotectedHeader, String signature, Function<Publisher<? extends JWK>, Mono<JWS<A>>> jwsFactory) {
			super(protectedHeader, unprotectedHeader);
			this.signature = signature;
			this.jwsFactory = jwsFactory;
		}

		@Override
		@JsonProperty("signature")
		public String getSignature() {
			return this.signature;
		}

		@Override
		public Mono<JWS<A>> readJWS() throws JWSReadException, JWSBuildException, JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException {
			return this.jwsFactory.apply(null);
		}

		@Override
		public Mono<JWS<A>> readJWS(Publisher<? extends JWK> keys) throws JWSReadException, JWSBuildException, JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException {
			return this.jwsFactory.apply(keys);
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
			GenericReadSignature<?> other = (GenericReadSignature<?>) obj;
			return Objects.equals(signature, other.signature);
		}
	}
}
