/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.http.server.internal;

import io.inverno.mod.http.server.HttpServerException;

/**
 * <p>
 * Stackless HTTP server exception.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.11
 */
public class StacklessHttpServerException extends HttpServerException {

	private static final long serialVersionUID = 1L;

	/**
	 * <p>
	 * Creates a stackless HTTP server exception.
	 * </p>
	 */
	public StacklessHttpServerException() {
	}

	/**
	 * <p>
	 * Creates a stackless HTTP server exception.
	 * </p>
	 * 
	 * @param message the message
	 */
	public StacklessHttpServerException(String message) {
		super(message);
	}

	/**
	 * <p>
	 * Creates an HTTP server exception.
	 * </p>
	 * 
	 * @param message the message
	 * @param cause   the cause
	 */
	public StacklessHttpServerException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * <p>
	 * Creates a stackless HTTP server exception.
	 * </p>
	 * 
	 * @param cause the cause
	 */
	public StacklessHttpServerException(Throwable cause) {
		super(cause);
	}

	@Override
	public synchronized Throwable fillInStackTrace() {
		return this;
	}
}
