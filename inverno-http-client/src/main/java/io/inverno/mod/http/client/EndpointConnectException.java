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
 * Thrown when an endpoint couldn't establish a connection to a server.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class EndpointConnectException extends HttpClientException {

	private static final long serialVersionUID = 1L;
	
	/**
	 * <p>
	 * Creates an endpoint connect exception.
	 * </p>
	 */
	public EndpointConnectException() {
	}

	/**
	 * <p>
	 * Creates an endpoint connect exception.
	 * </p>
	 * 
	 * @param message the message
	 */
	public EndpointConnectException(String message) {
		super(message);
	}

	/**
	 * <p>
	 * Creates an endpoint connect exception.
	 * </p>
	 * 
	 * @param message the message
	 * @param cause   the cause
	 */
	public EndpointConnectException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * <p>
	 * Creates an endpoint connect exception.
	 * </p>
	 * 
	 * @param cause the cause
	 */
	public EndpointConnectException(Throwable cause) {
		super(cause);
	}
}
