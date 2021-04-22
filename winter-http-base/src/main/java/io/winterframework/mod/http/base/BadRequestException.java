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
 * A web exception that indicates a {@link Status#BAD_REQUEST Bad Request (400)}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see HttpException
 */
public class BadRequestException extends HttpException {

	private static final long serialVersionUID = -6286519099962329920L;

	/**
	 * <p>
	 * Creates a bad request exception.
	 * </p>
	 */
	public BadRequestException() {
		super(Status.BAD_REQUEST);
	}

	/**
	 * <p>
	 * Creates a bad request exception with the specified message.
	 * </p>
	 * 
	 * @param message a message
	 */
	public BadRequestException(String message) {
		super(Status.BAD_REQUEST, message);
	}

	/**
	 * <p>
	 * Creates a bad request exception with the specified cause.
	 * </p>
	 * 
	 * @param cause a cause
	 */
	public BadRequestException(Throwable cause) {
		super(Status.BAD_REQUEST, cause);
	}

	/**
	 * <p>
	 * Creates a bad request exception with the specified message and cause.
	 * </p>
	 * 
	 * @param message a message
	 * @param cause   a cause
	 */
	public BadRequestException(String message, Throwable cause) {
		super(Status.BAD_REQUEST, message, cause);
	}
}
