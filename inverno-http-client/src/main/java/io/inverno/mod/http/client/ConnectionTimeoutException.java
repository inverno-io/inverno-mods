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
package io.inverno.mod.http.client;

/**
 * <p>
 * Thrown when a connection has timed out.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @see HttpClientConfiguration
 */
public class ConnectionTimeoutException extends HttpClientException {

	private static final long serialVersionUID = 1L;

	/**
	 * <p>
	 * Creates a Connection timeout exception.
	 * </p>
	 */
	public ConnectionTimeoutException() {
	}

	/**
	 * <p>
	 * Creates a Connection timeout exception.
	 * </p>
	 * 
	 * @param message the message
	 */
	public ConnectionTimeoutException(String message) {
		super(message);
	}

	/**
	 * <p>
	 * Creates a Connection timeout exception.
	 * </p>
	 * 
	 * @param message the message
	 * @param cause   the cause
	 */
	public ConnectionTimeoutException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * <p>
	 * Creates a Connection timeout exception.
	 * </p>
	 * 
	 * @param cause the cause
	 */
	public ConnectionTimeoutException(Throwable cause) {
		super(cause);
	}

	/**
	 * <p>
	 * Creates a Connection timeout exception.
	 * </p>
	 * 
	 * @param message            the message
	 * @param cause              the cause
	 * @param enableSuppression 
	 * @param writableStackTrace 
	 */
	public ConnectionTimeoutException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
