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
package io.inverno.mod.http.client.internal;

import io.inverno.mod.http.client.HttpClientException;

/**
 * <p>
 * Thrown by the {@link PooledEndpoint} to indicate an error in the pool.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class ConnectionPoolException extends HttpClientException {

	private static final long serialVersionUID = 1L;

	/**
	 * <p>
	 * Creates a Connection pool exception.
	 * </p>
	 */
	public ConnectionPoolException() {
	}

	/**
	 * <p>
	 * Creates a Connection pool exception.
	 * </p>
	 * 
	 * @param message the message
	 */
	public ConnectionPoolException(String message) {
		super(message);
	}

	/**
	 * <p>
	 * Creates a Connection pool exception.
	 * </p>
	 *
	 * @param message the message
	 * @param cause   the cause
	 */
	public ConnectionPoolException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * <p>
	 * Creates a Connection pool exception.
	 * </p>
	 * 
	 * @param cause the cause
	 */
	public ConnectionPoolException(Throwable cause) {
		super(cause);
	}

	/**
	 * <p>
	 * Creates a Connection pool exception.
	 * </p>
	 * 
	 * @param message            the message
	 * @param cause              the cause 
	 * @param enableSuppression
	 * @param writableStackTrace 
	 */
	public ConnectionPoolException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
