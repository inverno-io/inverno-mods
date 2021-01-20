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
package io.winterframework.mod.base.converter;

/**
 * @author jkuhn
 *
 */
public class DecoderNotFoundException extends ConverterException {

	private static final long serialVersionUID = 7872408407918829353L;

	public DecoderNotFoundException() {
	}

	public DecoderNotFoundException(String message) {
		super(message);
	}

	public DecoderNotFoundException(Throwable cause) {
		super(cause);
	}

	public DecoderNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public DecoderNotFoundException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
