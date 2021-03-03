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
package io.winterframework.mod.web;

import io.winterframework.mod.http.base.BadRequestException;

/**
 * @author jkuhn
 *
 */
public class MissingRequiredParameterException extends BadRequestException {

	private static final long serialVersionUID = -1193466181199257037L;

	private String parameterName;
	
	public MissingRequiredParameterException() {
	}

	public MissingRequiredParameterException(String parameterName) {
		super("Missing required parameter: " + parameterName);
		this.parameterName = parameterName;
	}
	
	public MissingRequiredParameterException(Throwable cause) {
		super(cause);
	}

	public MissingRequiredParameterException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public String getParameterName() {
		return parameterName;
	}
}
