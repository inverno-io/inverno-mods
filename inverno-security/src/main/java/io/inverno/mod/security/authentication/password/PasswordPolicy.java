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

/**
 * <p>
 * A password policy is used to evaluate the strength of a password in a login credentials against specific rules.
 * </p>
 * 
 * <p>
 * The password strength returned by {@link #verify(io.inverno.mod.security.authentication.LoginCredentials, java.lang.String) } provides both a qualitative and quantitative marks that expose the
 * level of protection of a password. A {@link PasswordPolicyException} is thrown when a password does not comply with the policy and should be rejected.
 * </p>
 * 
 * <p>
 * Properly secured password policy implementations should consider <a href="https://pages.nist.gov/800-63-3/sp800-63b.html">NIST Digital Identity Guidelines Section 5.1.1.2</a>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the login credentials type
 * @param <B> the password strength type
 */
public interface PasswordPolicy<A extends LoginCredentials, B extends PasswordPolicy.PasswordStrength> {
	
	/**
	 * <p>
	 * Verifies that the specified raw password complies with the policy.
	 * </p>
	 *
	 * @param credentials the current user credentials for which the password should be defined
	 * @param rawPassword the raw password to check
	 *
	 * @return a password strength
	 *
	 * @throws PasswordPolicyException if the specified password does not comply with the password policy
	 */
	B verify(A credentials, String rawPassword) throws PasswordPolicyException;
	
	/**
	 * <p>
	 * A password strength provides both qualitative and quantitative marks to assess its level of protection against password cracking attacks.
	 * </p>
	 * 
	 * <p>
	 * It results from the evaluation of a password against a password policy (see {@link PasswordPolicy#verify(io.inverno.mod.security.authentication.LoginCredentials, java.lang.String) }).
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	interface PasswordStrength {
		
		/**
		 * <p>
		 * Represents a qualitative password protection mark.
		 * </p>
		 * 
		 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
		 * @since 1.5
		 */
		static enum Qualifier {
			/**
			 * Password offers a very weak protection against attack and should be rejected.
			 */
			VERY_WEAK,
			/**
			 * Password offers a weak protection against attack and should be rejected.
			 */
			WEAK,
			/**
			 * Password offers a medium protection against attack.
			 */
			MEDIUM,
			/**
			 * Password offers a strong protection against attack.
			 */
			STRONG,
			/**
			 * Password offers a very strong protection against attack.
			 */
			VERY_STRONG;
		}
		
		/**
		 * <p>
		 * Returns the password protection qualifier.
		 * </p>
		 * 
		 * @return a password protection qualifier
		 */
		Qualifier getQualifier();
		
		/**
		 * <p>
		 * returns the password protection score.
		 * </p>
		 * 
		 * @return a password protection score
		 */
		int getScore();
	}
}
