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
package io.winterframework.mod.commons.converter;

/**
 * @author jkuhn
 *
 */
public class ValueCodecException extends RuntimeException {

	private static final long serialVersionUID = -6804673345487857625L;

	public ValueCodecException() {
		super();
	}

	public ValueCodecException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ValueCodecException(String message, Throwable cause) {
		super(message, cause);
	}

	public ValueCodecException(String message) {
		super(message);
	}

	public ValueCodecException(Throwable cause) {
		super(cause);
	}
}
