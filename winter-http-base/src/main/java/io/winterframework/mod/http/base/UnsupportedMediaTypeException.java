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
 * A web exception that indicates that the request body if of an
 * {@link Status#UNSUPPORTED_MEDIA_TYPE Unsupported Media Type (415)} for the
 * requested resource.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see HttpException
 */
public class UnsupportedMediaTypeException extends HttpException {

	private static final long serialVersionUID = 5153779471415173515L;

	/**
	 * <p>
	 * Creates an unsupported media type exception.
	 * </p>
	 */
	public UnsupportedMediaTypeException() {
		super(Status.UNSUPPORTED_MEDIA_TYPE);
	}

	/**
	 * <p>
	 * Creates an unsupported media type exception with the specified message.
	 * </p>
	 * 
	 * @param message a message
	 */
	public UnsupportedMediaTypeException(String message) {
		super(Status.UNSUPPORTED_MEDIA_TYPE, message);
	}

	/**
	 * <p>
	 * Creates an unsupported media type exception with the specified cause.
	 * </p>
	 * 
	 * @param cause a cause
	 */
	public UnsupportedMediaTypeException(Throwable cause) {
		super(Status.UNSUPPORTED_MEDIA_TYPE, cause);
	}

	/**
	 * <p>
	 * Creates an unsupported media type exception with the specified message and
	 * cause.
	 * </p>
	 * 
	 * @param message a message
	 * @param cause   a cause
	 */
	public UnsupportedMediaTypeException(String message, Throwable cause) {
		super(Status.UNSUPPORTED_MEDIA_TYPE, message, cause);
	}
}
