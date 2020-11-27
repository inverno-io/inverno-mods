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
package io.winterframework.mod.web;

import java.util.Set;

/**
 * @author jkuhn
 *
 */
public class MethodNotAllowedException extends WebException {

	private static final long serialVersionUID = 3533891588820461527L;

	private Set<Method> allowedMethods;
	
	public MethodNotAllowedException(Set<Method> allowedMethods) {
		super(Status.METHOD_NOT_ALLOWED);
		this.allowedMethods = allowedMethods;
	}

	public MethodNotAllowedException(Set<Method> allowedMethods, String message) {
		super(Status.METHOD_NOT_ALLOWED, message);
		this.allowedMethods = allowedMethods;
	}

	public MethodNotAllowedException(Set<Method> allowedMethods, Throwable cause) {
		super(Status.METHOD_NOT_ALLOWED, cause);
		this.allowedMethods = allowedMethods;
	}

	public MethodNotAllowedException(Set<Method> allowedMethods, String message, Throwable cause) {
		super(Status.METHOD_NOT_ALLOWED, message, cause);
		this.allowedMethods = allowedMethods;
	}
	
	public Set<Method> getAllowedMethods() {
		return allowedMethods;
	}
}
