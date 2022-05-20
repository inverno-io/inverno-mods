/*
 * Copyright 2022 Jeremy Kuhn
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
package io.inverno.mod.ldap;

/**
 * <p>
 * Thrown by an {@link LDAPClient} to indicate an error wile accessing an LDAP server.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public abstract class LDAPException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * <p>
	 * Creates an LDAP exception.
	 * </p>
	 */
	public LDAPException() {
	}

	/**
	 * <p>
	 * Creates an LDAP exception.
	 * </p>
	 * 
	 * @param message a message
	 */
	public LDAPException(String message) {
		super(message);
	}

	/**
	 * <p>
	 * Creates an LDAP exception.
	 * </p>
	 * 
	 * @param cause the cause of the error
	 */
	public LDAPException(Throwable cause) {
		super(cause);
	}
	
	/**
	 * <p>
	 * Creates an LDAP exception.
	 * </p>
	 * 
	 * @param message a message
	 * @param cause   the cause of the error
	 */
	public LDAPException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * <p>
	 * Creates an LDAP exception.
	 * </p>
	 * 
	 * @param message            a message
	 * @param cause              the cause of the error
	 * @param enableSuppression whether or not suppression is enabled or disabled
	 * @param writableStackTrace whether or not the stack trace should be writable
	 */
	public LDAPException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * <p>
	 * Returns the code corresponding to the LDAP error.
	 * </p>
	 * 
	 * @return the error code
	 */
	public abstract Integer getErrorCode();
	
	/**
	 * <p>
	 * Returns the description of the LDAP error.
	 * </p>
	 * 
	 * <p>
	 * This shall corresponds to the actual error message returned by the LDAP server.
	 * </p>
	 * 
	 * @return the error description
	 */
	public abstract String getErrorDescription();
}
