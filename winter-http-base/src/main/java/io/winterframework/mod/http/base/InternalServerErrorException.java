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
package io.winterframework.mod.http.base;

/**
 * <p>
 * A web exception that indicates an {@link Status#INTERNAL_SERVER_ERROR Internal Server Error (500)}
 * accessing a server resource.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see HttpException
 */
public class InternalServerErrorException extends HttpException {

	private static final long serialVersionUID = 1297738299570958017L;

	/**
	 * <p>
	 * Creates an internal server error exception.
	 * </p>
	 */
	public InternalServerErrorException() {
		super(Status.INTERNAL_SERVER_ERROR);
	}

	/**
	 * <p>
	 * Creates an internal server error exception with the specified message.
	 * </p>
	 * 
	 * @param message a message
	 */
	public InternalServerErrorException(String message) {
		super(Status.INTERNAL_SERVER_ERROR, message);
	}

	/**
	 * <p>
	 * Creates an internal server error exception with the specified cause.
	 * </p>
	 * 
	 * @param cause a cause
	 */
	public InternalServerErrorException(Throwable cause) {
		super(Status.INTERNAL_SERVER_ERROR, cause);
	}

	/**
	 * <p>
	 * Creates an internal server error exception with the specified message and
	 * cause.
	 * </p>
	 * 
	 * @param message a message
	 * @param cause   a cause
	 */
	public InternalServerErrorException(String message, Throwable cause) {
		super(Status.INTERNAL_SERVER_ERROR, message, cause);
	}
}
