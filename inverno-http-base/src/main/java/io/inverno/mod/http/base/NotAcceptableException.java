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

import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * A HTTP exception that indicates a {@link Status#NOT_ACCEPTABLE Not Acceptable (406)} client requested.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see HttpException
 */
public class NotAcceptableException extends HttpException {

	private static final long serialVersionUID = 7410917159887462383L;

	/**
	 * The list of media types accepted by the requested resource.
	 */
	private final Optional<Set<String>> acceptableMediaTypes;
	
	/**
	 * <p>
	 * Creates a not acceptable exception.
	 * </p>
	 */
	public NotAcceptableException() {
		super(Status.NOT_ACCEPTABLE);
		this.acceptableMediaTypes = Optional.empty();
	}

	/**
	 * <p>
	 * Creates a not acceptable exception with the specified message.
	 * </p>
	 * 
	 * @param message a message
	 */
	public NotAcceptableException(String message) {
		super(Status.NOT_ACCEPTABLE, message);
		this.acceptableMediaTypes = Optional.empty();
	}

	/**
	 * <p>
	 * Creates a not acceptable exception with the specified cause.
	 * </p>
	 * 
	 * @param cause a cause
	 */
	public NotAcceptableException(Throwable cause) {
		super(Status.NOT_ACCEPTABLE, cause);
		this.acceptableMediaTypes = Optional.empty();
	}

	/**
	 * <p>
	 * Creates a not acceptable exception with the specified message and cause.
	 * </p>
	 * 
	 * @param message a message
	 * @param cause   a cause
	 */
	public NotAcceptableException(String message, Throwable cause) {
		super(Status.NOT_ACCEPTABLE, message, cause);
		this.acceptableMediaTypes = Optional.empty();
	}
	
	/**
	 * <p>
	 * Creates a not acceptable exception with the specified list of media types accepted by the requested resource.
	 * </p>
	 * 
	 * @param acceptableMediaTypes a list of acceptable media types
	 */
	public NotAcceptableException(Set<String> acceptableMediaTypes) {
		super(Status.NOT_ACCEPTABLE);
		this.acceptableMediaTypes = Optional.ofNullable(acceptableMediaTypes);
	}

	/**
	 * <p>
	 * Creates a not acceptable exception with the specified list of media types accepted by the requested resource and message.
	 * </p>
	 * 
	 * @param acceptableMediaTypes a list of acceptable media types
	 * @param message              a message
	 */
	public NotAcceptableException(Set<String> acceptableMediaTypes, String message) {
		super(Status.NOT_ACCEPTABLE, message);
		this.acceptableMediaTypes = Optional.ofNullable(acceptableMediaTypes);
	}

	/**
	 * <p>
	 * Creates a not acceptable exception with the specified list of media types accepted by the requested resource and cause.
	 * </p>
	 * 
	 * @param acceptableMediaTypes a list of acceptable media types
	 * @param cause                a cause
	 */
	public NotAcceptableException(Set<String> acceptableMediaTypes, Throwable cause) {
		super(Status.NOT_ACCEPTABLE, cause);
		this.acceptableMediaTypes = Optional.ofNullable(acceptableMediaTypes);
	}

	/**
	 * <p>
	 * Creates a not acceptable exception with the specified list of media types accepted by the requested resource, message and cause.
	 * </p>
	 * 
	 * @param acceptableMediaTypes a list of acceptable media types
	 * @param message              a message
	 * @param cause                a cause
	 */
	public NotAcceptableException(Set<String> acceptableMediaTypes, String message, Throwable cause) {
		super(Status.NOT_ACCEPTABLE, message, cause);
		this.acceptableMediaTypes = Optional.ofNullable(acceptableMediaTypes);
	}
	
	/**
	 * <p>
	 * Returns the list of media types accepted by the requested resource.
	 * </p>
	 * 
	 * @return the list of acceptable media types
	 */
	public Optional<Set<String>> getAcceptableMediaTypes() {
		return acceptableMediaTypes;
	}
}
