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

/**
 * <p>
 * Thrown to indicate that a password does not comply with a password policy.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class PasswordPolicyException extends io.inverno.mod.security.SecurityException {

	private static final long serialVersionUID = 1L;
	
	/**
	 * The password strength.
	 */
	private final PasswordPolicy.PasswordStrength passwordStrength;

	/**
	 * <p>
	 * Creates a password policy exception with the specified strength.
	 * </p>
	 * 
	 * @param passwordStrength the password strength of the non-compliant password
	 */
	public PasswordPolicyException(PasswordPolicy.PasswordStrength passwordStrength) {
		this.passwordStrength = passwordStrength;
	}

	/**
	 * <p>
	 * Creates a password exception with the specified password strength and message.
	 * </p>
	 * 
	 * @param passwordStrength the password strength of the non-compliant password
	 * @param message the message
	 */
	public PasswordPolicyException(PasswordPolicy.PasswordStrength passwordStrength, String message) {
		super(message);
		this.passwordStrength = passwordStrength;
	}

	/**
	 * <p>
	 * Creates a password exception with the specified password strength and cause.
	 * </p>
	 *
	 * @param passwordStrength the password strength of the non-compliant password
	 * @param cause the cause
	 */
	public PasswordPolicyException(PasswordPolicy.PasswordStrength passwordStrength, Throwable cause) {
		super(cause);
		this.passwordStrength = passwordStrength;
	}
	
	/**
	 * <p>
	 * Creates a password exception with the specified password strength, message and cause.
	 * </p>
	 *
	 * @param passwordStrength the password strength of the non-compliant password
	 * @param message the message
	 * @param cause   the cause
	 */
	public PasswordPolicyException(PasswordPolicy.PasswordStrength passwordStrength, String message, Throwable cause) {
		super(message, cause);
		this.passwordStrength = passwordStrength;
	}

	/**
	 * <p>
	 * Creates a password exception with the specified password strength, message, cause, suppression enabled or disabled and writable stack trace enabled or disabled.
	 * </p>
	 *
	 * @param passwordStrength the password strength of the non-compliant password
	 * @param message            the message
	 * @param cause              the cause
	 * @param enableSuppression  true to enable suppression, false otherwise
	 * @param writableStackTrace true to make the stack trace writable, false otherwise
	 */
	public PasswordPolicyException(PasswordPolicy.PasswordStrength passwordStrength, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		this.passwordStrength = passwordStrength;
	}

	/**
	 * <p>
	 * Returns the strength of the non-compliant password.
	 * </p>
	 * 
	 * @return the password strength
	 */
	public PasswordPolicy.PasswordStrength getPasswordStrength() {
		return passwordStrength;
	}
}
