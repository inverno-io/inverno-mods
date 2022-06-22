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
import org.bouncycastle.crypto.generators.SCrypt;

/**
 * <p>
 * A password that uses <a href="https://en.wikipedia.org/wiki/Scrypt">Scrypt</a> hashing function to encode password.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class SCryptPassword extends AbstractPassword<SCryptPassword, SCryptPassword.Encoder> {

	/**
	 * <p>
	 * Creates a Scrypt password with the specified value and encoder.
	 * </p>
	 *
	 * @param encoded the encoded password value
	 * @param encoder the password encoder
	 */
	@JsonCreator
	public SCryptPassword(@JsonProperty("value") String encoded, @JsonProperty("encoder") Encoder encoder) {
		super(encoded, encoder);
	}
	
	/**
	 * <p>
	 * A <a href="https://en.wikipedia.org/wiki/Scrypt">Scrypt</a> password encoder implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	public static class Encoder implements Password.Encoder<SCryptPassword, SCryptPassword.Encoder> {

		/**
		 * The default salt length in kikibytes: {@code 16}.
		 */
		public static final int DEFAULT_SALT_LENGTH = 16;
		
		/**
		 * The default cost factor as power of two: {@code 2^14}.
		 */
		public static final int DEFAULT_COST_FACTOR = 16384;
		
		/**
		 * The default block size factor: {@code 8}.
		 */
		public static final int DEFAULT_BLOCK_SIZE_FACTOR = 8;
		
		/**
		 * The default parallelization factor: {@code 1}.
		 */
		public static final int DEFAULT_PARALLELIZATION_FACTOR = 1;
		
		/**
		 * The default hash length in kikibytes: {@code 32}.
		 */
		public static final int DEFAULT_HASH_LENGTH = 32;
		
		/**
		 * The salt length in kikibytes.
		 */
		@JsonIgnore
		private final int saltLength;
		
		/**
		 * The cost factor as power of two.
		 */
		@JsonIgnore
		private final int costFactor;
		
		/**
		 * The block size factor.
		 */
		@JsonIgnore
		private final int blockSizeFactor;
		
		/**
		 * The parallelization factor.
		 */
		@JsonIgnore
		private final int parallelizationFactor;
		
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
		 * Creates a default Scrypt password encoder.
		 * </p>
		 */
		public Encoder() {
			this(DEFAULT_SALT_LENGTH, DEFAULT_COST_FACTOR, DEFAULT_BLOCK_SIZE_FACTOR, DEFAULT_PARALLELIZATION_FACTOR, DEFAULT_HASH_LENGTH, PasswordUtils.DEFAULT_SECURE_RANDOM);
		}
		
		/**
		 * <p>
		 * Creates a default Scrypt password encoder with the specified salt length, cost factor, block size factor, parallelization factor and hash length.
		 * </p>
		 *
		 * @param saltLength            the salt length in kikibytes
		 * @param costFactor            the cost factor
		 * @param blockSizeFactor       the block size factor
		 * @param parallelizationFactor the parallelization factor
		 * @param hashLength            the hash length in kikibytes
		 * 
		 * @throws IllegalArgumentException if specified parameters are incorrect
		 */
		@JsonCreator
		public Encoder(@JsonProperty("saltLength") int saltLength, @JsonProperty("costFactor") int costFactor, @JsonProperty("blockSizeFactor") int blockSizeFactor, @JsonProperty("parallelizationFactor") int parallelizationFactor, @JsonProperty("hashLength") int hashLength) throws IllegalArgumentException {
			this(saltLength, costFactor, blockSizeFactor, parallelizationFactor, hashLength, PasswordUtils.DEFAULT_SECURE_RANDOM);
		}

		/**
		 * <p>
		 * Creates a default Scrypt password encoder with the specified salt length, cost factor, block size factor, parallelization factor, hash length and secure random.
		 * </p>
		 *
		 * @param saltLength            the salt length in kikibytes
		 * @param costFactor            the cost factor
		 * @param blockSizeFactor       the block size factor
		 * @param parallelizationFactor the parallelization factor
		 * @param hashLength            the hash length in kikibytes
		 * @param secureRandom          the secure random
		 *
		 * @throws IllegalArgumentException if specified parameters are incorrect
		 */
		public Encoder(int saltLength, int costFactor, int blockSizeFactor, int parallelizationFactor, int hashLength, SecureRandom secureRandom) throws IllegalArgumentException {
			if(costFactor <= 1 || (costFactor & (costFactor - 1)) != 0) {
				throw new IllegalArgumentException("Cost factor must be > 1 and a power of 2");
			}
			if(blockSizeFactor == 1 && costFactor >= 65536) {
				throw new IllegalArgumentException("Cost factor must be > 1 and < 65536");
			}
			if(blockSizeFactor < 1) {
				throw new IllegalArgumentException("Block size factor must be >= 1");
			}
			int maxParallel = Integer.MAX_VALUE / (128 * blockSizeFactor * 8);
			if(parallelizationFactor < 1 || parallelizationFactor > maxParallel) {
				throw new IllegalArgumentException("Parallelisation factor must be >= 1 and <= " + maxParallel	+ " (based on block size r of " + blockSizeFactor + ")");
			}
			if(hashLength < 1) {
				throw new IllegalArgumentException("Generated hash length must be >= 1.");
			}
			
			this.saltLength = saltLength;
			this.costFactor = costFactor;
			this.blockSizeFactor = blockSizeFactor;
			this.parallelizationFactor = parallelizationFactor;
			this.hashLength = hashLength;
			this.secureRandom = secureRandom;
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
		 * Returns the cost factor.
		 * </p>
		 * 
		 * @return the cost factor.
		 */
		@JsonProperty("costFactor")
		public int getCostFactor() {
			return costFactor;
		}

		/**
		 * <p>
		 * Returns the block size factor.
		 * </p>
		 * 
		 * @return the block size factor
		 */
		@JsonProperty("blockSizeFactor")
		public int getBlockSizeFactor() {
			return blockSizeFactor;
		}

		/**
		 * <p>
		 * Returns the parallelization factor.
		 * </p>
		 * 
		 * @return the parallelization factor
		 */
		@JsonProperty("parallelizationFactor")
		public int getParallelizationFactor() {
			return parallelizationFactor;
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
		public SCryptPassword recover(String encoded) throws PasswordException {
			// TODO check that the encoded value is correct (length, format...)
			return new SCryptPassword(encoded, this);
		}
		
		@Override
		public SCryptPassword encode(String raw) throws PasswordException {
			byte[] salt = PasswordUtils.generateSalt(this.secureRandom, this.saltLength);
			return new SCryptPassword(PasswordUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(this.encode(raw.getBytes(), salt)), this);
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
			byte[] hash  = SCrypt.generate(raw, salt, this.costFactor, this.blockSizeFactor, this.parallelizationFactor, this.hashLength);
			
			byte[] encoded = new byte[this.saltLength + hash.length];
			System.arraycopy(salt, 0, encoded, 0, this.saltLength);
			System.arraycopy(hash, 0, encoded, this.saltLength, hash.length);
			
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
			int hash = 7;
			hash = 29 * hash + this.saltLength;
			hash = 29 * hash + this.costFactor;
			hash = 29 * hash + this.blockSizeFactor;
			hash = 29 * hash + this.parallelizationFactor;
			hash = 29 * hash + this.hashLength;
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
			if (this.costFactor != other.costFactor) {
				return false;
			}
			if (this.blockSizeFactor != other.blockSizeFactor) {
				return false;
			}
			if (this.parallelizationFactor != other.parallelizationFactor) {
				return false;
			}
			return this.hashLength == other.hashLength;
		}
	}
}
