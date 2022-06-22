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
import java.security.SecureRandom;
import java.util.Base64;
import org.bouncycastle.crypto.generators.BCrypt;

/**
 * <p>
 * A password that uses <a href="https://en.wikipedia.org/wiki/Bcrypt">Bcrypt</a> hashing function to encode password.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class BCryptPassword extends AbstractPassword<BCryptPassword, BCryptPassword.Encoder> {

	/**
	 * <p>
	 * Creates a Bcrypt password with the specified value and encoder.
	 * </p>
	 *
	 * @param encoded the encoded password value
	 * @param encoder the password encoder
	 */
	@JsonCreator
	public BCryptPassword(@JsonProperty("value") String encoded, @JsonProperty("encoder") Encoder encoder) {
		super(encoded, encoder);
	}
	
	/**
	 * <p>
	 * A <a href="https://en.wikipedia.org/wiki/Bcrypt">Bcrypt</a> password encoder implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	public static class Encoder implements Password.Encoder<BCryptPassword, BCryptPassword.Encoder> {

		/**
		 * The default cost: {@code 10}.
		 */
		public static final int DEFAULT_COST = 10;
		
		/**
		 * The default salt length in kikibytes: {@code 16}.
		 */
		public static final int SALT_LENGTH = 16;
		
		/**
		 * The cost.
		 */
		@JsonIgnore
		private final int cost;

		/**
		 * The salt length in kikibytes.
		 */
		@JsonIgnore
		private final int saltLength;
		
		/**
		 * The secure random.
		 */
		@JsonIgnore
		private SecureRandom secureRandom;
		
		/**
		 * <p>
		 * Creates a default Bcrypt password encoder.
		 * </p>
		 */
		public Encoder() {
			this(DEFAULT_COST, SALT_LENGTH, PasswordUtils.DEFAULT_SECURE_RANDOM);
		}
		
		/**
		 * <p>
		 * Creates a Bcrypt password encoder with the specified cost and salt length.
		 * </p>
		 * 
		 * @param cost the cost
		 * @param saltLength   the salt length in kikibytes
		 * 
		 * @throws IllegalArgumentException if specified parameters are incorrect
		 */
		@JsonCreator
		public Encoder(@JsonProperty("cost") int cost, @JsonProperty("saltLength") int saltLength) throws IllegalArgumentException {
			this(cost, saltLength, PasswordUtils.DEFAULT_SECURE_RANDOM);
		}
		
		/**
		 * <p>
		 * Creates a Bcrypt password encoder with the specified cost, salt length and secure random.
		 * </p>
		 *
		 * @param cost         the cost
		 * @param saltLength   the salt length in kikibytes
		 * @param secureRandom the secure random
		 * 
		 * @throws IllegalArgumentException if specified parameters are incorrect
		 */
		public Encoder(int cost, int saltLength, SecureRandom secureRandom) throws IllegalArgumentException {
			if(cost < 4 || cost > 31) {
				throw new IllegalArgumentException("Invalid cost parameter " + cost + " which must be between 4 and 31 inclusive");
			}
			this.cost = cost;
			this.saltLength = saltLength;
			this.secureRandom = secureRandom != null ? secureRandom : PasswordUtils.DEFAULT_SECURE_RANDOM;
		}

		/**
		 * <p>
		 * Returns the cost.
		 * </p>
		 * 
		 * @return the cost
		 */
		@JsonProperty("cost")
		public int getCost() {
			return cost;
		}

		/**
		 * <p>
		 * Returns the salt length in kikibytes.
		 * </p>
		 * 
		 * @return the salt length
		 */
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
		public BCryptPassword recover(String encoded) throws PasswordException {
			// TODO check that the encoded value is correct (length, format...)
			return new BCryptPassword(encoded, this);
		}
		
		@Override
		public BCryptPassword encode(String raw) throws PasswordException {
			byte[] salt = PasswordUtils.generateSalt(this.secureRandom, SALT_LENGTH);
			return new BCryptPassword(PasswordUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(this.encode(raw.getBytes(), salt)), this);
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
		 */
		private byte[] encode(byte[] raw, byte[] salt) {
			byte[] hash  = BCrypt.generate(raw, salt, this.cost);
			
			byte[] encoded = new byte[SALT_LENGTH + hash.length];
			System.arraycopy(salt, 0, encoded, 0, SALT_LENGTH);
			System.arraycopy(hash, 0, encoded, SALT_LENGTH, hash.length);
			
			return encoded;
		}
		
		@Override
		public boolean matches(String raw, String encoded) throws PasswordException {
			byte[] encodedBytes = Base64.getUrlDecoder().decode(encoded);
			byte[] salt = new byte[SALT_LENGTH];
			System.arraycopy(encodedBytes, 0, salt, 0, SALT_LENGTH);

			return MessageDigest.isEqual(this.encode(raw.getBytes(), salt), encodedBytes);
		}

		@Override
		public int hashCode() {
			int hash = 3;
			hash = 97 * hash + this.cost;
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
			return this.cost == other.cost;
		}
	}
}
