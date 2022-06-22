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
package io.inverno.mod.security.http.digest;

import io.inverno.mod.security.authentication.AuthenticationException;

/**
 * <p>
 * Thrown by a {@link DigestCredentialsMatcher} to indicate that the provided nonce has expired.
 * </p>
 * 
 * <p>
 * This exception should be intercepted by a {@link DigestAuthenticationErrorInterceptor} to send with a stale response to the client as defined by
 * <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.3">RFC 7616 section 3.3</a>.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class ExpiredNonceException extends AuthenticationException {

	private static final long serialVersionUID = 1L;

	/**
	 * <p>
	 * Creates an expired nonce exception.
	 * </p>
	 */
	public ExpiredNonceException() {
	}

	/**
	 * <p>
	 * Creates an expired nonce exception with the specified message.
	 * </p>
	 * 
	 * @param message the message
	 */
	public ExpiredNonceException(String message) {
		super(message);
	}
	
	/**
	 * <p>
	 * Creates an expired nonce exception with the specified cause.
	 * </p>
	 *
	 * @param cause the cause
	 */
	public ExpiredNonceException(Throwable cause) {
		super(cause);
	}

	/**
	 * <p>
	 * Creates an expired nonce exception with the specified message and cause.
	 * </p>
	 *
	 * @param message the message
	 * @param cause   the cause
	 */
	public ExpiredNonceException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * <p>
	 * Creates an expired nonce exception with the specified message, cause, suppression enabled or disabled and writable stack trace enabled or disabled.
	 * </p>
	 *
	 * @param message            the message
	 * @param cause              the cause
	 * @param enableSuppression  true to enable suppression, false otherwise
	 * @param writableStackTrace true to make the stack trace writable, false otherwise
	 */
	public ExpiredNonceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
