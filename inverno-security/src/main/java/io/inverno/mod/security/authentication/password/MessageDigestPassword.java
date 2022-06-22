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
package io.inverno.mod.security.authentication.password;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * <p>
 * A password that uses a {@link MessageDigest} to encode password.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class MessageDigestPassword extends AbstractPassword<MessageDigestPassword, MessageDigestPassword.Encoder> {

	/**
	 * <p>
	 * Creates a message digest password with the specified value and encoder.
	 * </p>
	 *
	 * @param encoded the encoded password value
	 * @param encoder the password encoder
	 */
	@JsonCreator
	public MessageDigestPassword(@JsonProperty("value") String encoded, @JsonProperty("encoder") Encoder encoder) {
		super(encoded, encoder);
	}
	
	/**
	 * <p>
	 * A {@link MessageDigest} based password encoder implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	public static class Encoder implements Password.Encoder<MessageDigestPassword, MessageDigestPassword.Encoder> {
	
		/**
		 * The default algorithm: {@code SHA-512}.
		 */
		public static final String DEFAULT_ALGORITHM = "SHA-512";
		
		/**
		 * The default secret: {@code new byte[0]}.
		 */
		public static final byte[] DEFAULT_SECRET = new byte[0];
		
		/**
		 * The default salt length in bytes: {@code 16}.
		 */
		public static final int DEFAULT_SALT_LENGTH = 16;
		
		/**
		 * The algorithm.
		 */
		@JsonIgnore
		private final String algorithm;
		
		/**
		 * The secret.
		 */
		@JsonIgnore
		private final byte[] secret;
		
		/**
		 * The salt length in kikibytes.
		 */
		@JsonIgnore
		private final int saltLength;
		
		/**
		 * The secure random.
		 */
		@JsonIgnore
		private final SecureRandom secureRandom;
		
		/**
		 * The message digest.
		 */
		@JsonIgnore
		private MessageDigest digest;

		/**
		 * <p>
		 * Creates a default message digest password encoder.
		 * </p>
		 */
		public Encoder() {
			this(DEFAULT_ALGORITHM, DEFAULT_SECRET, DEFAULT_SALT_LENGTH, PasswordUtils.DEFAULT_SECURE_RANDOM);
		}
		
		/**
		 * <p>
		 * Creates a message digest password encoder using the specified algorithm.
		 * </p>
		 * 
		 * @param algorithm the algorithm
		 */
		public Encoder(String algorithm) {
			this(algorithm, DEFAULT_SECRET, DEFAULT_SALT_LENGTH, PasswordUtils.DEFAULT_SECURE_RANDOM);
		}
		
		/**
		 * <p>
		 * Creates a message digest password encoder using the specified algorithm and secret.
		 * </p>
		 * 
		 * @param algorithm the algorithm
		 * @param secret    the secret
		 */
		public Encoder(String algorithm, byte[] secret) {
			this(algorithm, secret, DEFAULT_SALT_LENGTH, PasswordUtils.DEFAULT_SECURE_RANDOM);
		}
		
		/**
		 * <p>
		 * Creates a message digest password encoder using the specified algorithm, secret and salt length.
		 * </p>
		 * 
		 * @param algorithm  the algorithm
		 * @param secret     the secret
		 * @param saltLength the salt length in kikibytes
		 */
		@JsonCreator
		public Encoder(@JsonProperty("algorithm") String algorithm, @JsonProperty("secret") byte[] secret, @JsonProperty("saltLength") int saltLength) {
			this(algorithm, secret, saltLength, PasswordUtils.DEFAULT_SECURE_RANDOM);
		}
		
		/**
		 * <p>
		 * Creates a message digest password encoder using the specified algorithm, secret, salt length and secure random.
		 * </p>
		 *
		 * @param algorithm    the algorithm
		 * @param secret       the secret
		 * @param saltLength   the salt length in kikibytes
		 * @param secureRandom the secure random
		 */
		public Encoder(String algorithm, byte[] secret, int saltLength, SecureRandom secureRandom) {
			this.algorithm = Objects.requireNonNull(algorithm);
			this.secret = secret != null ? secret : DEFAULT_SECRET;
			this.saltLength = saltLength;
			this.secureRandom = secureRandom != null ? secureRandom : PasswordUtils.DEFAULT_SECURE_RANDOM;
		}

		/**
		 * <p>
		 * Returns the algorithm.
		 * </p>
		 * 
		 * @return the algorithm
		 */
		@JsonProperty("algorithm")
		public String getAlgorithm() {
			return algorithm;
		}

		/**
		 * <p>
		 * Returns the secret.
		 * </p>
		 * 
		 * @return the secret
		 */
		@JsonProperty("secret")
		public byte[] getSecret() {
			return secret;
		}

		/**
		 * <p>
		 * Returns the salt length in kikibytes.
		 * </p>
		 * 
		 * @return the salt length
		 */
		@JsonProperty("saltLength")
		public int getSaltLength() {
			return saltLength;
		}

		/**
		 * <p>
		 * Returns the secure random.
		 * </p>
		 * 
		 * @return the secure random
		 */
		@JsonIgnore
		public SecureRandom getSecureRandom() {
			return secureRandom;
		}

		@Override
		public MessageDigestPassword recover(String encoded) throws PasswordException {
			// TODO check that the encoded value is correct (length, format...)
			return new MessageDigestPassword(encoded, this);
		}
		
		@Override
		public MessageDigestPassword encode(String raw) throws PasswordException {
			byte[] salt = PasswordUtils.generateSalt(this.secureRandom, this.saltLength);
			return new MessageDigestPassword(PasswordUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(this.encode(raw.getBytes(), salt)), this);
		}
		
		/**
		 * <p>
		 * Encodes the specified raw password using the specified salt.
		 * </p>
		 * 
		 * @param raw  the raw password to encode
		 * @param salt the salt to use
		 * 
		 * @return the encoded password
		 * 
		 * @throws PasswordException if there was an error encoding the password
		 */
		private byte[] encode(byte[] raw, byte[] salt) throws PasswordException {
			if(this.digest == null) {
				try {
					this.digest = MessageDigest.getInstance(this.algorithm);
				} 
				catch (NoSuchAlgorithmException e) {
					throw new PasswordException(e);
				}
			}
			
			// hash(secret + salt + raw)
			byte[] input = new byte[secret.length + salt.length + raw.length];
			
			System.arraycopy(this.secret, 0, input, 0, this.secret.length);
			System.arraycopy(salt, 0, input, this.secret.length, salt.length);
			System.arraycopy(raw, 0, input, this.secret.length + salt.length, raw.length);
			
			byte[] hash = this.digest.digest(input);
			
			// salt + hash
			byte[] encoded = new byte[salt.length + hash.length];
			System.arraycopy(salt, 0, encoded, 0, salt.length);
			System.arraycopy(hash, 0, encoded, salt.length, hash.length);
			
			return encoded;
		}

		@Override
		public boolean matches(String raw, String encoded) throws PasswordException {
			byte[] encodedBytes = Base64.getUrlDecoder().decode(encoded);
			byte[] salt = new byte[this.saltLength];
			System.arraycopy(encodedBytes, 0, salt, 0, this.saltLength);

			return MessageDigest.isEqual(this.encode(raw.getBytes(), salt), encodedBytes);
		}

		@Override
		public int hashCode() {
			int hash = 3;
			hash = 19 * hash + Objects.hashCode(this.algorithm);
			hash = 19 * hash + Arrays.hashCode(this.secret);
			hash = 19 * hash + this.saltLength;
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final Encoder other = (Encoder) obj;
			if (this.saltLength != other.saltLength) {
				return false;
			}
			if (!Objects.equals(this.algorithm, other.algorithm)) {
				return false;
			}
			return Arrays.equals(this.secret, other.secret);
		}
	}
}
