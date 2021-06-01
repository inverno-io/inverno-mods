/*
 * Copyright 2020 Jeremy KUHN
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
package io.inverno.mod.http.base;

/**
 * <p>
 * A web exception that indicates that a resource was {@link Status#NOT_FOUND
 * Not Found (404)} on the server.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see HttpException
 */
public class NotFoundException extends HttpException {

	private static final long serialVersionUID = 1858611479382230346L;

	/**
	 * <p>
	 * Creates a not found exception.
	 * </p>
	 */
	public NotFoundException() {
		super(Status.NOT_FOUND);
	}

	/**
	 * <p>
	 * Creates a not found exception with the specified message.
	 * </p>
	 * 
	 * @param message a message
	 */
	public NotFoundException(String message) {
		super(Status.NOT_FOUND, message);
	}

	/**
	 * <p>
	 * Creates a not found exception with the specified cause.
	 * </p>
	 * 
	 * @param cause a cause
	 */
	public NotFoundException(Throwable cause) {
		super(Status.NOT_FOUND, cause);
	}

	/**
	 * <p>
	 * Creates a not found exception with the specified message and cause.
	 * </p>
	 * 
	 * @param message a message
	 * @param cause   a cause
	 */
	public NotFoundException(String message, Throwable cause) {
		super(Status.NOT_FOUND, message, cause);
	}
}
