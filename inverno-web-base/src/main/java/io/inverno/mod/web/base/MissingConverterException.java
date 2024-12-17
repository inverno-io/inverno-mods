/*
 * Copyright 2024 Jeremy KUHN
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
package io.inverno.mod.web.base;

/**
 * <p>
 * Thrown to indicate that no converter was found for a specific media type.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class MissingConverterException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * The media type for which there's no converter.
	 */
	private final String mediaType;

	/**
	 * <p>
	 * Creates a missing converter exception to report a null media type.
	 * </p>
	 */
	public MissingConverterException() {
		this.mediaType = null;
	}

	/**
	 * <p>
	 * Creates a missing converter exception with the specified media type.
	 * </p>
	 *
	 * @param mediaType The media type
	 */
	public MissingConverterException(String mediaType) {
		this.mediaType = mediaType;
	}

	/**
	 * <p>
	 * Creates a missing converter exception with the specified media type and message.
	 * </p>
	 *
	 * @param mediaType the media type
	 * @param message a message
	 */
	public MissingConverterException(String mediaType, String message) {
		super(message);
		this.mediaType = mediaType;
	}

	/**
	 * <p>
	 * Creates a missing converter exception with the specified media type and cause.
	 * </p>
	 *
	 * @param mediaType the media type
	 * @param cause the cause
	 */
	public MissingConverterException(String mediaType, Throwable cause) {
		super(cause);
		this.mediaType = mediaType;
	}

	/**
	 * <p>
	 * Creates a missing converter exception with the specified media type, message and cause.
	 * </p>
	 *
	 * @param mediaType the media type
	 * @param message a message
	 * @param cause the cause
	 */
	public MissingConverterException(String mediaType, String message, Throwable cause) {
		super(message, cause);
		this.mediaType = mediaType;
	}

	/**
	 * <p>
	 * Returns the media type.
	 * </p>
	 *
	 * @return the media type or null
	 */
	public String getMediaType() {
		return mediaType;
	}
}
