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

import java.util.Optional;
import java.util.Set;

/**
 * @author jkuhn
 *
 */
public class NotAcceptableException extends WebException {

	private static final long serialVersionUID = 7410917159887462383L;

	private Optional<Set<String>> acceptableMediaTypes;
	
	public NotAcceptableException() {
		super(Status.NOT_ACCEPTABLE);
	}

	public NotAcceptableException(String message) {
		super(Status.NOT_ACCEPTABLE, message);
	}

	public NotAcceptableException(Throwable cause) {
		super(Status.NOT_ACCEPTABLE, cause);
	}

	public NotAcceptableException(String message, Throwable cause) {
		super(Status.NOT_ACCEPTABLE, message, cause);
	}
	
	public NotAcceptableException(Set<String> acceptableMediaTypes) {
		super(Status.NOT_ACCEPTABLE);
		this.acceptableMediaTypes = Optional.ofNullable(acceptableMediaTypes);
	}

	public NotAcceptableException(Set<String> acceptableMediaTypes, String message) {
		super(Status.NOT_ACCEPTABLE, message);
		this.acceptableMediaTypes = Optional.ofNullable(acceptableMediaTypes);
	}

	public NotAcceptableException(Set<String> acceptableMediaTypes, Throwable cause) {
		super(Status.NOT_ACCEPTABLE, cause);
		this.acceptableMediaTypes = Optional.ofNullable(acceptableMediaTypes);
	}

	public NotAcceptableException(Set<String> acceptableMediaTypes, String message, Throwable cause) {
		super(Status.NOT_ACCEPTABLE, message, cause);
		this.acceptableMediaTypes = Optional.ofNullable(acceptableMediaTypes);
	}
	
	public Optional<Set<String>> getAcceptableMediaTypes() {
		return acceptableMediaTypes;
	}
}
