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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.security.jose.JOSEObjectBuildException;
import io.inverno.mod.security.jose.JOSEObjectReadException;
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.internal.AbstractJsonJOSEObject;
import io.inverno.mod.security.jose.jwe.JWE;
import io.inverno.mod.security.jose.jwe.JWEBuildException;
import io.inverno.mod.security.jose.jwe.JWEHeader;
import io.inverno.mod.security.jose.jwe.JWEReadException;
import io.inverno.mod.security.jose.jwe.JsonJWE;
import io.inverno.mod.security.jose.jwk.JWK;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic JSON JWE implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the payload type
 * @param <B> the JSON JWE recipient type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericJsonJWE<A, B extends JsonJWE.Recipient<A>> extends AbstractJsonJOSEObject<A> implements JsonJWE<A, B> {
	
	private final JWEHeader protectedHeader;
	private final JWEHeader unprotectedHeader;
	private final String iv;
	private final String aad;
	private final String cipherText;
	private final String tag;
	private final List<B> recipients;

	/**
	 * <p>
	 * Creates a generic JSON JWE.
	 * </p>
	 *
	 * @param protectedHeader   the protected header
	 * @param unprotectedHeader the unprotected header
	 * @param iv                the Base64URL encoded initialization vector without padding
	 * @param aad               the Base64URL encoded additional authentication data without padding
	 * @param cipherText        the Base64URL encoded cipher text without padding
	 * @param tag               the Base64URL encoded authenticationt tag without padding
	 * @param recipients        the list of JSON JWE recipients
	 * @param mapper            an object mapper
	 */
	public GenericJsonJWE(JWEHeader protectedHeader, JWEHeader unprotectedHeader, String iv, String aad, String cipherText, String tag, List<B> recipients, ObjectMapper mapper) {
		super(mapper);
		this.protectedHeader = protectedHeader;
		this.unprotectedHeader = unprotectedHeader;
		this.iv = iv;
		this.aad = aad;
		this.cipherText = cipherText;
		this.tag = tag;
		this.recipients = recipients;
	}

	@Override
	@JsonIgnore
	public JWEHeader getProtectedHeader() {
		return this.protectedHeader;
	}

	/**
	 * <p>
	 * Returns the protected JWE header encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded protected JWE header.
	 */
	@JsonProperty("protected")
	public String getEncodedProtectedHeader() {
		return this.protectedHeader.getEncoded();
	}
	
	@Override
	@JsonProperty("unprotected")
	public JWEHeader getUnprotectedHeader() {
		return this.unprotectedHeader;
	}
	
	@Override
	@JsonProperty("iv")
	public String getInitializationVector() {
		return this.iv;
	}

	@Override
	@JsonProperty("aad")
	public String getAdditionalAuthenticationData() {
		return this.aad;
	}
	
	@Override
	@JsonProperty("ciphertext")
	public String getCipherText() {
		return this.cipherText;
	}

	@Override
	@JsonProperty("tag")
	public String getAuthenticationTag() {
		return this.tag;
	}
	
	@Override
	@JsonProperty("recipients")
	public List<B> getRecipients() {
		return this.recipients;
	}
	
	@Override
	public String toJson() throws JOSEProcessingException {
		if(this.recipients.size() == 1) {
			AbstractRecipient<?> recipient = (AbstractRecipient<?>)this.recipients.get(0);
			try {
				Map<String, Object> flattenedJsonJWE = new HashMap<String, Object>();
				
				String protected0 = this.getEncodedProtectedHeader();
				if(protected0 != null) {
					flattenedJsonJWE.put("protected", protected0);
				}
				JWEHeader unprotected = this.getUnprotectedHeader();
				if(unprotected != null) {
					flattenedJsonJWE.put("unprotected", unprotected);
				}
				JWEHeader header = recipient.getHeader();
				if(header != null) {
					flattenedJsonJWE.put("header", header);
				}
				String encrypted_key = recipient.getEncryptedKey();
				if(encrypted_key != null) {
					flattenedJsonJWE.put("encrypted_key", encrypted_key);
				}
				String aad = this.getAdditionalAuthenticationData();
				if(aad != null) {
					flattenedJsonJWE.put("aad", aad);
				}
				String iv = this.getInitializationVector();
				if(iv != null) {
					flattenedJsonJWE.put("iv", iv);
				}
				String ciphertext = this.getCipherText();
				if(ciphertext != null) {
					flattenedJsonJWE.put("ciphertext", ciphertext);
				}
				String tag = this.getAuthenticationTag();
				if(tag != null) {
					flattenedJsonJWE.put("tag", tag);
				}
				
				return this.mapper.writeValueAsString(flattenedJsonJWE);
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
		result = prime * result
				+ Objects.hash(aad, cipherText, iv, protectedHeader, recipients, tag, unprotectedHeader);
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
		GenericJsonJWE<?, ?> other = (GenericJsonJWE<?, ?>) obj;
		return Objects.equals(aad, other.aad) && Objects.equals(cipherText, other.cipherText)
				&& Objects.equals(iv, other.iv) && Objects.equals(protectedHeader, other.protectedHeader)
				&& Objects.equals(recipients, other.recipients) && Objects.equals(tag, other.tag)
				&& Objects.equals(unprotectedHeader, other.unprotectedHeader);
	}

	/**
	 * <p>
	 * Base {@link JsonJWE.Recipient} implementation.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 * 
	 * @param <A> the payload type
	 */
	public static abstract class AbstractRecipient<A> implements JsonJWE.Recipient<A> {
		
		/**
		 * The recipient specific JWE header.
		 */
		protected final JWEHeader header;
		
		/**
		 * <p>
		 * Creates a JSON JWE recipient.
		 * </p>
		 * 
		 * @param header the recipient specific JWE header
		 */
		public AbstractRecipient(JWEHeader header) {
			this.header = header;
		}

		@Override
		@JsonProperty("header")
		public JWEHeader getHeader() {
			return this.header;
		}

		@Override
		public int hashCode() {
			return Objects.hash(header);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AbstractRecipient<?> other = (AbstractRecipient<?>) obj;
			return Objects.equals(header, other.header);
		}
	}
	
	/**
	 * <p>
	 * Generic built recipient implementation.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 * 
	 * @param <A> the payload type
	 */
	public static class GenericBuiltRecipient<A> extends AbstractRecipient<A> implements JsonJWE.BuiltRecipient<A> {

		private final JWE<A> jwe;
		
		/**
		 * <p>
		 * Creates a JSON JWE built recipient.
		 * </p>
		 *
		 * @param header the recipient specific JWE header
		 * @param jwe    the recipient JWE
		 */
		public GenericBuiltRecipient(JWEHeader header, JWE<A> jwe) {
			super(header);
			this.jwe = jwe;
		}

		@Override
		@JsonProperty("encrypted_key")
		public String getEncryptedKey() {
			return this.jwe.getEncryptedKey();
		}
		
		@Override
		@JsonIgnore
		public JWE<A> getJWE() {
			return this.jwe;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(jwe);
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
			GenericBuiltRecipient<?> other = (GenericBuiltRecipient<?>) obj;
			return Objects.equals(jwe, other.jwe);
		}
	}
	
	/**
	 * <p>
	 * Generic read recipient implementation.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 * 
	 * @param <A> the payload type
	 */
	public static class GenericReadRecipient<A> extends AbstractRecipient<A> implements JsonJWE.ReadRecipient<A> {

		private final String encryptedKey;
		Function<Publisher<? extends JWK>, Mono<JWE<A>>> jweFactory;
		
		/**
		 * <p>
		 * Creates a JSON JWE read recipient.
		 * </p>
		 *
		 * @param header       the recipient specific JWE header
		 * @param encryptedKey the recipient specific Base64URL encoded encrypted key
		 * @param jweFactory   the recipient JWE factory used to verify and decrypt the recipient JWE
		 */
		public GenericReadRecipient(JWEHeader header, String encryptedKey, Function<Publisher<? extends JWK>, Mono<JWE<A>>> jweFactory) {
			super(header);
			this.encryptedKey = encryptedKey;
			this.jweFactory = jweFactory;
		}
		
		@Override
		@JsonProperty("encrypted_key")
		public String getEncryptedKey() {
			return this.encryptedKey;
		}

		@Override
		public Mono<JWE<A>> readJWE() throws JWEReadException, JWEBuildException, JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException {
			return this.jweFactory.apply(null);
		}

		@Override
		public Mono<JWE<A>> readJWE(Publisher<? extends JWK> keys) throws JWEReadException, JWEBuildException, JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException {
			return this.jweFactory.apply(keys);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(encryptedKey);
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
			GenericReadRecipient<?> other = (GenericReadRecipient<?>) obj;
			return Objects.equals(encryptedKey, other.encryptedKey);
		}
	}
}
