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
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * <p>
 * A password that uses <a href="https://en.wikipedia.org/wiki/PBKDF2">Password-Based Key Derivation Function 2</a> to encode password.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class PBKDF2Password extends AbstractPassword<PBKDF2Password, PBKDF2Password.Encoder> {

	/**
	 * <p>
	 * Creates a PBKDF2 password with the specified value and encoder.
	 * </p>
	 *
	 * @param encoded the encoded password value
	 * @param encoder the password encoder
	 */
	@JsonCreator
	public PBKDF2Password(@JsonProperty("value") String encoded, @JsonProperty("encoder") Encoder encoder) {
		super(encoded, encoder);
	}
	
	/**
	 * <p>
	 * A PBKDF2 based password encoder implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	public static class Encoder implements Password.Encoder<PBKDF2Password, PBKDF2Password.Encoder> {

		/**
		 * SHA256 pseudorandom function.
		 */
		public static final String PRF_SHA256 = "HmacSHA256";
		
		/**
		 * SHA384 pseudorandom function.
		 */
		public static final String PRF_SHA384 = "HmacSHA384";
		
		/**
		 * SHA512 pseudorandom function.
		 */
		public static final String PRF_SHA512 = "HmacSHA512";
		
		/**
		 * Default pseudorandom function.
		 */
		public static final String DEFAULT_PRF = PRF_SHA256;
		
		/**
		 * Default salt length in kikibytes: {@code 16}.
		 */
		public static final int DEFAULT_SALT_LENGTH = 16;
		
		/**
		 * Default hash length in kikibytes: {@code 32}.
		 */
		public static final int DEFAULT_HASH_LENGTH = 32;
		
		/**
		 * Default iteration count: {@code 1000}.
		 */
		public static final int DEFAULT_ITERATION_COUNT = 1000;
		
		/**
		 * The pseudorandom function.
		 */
		@JsonIgnore
		private final String prf;
		
		/**
		 * The iteration count.
		 */
		@JsonIgnore
		private final int iterationCount;
		
		/**
		 * The salt length in kikibytes.
		 */
		@JsonIgnore
		private final int saltLength;
		
		/**
		 * The hash length in kikibytes.
		 */
		@JsonIgnore
		private final int hashLength;
		
		/**
		 * The secure random.
		 */
		@JsonIgnore
		private final SecureRandom secureRandom;

		/**
		 * <p>
		 * Creates a default PBKDF2 password encoder.
		 * </p>
		 */
		public Encoder() {
			this(DEFAULT_PRF, DEFAULT_ITERATION_COUNT, DEFAULT_SALT_LENGTH, DEFAULT_HASH_LENGTH, PasswordUtils.DEFAULT_SECURE_RANDOM);
		}
		
		/**
		 * <p>
		 * Creates a PBKDF2 password encoder with the specified pseudorandom function.
		 * </p>
		 * 
		 * @param prf the pseudorandom function
		 */
		public Encoder(String prf) {
			this(prf, DEFAULT_ITERATION_COUNT, DEFAULT_SALT_LENGTH, DEFAULT_HASH_LENGTH, PasswordUtils.DEFAULT_SECURE_RANDOM);
		}
		
		/**
		 * <p>
		 * Creates a PBKDF2 password encoder with the specified pseudorandom function, iteration count, salt length and hash length.
		 * </p>
		 *
		 * @param prf            the pseudorandom function
		 * @param iterationCount the iteration count
		 * @param saltLength     the salt length in kikibytes
		 * @param hashLength     the hash length in kikibytes
		 */
		@JsonCreator
		public Encoder(@JsonProperty("prf") String prf, @JsonProperty("iterationCount") int iterationCount, @JsonProperty("saltLength") int saltLength, @JsonProperty("hashLength") int hashLength) {
			this(prf, iterationCount, saltLength, hashLength, PasswordUtils.DEFAULT_SECURE_RANDOM);
		}
		
		/**
		 * <p>
		 * Creates a PBKDF2 password encoder with the specified pseudorandom function, iteration count, salt length, hash length and secure random.
		 * </p>
		 *
		 * @param prf            the pseudorandom function
		 * @param iterationCount the iteration count
		 * @param saltLength     the salt length in kikibytes
		 * @param hashLength     the hash length in kikibytes
		 * @param secureRandom   the secure random
		 */
		public Encoder(String prf, int iterationCount, int saltLength,  int hashLength, SecureRandom secureRandom) {
			this.prf = Objects.requireNonNull(prf);
			this.saltLength = saltLength;
			this.iterationCount = iterationCount;
			this.hashLength = hashLength;
			this.secureRandom = Objects.requireNonNull(secureRandom);
		}

		/**
		 * <p>
		 * Returns the pseudorandom function.
		 * </p>
		 * 
		 * @return the pseudorandom function
		 */
		@JsonProperty("prf")
		public String getPseudoRandomFunction() {
			return prf;
		}

		/**
		 * <p>
		 * Returns the iteration count.
		 * </p>
		 * 
		 * @return the iteration count
		 */
		@JsonProperty("iterationCount")
		public int getIterationCount() {
			return iterationCount;
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
		 * Returns the hash length in kikibytes.
		 * </p>
		 * 
		 * @return the hash length
		 */
		@JsonProperty("hashLength")
		public int getHashLength() {
			return hashLength;
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
		public PBKDF2Password recover(String encoded) throws PasswordException {
			// TODO check that the encoded value is correct (length, format...)
			return new PBKDF2Password(encoded, this);
		}
		
		@Override
		public PBKDF2Password encode(String raw) throws PasswordException {
			byte[] salt = PasswordUtils.generateSalt(this.secureRandom, this.saltLength);
			return new PBKDF2Password(PasswordUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(this.encode(raw.toCharArray(), salt)), this);
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
		private byte[] encode(char[] raw, byte[] salt) throws PasswordException {
			try {
				SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2With" + this.prf);
				PBEKeySpec keySpec = new PBEKeySpec(raw, salt, this.iterationCount, this.hashLength * 8);
				
				byte[] hash = skf.generateSecret(keySpec).getEncoded();
				
				byte[] encoded = new byte[salt.length + hash.length];
				System.arraycopy(salt, 0, encoded, 0, salt.length);
				System.arraycopy(hash, 0, encoded, salt.length, hash.length);
				
				return encoded;
			} 
			catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
				throw new PasswordException(e);
			}
		}
		
		@Override
		public boolean matches(String raw, String encoded) throws PasswordException {
			byte[] encodedBytes = Base64.getUrlDecoder().decode(encoded);
			
			byte[] salt = new byte[this.saltLength];
			System.arraycopy(encodedBytes, 0, salt, 0, this.saltLength);
			
			return MessageDigest.isEqual(this.encode(raw.toCharArray(), salt), encodedBytes);
		}

		@Override
		public int hashCode() {
			int hash = 7;
			hash = 41 * hash + Objects.hashCode(this.prf);
			hash = 41 * hash + this.iterationCount;
			hash = 41 * hash + this.saltLength;
			hash = 41 * hash + this.hashLength;
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
			if (this.iterationCount != other.iterationCount) {
				return false;
			}
			if (this.saltLength != other.saltLength) {
				return false;
			}
			if (this.hashLength != other.hashLength) {
				return false;
			}
			return Objects.equals(this.prf, other.prf);
		}
	}
}
