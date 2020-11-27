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

/**
 * @author jkuhn
 *
 */
public class UnsupportedMediaTypeException extends WebException {

	private static final long serialVersionUID = 5153779471415173515L;

	public UnsupportedMediaTypeException() {
		super(Status.UNSUPPORTED_MEDIA_TYPE);
	}

	public UnsupportedMediaTypeException(String message) {
		super(Status.UNSUPPORTED_MEDIA_TYPE, message);
	}

	public UnsupportedMediaTypeException(Throwable cause) {
		super(Status.UNSUPPORTED_MEDIA_TYPE, cause);
	}

	public UnsupportedMediaTypeException(String message, Throwable cause) {
		super(Status.UNSUPPORTED_MEDIA_TYPE, message, cause);
	}
}
