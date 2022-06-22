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
package io.inverno.mod.security.jose.jwe;

import io.inverno.mod.security.jose.JOSEProcessingException;

/**
 * <p>
 * Thrown by a {@link JWEZip} to indicate an error while compressing or decompressing data.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class JWEZipException extends JOSEProcessingException {

	private static final long serialVersionUID = 1L;

	/**
	 * <p>
	 * Creates a JWE zip exception.
	 * </p>
	 */
	public JWEZipException() {
	}

	/**
	 * <p>
	 * Creates a JWE zip exception with the specified message.
	 * </p>
	 * 
	 * @param message the message
	 */
	public JWEZipException(String message) {
		super(message);
	}
	
	/**
	 * <p>
	 * Creates a JWE zip exception with the specified cause.
	 * </p>
	 *
	 * @param cause the cause
	 */
	public JWEZipException(Throwable cause) {
		super(cause);
	}

	/**
	 * <p>
	 * Creates a JWE zip exception with the specified message and cause.
	 * </p>
	 *
	 * @param message the message
	 * @param cause   the cause
	 */
	public JWEZipException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * <p>
	 * Creates a JWE zip exception with the specified message, cause, suppression enabled or disabled and writable stack trace enabled or disabled.
	 * </p>
	 *
	 * @param message            the message
	 * @param cause              the cause
	 * @param enableSuppression  true to enable suppression, false otherwise
	 * @param writableStackTrace true to make the stack trace writable, false otherwise
	 */
	public JWEZipException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}