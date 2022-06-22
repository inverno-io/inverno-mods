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
 * A HTTP exception that indicates that the access to a server resource is {@link Status#UNAUTHORIZED Unauthorized (401)}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see HttpException
 */
public class UnauthorizedException extends HttpException {

	private static final long serialVersionUID = 2146912456392459200L;

	/**
	 * <p>
	 * Creates an unauthorized exception.
	 * </p>
	 */
	public UnauthorizedException() {
		super(Status.UNAUTHORIZED);
	}

	/**
	 * <p>
	 * Creates an unauthorized exception with the specified message.
	 * </p>
	 * 
	 * @param message a message
	 */
	public UnauthorizedException(String message) {
		super(Status.UNAUTHORIZED, message);
	}

	/**
	 * <p>
	 * Creates an unauthorized exception with the specified cause.
	 * </p>
	 * 
	 * @param cause a cause
	 */
	public UnauthorizedException(Throwable cause) {
		super(Status.UNAUTHORIZED, cause);
	}

	/**
	 * <p>
	 * Creates an unauthorized exception with the specified message and cause.
	 * </p>
	 * 
	 * @param message a message
	 * @param cause   a cause
	 */
	public UnauthorizedException(String message, Throwable cause) {
		super(Status.UNAUTHORIZED, message, cause);
	}
}
