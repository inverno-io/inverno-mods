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
import java.util.Arrays;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

/**
 * <p>
 * A password that uses <a href="https://en.wikipedia.org/wiki/Argon2">Argon2</a> key derivation function to encode password.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class Argon2Password extends AbstractPassword<Argon2Password, Argon2Password.Encoder> {

	/**
	 * <p>
	 * Creates an Argon2 password with the specified value and encoder.
	 * </p>
	 *
	 * @param encoded the encoded password value
	 * @param encoder the password encoder
	 */
	@JsonCreator
	public Argon2Password(@JsonProperty("value") String encoded, @JsonProperty("encoder") Encoder encoder) {
		super(encoded, encoder);
	}
	
	/**
	 * <p>
	 * An <a href="https://en.wikipedia.org/wiki/Argon2">Argon2</a> password encoder implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	public static class Encoder implements Password.Encoder<Argon2Password, Argon2Password.Encoder> {
		
		/**
		 * <p>
		 * The type of hash.
		 * </p>
		 * 
		 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
		 * @since 1.5
		 */
		public static enum HashType {
			
			/**
			 * Argon2d maximizes resistance to GPU cracking attacks.
			 */
			D(Argon2Parameters.ARGON2_d, "argon2d"),
			
			/**
			 * Argon2i is optimized to resist side-channel attacks.
			 */
			I(Argon2Parameters.ARGON2_i, "argon2i"),
			
			/**
			 * Argon2id is a hybrid version that follows the Argon2i approach for the first half pass over memory and the Argon2d approach for subsequent passes. 
			 */
			ID(Argon2Parameters.ARGON2_id, "argon2id");
			
			/**
			 * The hash type id.
			 */
			private final int id;
			
			/**
			 * The hash type name.
			 */
			private final String name;
			
			/**
			 * <p>
			 * Creates an hash type with the specified id and name.
			 * </p>
			 *
			 * @param id   the hash type id
			 * @param name the hash type name
			 */
			private HashType(int id, String name) {
				this.id = id;
				this.name = name;
			}
			
			/**
			 * <p>
			 * Returns the hash type id.
			 * </p>
			 * 
			 * @return the hash type id
			 */
			public int getId() {
				return this.id;
			}

			/**
			 * <p>
			 * Returns the hash type name.
			 * </p>
			 * 
			 * @return the name
			 */
			public String getName() {
				return this.name;
			}
			
			/**
			 * <p>
			 * Returns the hash type corresponding to the specified id.
			 * </p>
			 * 
			 * @param id an hash type id
			 * 
			 * @return an hash type
			 * 
			 * @throws IllegalArgumentException if the specified id is not supported
			 */
			public static HashType fromId(int id) throws IllegalArgumentException {
				switch(id) {
					case Argon2Parameters.ARGON2_d: return HashType.D;
					case Argon2Parameters.ARGON2_i: return HashType.I;
					case Argon2Parameters.ARGON2_id: return HashType.ID;
					default: throw new IllegalArgumentException("Unsupported type with id: " + id);
				}
			}
			
			/**
			 * <p>
			 * Returns the hash type corresponding to the specified name.
			 * </p>
			 * 
			 * @param name an hash type name
			 * 
			 * @return an hash type
			 * 
			 * @throws IllegalArgumentException if the specified name is not supported
			 */
			public static HashType fromName(String name) throws IllegalArgumentException {
				switch(name) {
					case "argon2d": return HashType.D;
					case "argon2i": return HashType.I;
					case "argon2id": return HashType.ID;
					default: throw new IllegalArgumentException("Unsupported type with name: " + name);
				}
			}
		}
		
		/**
		 * The default hash type: {@link HashType#I}.
		 */
		public static final HashType DEFAULT_TYPE = HashType.I;
		
		/**
		 * The default salt length in kikibytes: {@code 16}.
		 */
		public static final int DEFAULT_SALT_LENGTH = 16;
		
		/**
		 * The default hash length in kikibytes: {@code 32}.
		 */
		public static final int DEFAULT_HASH_LENGTH = 32;
		
		/**
		 * The default parallelism: {@code 1}.
		 */
		public static final int DEFAULT_PARALLELISM = 1;
		
		/**
		 * The default memory cost in kibibytes: {@code 12}.
		 */
		public static final int DEFAULT_MEMORY = 12;
		
		/**
		 * The default iteration count: {@code 3}.
		 */
		public static final int DEFAULT_ITERATION_COUNT = 3;
		
		/**
		 * The hash type.
		 */
		@JsonIgnore
		private final HashType type;
		
		/**
		 * The salt length.
		 */
		@JsonIgnore
		private final int saltLength;
		
		/**
		 * The hash length.
		 */
		@JsonIgnore
		private final int hashLength;
		
		/**
		 * The degree of parallelism (i.e. number of threads).
		 */
		@JsonIgnore
		private final int parallelism;
		
		/**
		 * The amount of memory (in kibibytes) to use.
		 */
		@JsonIgnore
		private final int memory;
		
		/**
		 * The number of iterations to perform.
		 */
		@JsonIgnore
		private final int iterationCount;
		
		/**
		 * Optional secret data.
		 */
		@JsonIgnore
		private final byte[] secret;
		
		/**
		 * Optional additional data.
		 */
		@JsonIgnore
		private final byte[] additionalData;
		
		/**
		 * The secure random.
		 */
		@JsonIgnore
		private final SecureRandom secureRandom;
		
		/**
		 * <p>
		 * Creates a default Argon2 password encoder.
		 * </p>
		 */
		public Encoder() {
			this(DEFAULT_TYPE, DEFAULT_SALT_LENGTH, DEFAULT_HASH_LENGTH, DEFAULT_PARALLELISM, DEFAULT_ITERATION_COUNT, DEFAULT_ITERATION_COUNT, null, null, PasswordUtils.DEFAULT_SECURE_RANDOM);
		}
		
		/**
		 * <p>
		 * Creates an Argon2 password encoder with the specified hash type, salt length, hash length, degree of parallelism, amount of memory and iteration count.
		 * </p>
		 *
		 * @param type           the hash type
		 * @param saltLength     the salt length in kikibytes
		 * @param hashLength     the hash length in kikibytes
		 * @param parallelism    the degree of parallelism
		 * @param memory         the amount of memory in kibibytes
		 * @param iterationCount the iteration count
		 */
		public Encoder(HashType type, int saltLength, int hashLength, int parallelism, int memory, int iterationCount) {
			this(type, saltLength, hashLength, parallelism, memory, iterationCount, null, null, PasswordUtils.DEFAULT_SECURE_RANDOM);
		}
		
		/**
		 * <p>
		 * Creates an Argon2 password encoder with the specified hash type, salt length, hash length, degree of parallelism, amount of memory, iteration count, secret data and
		 * additional data.
		 * </p>
		 *
		 * @param type           the hash type
		 * @param saltLength     the salt length in kikibytes
		 * @param hashLength     the hash length in kikibytes
		 * @param parallelism    the degree of parallelism
		 * @param memory         the amount of memory in kibibytes
		 * @param iterationCount the iteration count
		 * @param secret         secret data
		 * @param additionalData additional data
		 */
		@JsonCreator
		public Encoder(@JsonProperty("type") HashType type, @JsonProperty("saltLength") int saltLength, @JsonProperty("hashLength") int hashLength, @JsonProperty("parallelism") int parallelism, @JsonProperty("memory") int memory, @JsonProperty("iterationCount") int iterationCount, @JsonProperty("secret") byte[] secret, @JsonProperty("additionalData") byte[] additionalData) {
			this(type, saltLength, hashLength, parallelism, memory, iterationCount, secret, additionalData, PasswordUtils.DEFAULT_SECURE_RANDOM);
		}
		
		/**
		 * <p>
		 * Creates an Argon2 password encoder with the specified hash type, salt length, hash length, degree of parallelism, amount of memory, iteration count, secret data, additional
		 * data and secure random.
		 * </p>
		 *
		 * @param type           the hash type
		 * @param saltLength     the salt length in kikibytes
		 * @param hashLength     the hash length in kikibytes
		 * @param parallelism    the degree of parallelism
		 * @param memory         the amount of memory in kikibytes
		 * @param iterationCount the iteration count
		 * @param secret         secret data
		 * @param additionalData additional data
		 * @param secureRandom   a secure random
		 */
		public Encoder(HashType type, int saltLength, int hashLength, int parallelism, int memory, int iterationCount, byte[] secret, byte[] additionalData, SecureRandom secureRandom) {
			this.type = Objects.requireNonNull(type);
			this.saltLength = saltLength;
			this.hashLength = hashLength;
			this.parallelism = parallelism;
			this.memory = memory;
			this.iterationCount = iterationCount;
			this.secret = secret;
			this.additionalData = additionalData;
			this.secureRandom = secureRandom;
		}

		/**
		 * <p>
		 * Returns the hash type.
		 * </p>
		 * 
		 * @return the hash type
		 */
		@JsonProperty("type")
		public HashType getType() {
			return type;
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
		 * Returns the degree of parallelism (i.e. number of threads).
		 * </p>
		 * 
		 * @return the degree of parallelism
		 */
		@JsonProperty("parallelism")
		public int getParallelism() {
			return parallelism;
		}

		/**
		 * <p>
		 * Returns the amount of memory to use in kibibytes.
		 * </p>
		 * 
		 * @return the amount of memory
		 */
		@JsonProperty("memory")
		public int getMemory() {
			return memory;
		}

		/**
		 * <p>
		 * Returns the number of iterations to perform;
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
		 * Returns the optional secret key.
		 * </p>
		 * 
		 * @return the secret key or null
		 */
		@JsonProperty("secret")
		public byte[] getSecret() {
			return secret;
		}

		/**
		 * <p>
		 * Returns the optional additional data.
		 * </p>
		 * 
		 * @return the additional data or null
		 */
		@JsonProperty("additionalData")
		public byte[] getAdditionalData() {
			return additionalData;
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
		public Argon2Password recover(String encoded) throws PasswordException {
			// TODO check that the encoded value is correct (length, format...)
			return new Argon2Password(encoded, this);
		}
		
		@Override
		public Argon2Password encode(String raw) throws PasswordException {
			byte[] salt = PasswordUtils.generateSalt(this.secureRandom, this.saltLength);
			Argon2Parameters.Builder parametersBuilder = new Argon2Parameters.Builder(this.type.getId())
				.withVersion(Argon2Parameters.ARGON2_VERSION_13)
				.withSalt(salt)
				.withParallelism(this.parallelism)
				.withMemoryAsKB(this.memory)
				.withIterations(this.iterationCount);
			
			if(this.secret != null) {
				parametersBuilder = parametersBuilder.withSecret(this.secret);
			}
			
			if(this.additionalData != null) {
				parametersBuilder = parametersBuilder.withAdditional(this.additionalData);
			}
			
			byte[] hash = this.encode(raw.getBytes(), parametersBuilder.build());
			
			/* 
			 * see https://github.com/P-H-C/phc-winner-argon2/blob/master/src/encoding.c
			 *
			 * $<type>[$<version>]$<parameters>$<salt>$<hash>
			 *
			 * <type>    = "argon2" + this.type.getId()
			 * <version> = Argon2Parameters.ARGON2_VERSION_13
			 * <parameters> = "m=" + this.memory + ",t=" + this.iterationCount + "p=" + this.parallelism
			 * <salt> = Base64.getEncoder().withoutPadding().encodeToString(salt)
			 * <hash> = Base64.getEncoder().withoutPadding().encodeToString(hash)
			 */
			StringBuilder encoded = new StringBuilder();
			encoded.append('$').append(this.type.getName());
			encoded.append("$v=").append(Argon2Parameters.ARGON2_VERSION_13);
			encoded.append('$').append("m=").append(this.memory).append(",t=").append(this.iterationCount).append(",p=").append(this.parallelism);
			encoded.append('$').append(PasswordUtils.BASE64_NOPAD_ENCODER.encodeToString(salt));
			encoded.append('$').append(PasswordUtils.BASE64_NOPAD_ENCODER.encodeToString(hash));
				
			return new Argon2Password(encoded.toString(), this);
		}
		
		/**
		 * <p>
		 * Encodes the specified raw password using the specified Argon2 parameters.
		 * </p>
		 * 
		 * @param raw the raw password to encode
		 * @param parameters the Argon2 parameters to use
		 * 
		 * @return the encoded password
		 * 
		 * @throws PasswordException if there was an error encoding the raw password
		 */
		private byte[] encode(byte[] raw, Argon2Parameters parameters) throws PasswordException {
			Argon2BytesGenerator generator = new Argon2BytesGenerator();
			generator.init(parameters);
			byte[] hash = new byte[this.hashLength];
			generator.generateBytes(raw, hash);
			return hash;
		}
		
		@Override
		public boolean matches(String raw, String encoded) throws PasswordException {
			Argon2Parameters decoded_parameters;
			byte[] decoded_hash;
			Scanner scanner = new Scanner(encoded).useDelimiter("\\$|,");
			try  {
				String currentPart = scanner.next();

				Argon2Parameters.Builder parametersBuilder;
				try {
					parametersBuilder = new Argon2Parameters.Builder(HashType.fromName(currentPart).getId());
				}
				catch(IllegalArgumentException e) {
					throw new PasswordException("Invalid Argon2 encoded hash", e);
				}

				currentPart = scanner.next();
				if(currentPart.startsWith("v=")) {
					parametersBuilder.withVersion(Integer.parseInt(currentPart.substring(2)));
					currentPart = scanner.next();
				}
				
				if(!currentPart.startsWith("m=")) {
					throw new PasswordException("Invalid Argon2 encoded hash");
				}
				parametersBuilder.withMemoryAsKB(Integer.parseInt(currentPart.substring(2)));
				
				currentPart = scanner.next();
				if(!currentPart.startsWith("t=")) {
					throw new PasswordException("Invalid Argon2 encoded hash");
				}
				parametersBuilder.withIterations(Integer.parseInt(currentPart.substring(2)));
				
				currentPart = scanner.next();
				if(!currentPart.startsWith("p=")) {
					throw new PasswordException("Invalid Argon2 encoded hash");
				}
				parametersBuilder.withParallelism(Integer.parseInt(currentPart.substring(2)));

				parametersBuilder.withSalt(Base64.getDecoder().decode(scanner.next()));
				decoded_hash = Base64.getDecoder().decode(scanner.next());
				
				if(this.secret != null) {
					parametersBuilder = parametersBuilder.withSecret(this.secret);
				}
				if(this.additionalData != null) {
					parametersBuilder = parametersBuilder.withAdditional(this.additionalData);
				}
				decoded_parameters = parametersBuilder.build();
			}
			catch(NoSuchElementException e) {
				throw new PasswordException("Invalid Argon2 encoded hash", e);
			}
			return MessageDigest.isEqual(this.encode(raw.getBytes(), decoded_parameters), decoded_hash);
		}

		@Override
		public int hashCode() {
			int hash = 7;
			hash = 79 * hash + Objects.hashCode(this.type);
			hash = 79 * hash + this.saltLength;
			hash = 79 * hash + this.hashLength;
			hash = 79 * hash + this.parallelism;
			hash = 79 * hash + this.memory;
			hash = 79 * hash + this.iterationCount;
			hash = 79 * hash + Arrays.hashCode(this.secret);
			hash = 79 * hash + Arrays.hashCode(this.additionalData);
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
			if (this.hashLength != other.hashLength) {
				return false;
			}
			if (this.parallelism != other.parallelism) {
				return false;
			}
			if (this.memory != other.memory) {
				return false;
			}
			if (this.iterationCount != other.iterationCount) {
				return false;
			}
			if (this.type != other.type) {
				return false;
			}
			if (!Arrays.equals(this.secret, other.secret)) {
				return false;
			}
			return Arrays.equals(this.additionalData, other.additionalData);
		}
	}
}
