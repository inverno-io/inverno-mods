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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * <p>
 * A password represents secret data that can be used to authenticate an entity.
 * </p>
 * 
 * <p>
 * A password is kept safe by encoding its raw representation in a way that makes it difficult for an attacker to compute it or guess it. This representation exists to provide a secure way to 
 * manipulate and store password data. It allows encoding a password and match a raw password with an encoded representation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the password type
 * @param <B> the password encoder type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY)
public interface Password<A extends Password<A, B>, B extends Password.Encoder<A, B>> {
	
	/**
	 * <p>
	 * Returns the representation of the password encoded with the password encoder.
	 * </p>
	 * 
	 * @return the encoded password representation
	 */
	@JsonProperty("value")
	String getValue();
	
	/**
	 * <p>
	 * Returns the password encoder.
	 * </p>
	 * 
	 * @return a password encoder
	 */
	@JsonProperty("encoder")
	B getEncoder();
	
	/**
	 * <p>
	 * Determines whether the password matches the specified raw password.
	 * </p>
	 * 
	 * @param raw the raw password to match
	 * 
	 * @return true if the password matches the raw password, false otherwise
	 * 
	 * @throws PasswordException if there was an error matching passwords
	 */
	default boolean matches(String raw) throws PasswordException {
		return this.getEncoder().matches(raw, this.getValue());
	}
	
	/**
	 * <p>
	 * Determines whether the password matches the specified password.
	 * </p>
	 * 
	 * @param other the password to match
	 * 
	 * @return true if the password matches the other password, false otherwise
	 * 
	 * @throws PasswordException if there was an error matching passwords or if the specified password is of an incompatible type
	 */
	@SuppressWarnings("unchecked")
	default boolean matches(Password<?, ?> other) throws PasswordException {
		return this.getEncoder().matches((A)this, other);
	}
	
	/**
	 * <p>
	 * A password encoder used to encode a raw password into a secured password representation.
	 * </p>
	 * 
	 * @param <A> the password type
	 * @param <B> the password encoder type
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY)
	interface Encoder<A extends Password<A, B>, B extends Password.Encoder<A, B>> {
		
		/**
		 * <p>
		 * Recovers a password from its encoded representation generated with this encoder.
		 * </p>
		 *
		 * @param encoded an encoded password representation
		 *
		 * @return a password
		 *
		 * @throws PasswordException if there was an error recovering the password or if the specified encoded representation was not generated with this encoder
		 */
		A recover(String encoded) throws PasswordException;
		
		/**
		 * <p>
		 * Encodes the specified raw password.
		 * </p>
		 * 
		 * @param raw a raw password
		 * 
		 * @return an encoded password
		 * 
		 * @throws PasswordException if there was an error encoding the password
		 */
		A encode(String raw) throws PasswordException;
	
		
		/**
		 * <p>
		 * Matches the specified raw password with the specified encoded representation.
		 * </p>
		 * 
		 * @param raw     a raw password
		 * @param encoded an encoded password
		 * 
		 * @return true if passwords match, false otherwise
		 * 
		 * @throws PasswordException if there was an error matching passwords
		 */
		boolean matches(String raw, String encoded) throws PasswordException;
		
		/**
		 * <p>
		 * Matches the specified passwords.
		 * </p>
		 * 
		 * @param password a password
		 * @param other another password
		 * 
		 * @return true if passwords match, false otherwise
		 * @throws PasswordException if there was an error matching passwords or if passwords are of incompatible types
		 */
		default boolean matches(A password, Password<?, ?> other) throws PasswordException {
			if(password != null && other != null) {
				if(other instanceof RawPassword) {
					return this.matches(other.getValue(), password.getValue());
				}
				else if(password instanceof RawPassword) {
					return other.matches(password.getValue());
				}
				else if(password.getClass().equals(other.getClass())) {
					return password.getValue().equals(other.getValue());
				}
				else {
					throw new PasswordException("Incompatible passwords: " + password.getClass().getCanonicalName() + " and " + other.getClass().getCanonicalName());
				}
			}
			return false;
		}
	}
}
