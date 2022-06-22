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

import java.util.Set;

/**
 * <p>
 * A HTTP exception that indicates a client requested {@link Status#METHOD_NOT_ALLOWED Method Not Allowed (405)}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see HttpException
 */
public class MethodNotAllowedException extends HttpException {

	private static final long serialVersionUID = 3533891588820461527L;

	/**
	 * The list of methods allowed by the requested resource.
	 */
	private Set<Method> allowedMethods;
	
	/**
	 * <p>
	 * Creates a method not allowed exception with the specified list of methods
	 * allowed by the requested resource.
	 * </p>
	 * 
	 * @param allowedMethods a list of allowed methods
	 */
	public MethodNotAllowedException(Set<Method> allowedMethods) {
		super(Status.METHOD_NOT_ALLOWED);
		this.allowedMethods = allowedMethods;
	}

	/**
	 * <p>
	 * Creates a method not allowed exception with the specified list of methods
	 * allowed by the requested resource and message.
	 * </p>
	 * 
	 * @param allowedMethods a list of allowed methods
	 * @param message        a message
	 */
	public MethodNotAllowedException(Set<Method> allowedMethods, String message) {
		super(Status.METHOD_NOT_ALLOWED, message);
		this.allowedMethods = allowedMethods;
	}

	/**
	 * <p>
	 * Creates a method not allowed exception with the specified list of methods
	 * allowed by the requested resource and cause.
	 * </p>
	 * 
	 * @param allowedMethods a list of allowed methods
	 * @param cause          a cause
	 */
	public MethodNotAllowedException(Set<Method> allowedMethods, Throwable cause) {
		super(Status.METHOD_NOT_ALLOWED, cause);
		this.allowedMethods = allowedMethods;
	}

	/**
	 * <p>
	 * Creates a method not allowed exception with the specified list of methods
	 * allowed by the requested resource, message and cause.
	 * </p>
	 * 
	 * @param allowedMethods a list of allowed methods
	 * @param message        a message
	 * @param cause          a cause
	 */
	public MethodNotAllowedException(Set<Method> allowedMethods, String message, Throwable cause) {
		super(Status.METHOD_NOT_ALLOWED, message, cause);
		this.allowedMethods = allowedMethods;
	}
	
	/**
	 * <p>
	 * Returns the list of methods allowed by the requested resource.
	 * </p>
	 * 
	 * @return the list of allowed methods
	 */
	public Set<Method> getAllowedMethods() {
		return allowedMethods;
	}
}
