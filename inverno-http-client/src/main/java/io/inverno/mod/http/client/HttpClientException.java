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
 * Base exception class used to report HTTP client errors.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class HttpClientException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * <p>
	 * Creates an HTTP client exception.
	 * </p>
	 */
	public HttpClientException() {
	}

	/**
	 * <p>
	 * Creates a HTTP client exception.
	 * </p>
	 * 
	 * @param message the message
	 */
	public HttpClientException(String message) {
		super(message);
	}

	/**
	 * <p>
	 * Creates a HTTP client exception.
	 * </p>
	 * 
	 * @param message the message
	 * @param cause   the cause
	 */
	public HttpClientException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * <p>
	 * Creates a HTTP client exception.
	 * </p>
	 * 
	 * @param cause the cause
	 */
	public HttpClientException(Throwable cause) {
		super(cause);
	}
}
