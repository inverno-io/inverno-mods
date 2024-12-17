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
package io.inverno.mod.security.http.digest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.inverno.mod.security.authentication.password.AbstractPassword;
import io.inverno.mod.security.authentication.password.Password;
import io.inverno.mod.security.authentication.password.PasswordException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * <p>
 * A password that uses HTTP Digest function to encode password as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.4.2">RFC 7616 Section 3.4.2</a>.
 * </p>
 *
 * <p>
 * A digest password can be used to match digest credentials in a {@link DigestCredentialsMatcher}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see DigestCredentialsMatcher
 */
public class DigestPassword extends AbstractPassword<DigestPassword, DigestPassword.Encoder> {
	
	/**
	 * <p>
	 * Creates a Digest password with the specified value and encoder.
	 * </p>
	 *
	 * @param encoded the encoded password value
	 * @param encoder the password encoder
	 */
	@JsonCreator
	public DigestPassword(@JsonProperty("value") String encoded, @JsonProperty("encoder") DigestPassword.Encoder encoder) {
		super(encoded, encoder);
	}
	
	/**
	 * <p>
	 * An HTTP Digest password encoder implementation as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.4.2">RFC 7616 Section 3.4.2</a>.
	 * </p>
	 * 
	 * <p>
	 * The encoded password corresponds to {@code A1} in RFC 7616 which is constant in the HTTP digest authentication process.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	public static class Encoder implements Password.Encoder<DigestPassword, DigestPassword.Encoder> {

		/**
		 * The algorithm.
		 */
		@JsonIgnore
		private final String algorithm;

		/**
		 * The username.
		 */
		@JsonIgnore
		private final String username;

		/**
		 * The realm.
		 */
		@JsonIgnore
		private final String realm;

		/**
		 * The message digest.
		 */
		@JsonIgnore
		private MessageDigest digest;

		/**
		 * <p>
		 * Creates a digest password encoder with the specified algorithm, username and realm.
		 * </p>
		 *
		 * @param algorithm the algorithm
		 * @param username  the username
		 * @param realm     the realm
		 */
		@JsonCreator
		public Encoder(@JsonProperty("algorithm") String algorithm, @JsonProperty("username") String username, @JsonProperty("realm") String realm) {
			this.algorithm = Objects.requireNonNull(algorithm);
			this.username = Objects.requireNonNull(username);
			this.realm = Objects.requireNonNull(realm);
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
		 * Returns the username.
		 * </p>
		 * 
		 * @return the username
		 */
		@JsonProperty("username")
		public String getUsername() {
			return username;
		}

		/**
		 * <p>
		 * Returns the realm.
		 * </p>
		 * 
		 * @return the realm
		 */
		@JsonProperty("realm")
		public String getRealm() {
			return realm;
		}

		@Override
		public DigestPassword recover(String encoded) throws PasswordException {
			// TODO check that the encoded value is correct (length, format...)
			return new DigestPassword(encoded, this);
		}

		@Override
		public DigestPassword encode(String raw) throws PasswordException {
			if(this.digest == null) {
				try {
					this.digest = MessageDigest.getInstance(this.algorithm);
				} 
				catch (NoSuchAlgorithmException e) {
					throw new PasswordException(e);
				}
			}
			return new DigestPassword(DigestUtils.h(this.digest, this.username + ":" + this.realm + ":" + raw), this);
		}

		@Override
		public boolean matches(String raw, String encoded) throws PasswordException {
			return this.encode(raw).getValue().equals(encoded);
		}
	}
}
