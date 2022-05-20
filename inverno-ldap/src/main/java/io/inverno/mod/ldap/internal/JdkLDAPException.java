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
package io.inverno.mod.ldap.internal;

import io.inverno.mod.ldap.LDAPException;
import java.text.MessageFormat;
import java.text.ParseException;
import javax.naming.NamingException;

/**
 * <p>
 * JDK based {@link LDAPException} implementation.
 * </p>
 * 
 * <p>
 * This implementation parses specific JDK error messages from {@link NamingException} to extract LDAP error code and description (i.e. {@code [LDAP: <code> - <message>}).  
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class JdkLDAPException extends LDAPException {

	private static final long serialVersionUID = 1L;
	
	/**
	 * The JDK error message format: {@code [LDAP: <code> - <message>}
	 */
	private static final MessageFormat JDK_ERROR_MESSAGE_FORMAT = new MessageFormat("[LDAP: error code {0,number,integer} - {1}]");

	/**
	 * The extracted error code.
	 */
	private Integer errorCode;
	
	/**
	 * The extracted error description.
	 */
	private String errorDescription;

	/**
	 * <p>
	 * Creates a JDK LDAP exception.
	 * </p>
	 */
	public JdkLDAPException() {
		super();
	}
	
	/**
	 * <p>
	 * Creates a JDK LDAP exception.
	 * </p>
	 * 
	 * @param message a message
	 */
	public JdkLDAPException(String message) {
		super(message);
	}

	/**
	 * <p>
	 * Creates a JDK LDAP exception.
	 * </p>
	 * 
	 * @param cause the cause of the error
	 */
	public JdkLDAPException(Throwable cause) {
		super(cause);
		this.extractCodeAndDescription(cause);
	}
	
	/**
	 * <p>
	 * Creates a JDK LDAP exception.
	 * </p>
	 * 
	 * @param message a message
	 * @param cause   the cause of the error
	 */
	public JdkLDAPException(String message, Throwable cause) {
		super(message, cause);
		this.extractCodeAndDescription(cause);
	}

	/**
	 * <p>
	 * Creates a JDK LDAP exception.
	 * </p>
	 * 
	 * @param message            a message
	 * @param cause              the cause of the error
	 * @param enableSuppression whether or not suppression is enabled or disabled
	 * @param writableStackTrace whether or not the stack trace should be writable
	 */
	public JdkLDAPException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		this.extractCodeAndDescription(cause);
	}
	
	@Override
	public Integer getErrorCode() {
		return this.errorCode;
	}
	
	@Override
	public String getErrorDescription() {
		return this.errorDescription;
	}

	/**
	 * <p>
	 * Tries to extracts and set the LDPA error code and description from the specified cause.
	 * </p>
	 * 
	 * <p>
	 * If the specified cause is not a {@link NamingException}, LDAP error code and description are left null.
	 * </p>
	 * 
	 * @param cause the cause of the error
	 */
	private void extractCodeAndDescription(Throwable cause) {
		if(cause instanceof NamingException) {
			try {
				Object[] parse = JDK_ERROR_MESSAGE_FORMAT.parse(cause.getMessage());
				
				if(parse[0] != null) {
					this.errorCode = ((Number)parse[0]).intValue();
				}
				if(parse[1] != null) {
					this.errorDescription = parse[1].toString();
				}
			} 
			catch (ParseException e) {
				// ignore
			}
		}
	}
}
