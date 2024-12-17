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
package io.inverno.mod.http.base.ws;

import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.Status;

/**
 * <p>
 * Thrown to indicate an error while processing a WebSocket exchange.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class WebSocketException extends HttpException {

	private static final long serialVersionUID = 1L;

	/**
	 * <p>
	 * Creates a WebSocket exception with default status {@link Status#INTERNAL_SERVER_ERROR Internal Server Error (500)}.
	 * </p>
	 */
	public WebSocketException() {
	}

	/**
	 * <p>
	 * Creates a WebSocket exception with default status {@link Status#INTERNAL_SERVER_ERROR Internal Server Error (500)} and specified message.
	 * </p>
	 *
	 * @param message a message
	 */
	public WebSocketException(String message) {
		super(message);
	}

	/**
	 * <p>
	 * Creates a WebSocket exception with default status {@link Status#INTERNAL_SERVER_ERROR Internal Server Error (500)} and specified cause.
	 * </p>
	 *
	 * @param cause a cause
	 */
	public WebSocketException(Throwable cause) {
		super(cause);
	}

	/**
	 * <p>
	 * Creates a WebSocket exception with default status {@link Status#INTERNAL_SERVER_ERROR Internal Server Error (500)}, specified message and cause
	 * </p>
	 *
	 * @param message a message
	 * @param cause   a cause
	 */
	public WebSocketException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * <p>
	 * Creates a WebSocket exception with specified HTTP status code.
	 * </p>
	 *
	 * @param statusCode an HTTP status code
	 *
	 * @throws IllegalArgumentException if the specified status doesn't correspond to a known HTTP status
	 */
	public WebSocketException(int statusCode) throws IllegalArgumentException {
		super(statusCode);
	}

	/**
	 * <p>
	 * Creates a WebSocket exception with specified HTTP status code and message.
	 * </p>
	 *
	 * @param statusCode an HTTP status code
	 * @param message    a message
	 *
	 * @throws IllegalArgumentException if the specified status doesn't correspond to a known HTTP status
	 */
	public WebSocketException(int statusCode, String message) throws IllegalArgumentException {
		super(statusCode, message);
	}

	/**
	 * <p>
	 * Creates a WebSocket exception with specified HTTP status code and cause.
	 * </p>
	 *
	 * @param statusCode an HTTP status code
	 * @param cause      a cause
	 *
	 * @throws IllegalArgumentException if the specified status doesn't correspond to a known HTTP status
	 */
	public WebSocketException(int statusCode, Throwable cause) throws IllegalArgumentException {
		super(statusCode, cause);
	}

	/**
	 * <p>
	 * Creates a WebSocket exception with specified HTTP status code, message and cause.
	 * </p>
	 *
	 * @param statusCode an HTTP status code
	 * @param message    a message
	 * @param cause      a cause
	 *
	 * @throws IllegalArgumentException if the specified status doesn't correspond to a known HTTP status
	 */
	public WebSocketException(int statusCode, String message, Throwable cause) throws IllegalArgumentException {
		super(statusCode, message, cause);
	}

	/**
	 * <p>
	 * Creates a WebSocket exception with specified HTTP status.
	 * </p>
	 *
	 * @param status an HTTP status
	 */
	public WebSocketException(Status status) {
		super(status);
	}

	/**
	 * <p>
	 * Creates a WebSocket exception with specified HTTP status and message.
	 * </p>
	 *
	 * @param status  an HTTP status
	 * @param message a message
	 */
	public WebSocketException(Status status, String message) {
		super(status, message);
	}

	/**
	 * <p>
	 * Creates a WebSocket exception with specified HTTP status and cause.
	 * </p>
	 * 
	 * @param status an HTTP status
	 * @param cause  a cause
	 */
	public WebSocketException(Status status, Throwable cause) {
		super(status, cause);
	}

	/**
	 * <p>
	 * Creates a WebSocket exception with specified HTTP status, message and cause.
	 * </p>
	 * 
	 * @param status  an HTTP status
	 * @param message a message
	 * @param cause   a cause
	 */
	public WebSocketException(Status status, String message, Throwable cause) {
		super(status, message, cause);
	}
}
