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
package io.winterframework.mod.http.base.internal.header;

import io.winterframework.mod.http.base.BadRequestException;

/**
 * <p>
 * Thrown to indicates a malformed header.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
// TODO this extends BadRequestException, in case of http client we might also have malformed headers in the response as a result this might not be appropriate
public class MalformedHeaderException extends BadRequestException {

	private static final long serialVersionUID = 1780534837387915981L;

	/**
	 * <p>
	 * Creates a malformed header exception.
	 * </p>
	 */
	public MalformedHeaderException() {
	}

	/**
	 * <p>
	 * Creates a malformed header exception with the specified message.
	 * </p>
	 * 
	 * @param message the message
	 */
	public MalformedHeaderException(String message) {
		super(message);
	}

	/**
	 * <p>
	 * Creates a malformed header exception with the specified cause.
	 * </p>
	 * 
	 * @param cause the cause
	 */
	public MalformedHeaderException(Throwable cause) {
		super(cause);
	}

	/**
	 * <p>
	 * Creates a malformed header exception with the specified message and cause.
	 * </p>
	 * 
	 * @param message the message
	 * @param cause   the cause
	 */
	public MalformedHeaderException(String message, Throwable cause) {
		super(message, cause);
	}
}
