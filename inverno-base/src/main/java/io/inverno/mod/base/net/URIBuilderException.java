/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.base.net;

/**
 * <p>
 * Thrown by a {@link URIBuilder} to indicate an error when building a URI.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see URIBuilder
 */
public class URIBuilderException extends RuntimeException {

	private static final long serialVersionUID = 5532461848243878778L;

	/**
	 * <p>
	 * Creates a URI builder exception.
	 * </p>
	 */
	public URIBuilderException() {
	}

	/**
	 * <p>
	 * Creates a URI builder exception with the specified message.
	 * </p>
	 * 
	 * @param message the message
	 */
	public URIBuilderException(String message) {
		super(message);
	}

	/**
	 * <p>
	 * Creates a URI builder exception with the specified cause.
	 * </p>
	 * 
	 * @param cause the cause
	 */
	public URIBuilderException(Throwable cause) {
		super(cause);
	}

	/**
	 * <p>
	 * Creates a URI builder exception with the specified message and cause.
	 * </p>
	 * 
	 * @param message the message
	 * @param cause   the cause
	 */
	public URIBuilderException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * <p>
	 * Creates a URI builder exception with the specified message, cause, suppression enabled or disabled and writable stack trace enabled or disabled.
	 * </p>
	 * 
	 * @param message            the message
	 * @param cause              the cause
	 * @param enableSuppression  true to enable suppression, false otherwise
	 * @param writableStackTrace true to make the stack trace writable, false otherwise
	 */
	public URIBuilderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
