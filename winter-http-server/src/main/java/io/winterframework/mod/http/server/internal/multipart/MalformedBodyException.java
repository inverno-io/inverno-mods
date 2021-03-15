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
package io.winterframework.mod.http.server.internal.multipart;

import io.winterframework.mod.http.base.BadRequestException;

/**
 * <p>
 * Thrown to indicate a malformed multipart body.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class MalformedBodyException extends BadRequestException {

	private static final long serialVersionUID = 1083960865494661237L;

	public MalformedBodyException() {
	}

	public MalformedBodyException(String message) {
		super(message);
	}

	public MalformedBodyException(Throwable cause) {
		super(cause);
	}

	public MalformedBodyException(String message, Throwable cause) {
		super(message, cause);
	}
}
