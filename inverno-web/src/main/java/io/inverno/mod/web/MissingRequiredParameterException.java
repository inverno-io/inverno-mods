/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.web;

import io.inverno.mod.http.base.BadRequestException;

/**
 * <p>
 * Thrown to indicates that a required parameter is missing to process the
 * request.
 * </p>
 * 
 * <p>
 * The missing parameter could be any kind of request parameters: a query
 * parameter, a header, a cookie, path parameter...
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class MissingRequiredParameterException extends BadRequestException {

	private static final long serialVersionUID = -1193466181199257037L;

	/**
	 * The name of the missing parameter
	 */
	private String parameterName;
	
	/**
	 * <p>
	 * Creates a missing required parameter exception.
	 * </p>
	 */
	public MissingRequiredParameterException() {
	}
	
	/**
	 * <p>
	 * Creates a missing required parameter exception for the specified parameter
	 * name.
	 * </p>
	 * 
	 * @param parameterName the name of the missing parameter
	 */
	public MissingRequiredParameterException(String parameterName) {
		super("Missing required parameter: " + parameterName);
		this.parameterName = parameterName;
	}
	
	/**
	 * <p>
	 * Creates a missing required parameter exception with the specified cause.
	 * </p>
	 * 
	 * @param cause the cause
	 */
	public MissingRequiredParameterException(Throwable cause) {
		super(cause);
	}

	/**
	 * <p>
	 * Creates a missing required parameter exception with the specified parameter
	 * name and cause.
	 * </p>
	 * 
	 * @param parameterName the name of the missing parameter
	 * @param cause         the cause
	 */
	public MissingRequiredParameterException(String parameterName, Throwable cause) {
		super("Missing required parameter: " + parameterName, cause);
	}
	
	/**
	 * <p>
	 * Returns the name of the missing parameter.
	 * </p>
	 * 
	 * @return the missing parameter name or null
	 */
	public String getParameterName() {
		return parameterName;
	}
}
