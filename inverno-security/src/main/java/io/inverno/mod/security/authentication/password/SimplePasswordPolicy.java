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

import io.inverno.mod.security.authentication.LoginCredentials;
import java.util.Objects;

/**
 * <p>
 * A simple password policy that simply checks for password's length.
 * </p>
 * 
 * <p>
 * Following latest <a href="https://pages.nist.gov/800-63-3/sp800-63b.html">NIST Digital Identity Guidelines Section 5.1.1.2</a>, a password should be at least 8 characters and at most 64 characters
 * long. Please refer to these guidelines in order to build more robust password policies (dictionary words, repetitive or sequential characters, context-specific words...).
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the login credentials type
 */
public class SimplePasswordPolicy<A extends LoginCredentials> implements PasswordPolicy<A, SimplePasswordPolicy.SimplePasswordStrength> {
	
	/**
	 * The default minimum password length.
	 */
	public static final int DEFAULT_MINIMUM_PASSWORD_LENGTH = 8;

	/**
	 * The default maximum password length.
	 */
	public static final int DEFAULT_MAXIMUM_PASSWORD_LENGTH = 64;

	/**
	 * The minimum password length.
	 */
	private final int minimumPasswordLength;
	
	/**
	 * The maximum password length.
	 */
	private final int maximumPasswordLength;
	
	/**
	 * <p>
	 * Creates a default simple password policy.
	 * </p>
	 */
	public SimplePasswordPolicy() {
		this(DEFAULT_MINIMUM_PASSWORD_LENGTH, DEFAULT_MAXIMUM_PASSWORD_LENGTH);
	}
	
	/**
	 * <p>
	 * Creates a simple password policy with the specified password lengths.
	 * </p>
	 * 
	 * @param minimumPasswordLength the minimum password length
	 * @param maximumPasswordLength the maximum password length
	 * 
	 * @throws IllegalArgumentException if specified parameters are incorrect
	 */
	public SimplePasswordPolicy(int minimumPasswordLength, int maximumPasswordLength) throws IllegalArgumentException {
		if(minimumPasswordLength <= 0) {
			throw new IllegalArgumentException("Minimum password length must be strictly positive");
		}
		if(maximumPasswordLength < minimumPasswordLength) {
			throw new IllegalArgumentException("Maximum password length must greater than minimum password length");
		}
		this.minimumPasswordLength = minimumPasswordLength;
		this.maximumPasswordLength = maximumPasswordLength;
	}

	@Override
	public SimplePasswordStrength verify(A credentials, String rawPassword) throws PasswordPolicyException {
		SimplePasswordStrength strength = new SimplePasswordStrength(Objects.requireNonNull(rawPassword).length());
		
		if(strength.getScore() < this.minimumPasswordLength) {
			throw new PasswordPolicyException(strength, "Password must be at least " + this.minimumPasswordLength + " characters long");
		}
		else if(strength.getScore() > this.maximumPasswordLength) {
			throw new PasswordPolicyException(strength, "Password must be at most " + this.maximumPasswordLength + " characters long");
		}
		
		return strength;
	}

	/**
	 * <p>
	 * Returns the minimum password length.
	 * </p>
	 * 
	 * @return the minimum password length
	 */
	public int getMinimumPasswordLength() {
		return minimumPasswordLength;
	}

	/**
	 * <p>
	 * Returns the maximum password length.
	 * </p>
	 * 
	 * @return the minimum password length
	 */
	public int getMaximumPasswordLength() {
		return maximumPasswordLength;
	}
	
	/**
	 * <p>
	 * A simple password strength implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	public static class SimplePasswordStrength implements PasswordPolicy.PasswordStrength {

		/**
		 * The password protection score.
		 */
		private final int score;
		
		/**
		 * The password protection qualifier.
		 */
		private final PasswordPolicy.PasswordStrength.Qualifier qualifier;
		
		/**
		 * <p>
		 * Creates a simple password strength from the specified score which corresponds to the length of the password.
		 * </p>
		 * 
		 * @param score the length of the evaluated password
		 */
		public SimplePasswordStrength(int score) {
			this.score = score;
			if(score < 5) {
				this.qualifier = Qualifier.VERY_WEAK;
			}
			else if(score < 7) {
				this.qualifier = Qualifier.WEAK;
			}
			else if(score < 10) {
				this.qualifier = Qualifier.STRONG;
			}
			else {
				this.qualifier = Qualifier.VERY_STRONG;
			}
		}
		
		@Override
		public Qualifier getQualifier() {
			return this.qualifier;
		}

		@Override
		public int getScore() {
			return this.score;
		}
	}
}
