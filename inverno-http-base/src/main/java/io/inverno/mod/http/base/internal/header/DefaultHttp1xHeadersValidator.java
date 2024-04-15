/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.http.base.internal.header;

import io.netty.handler.codec.http.HttpHeaderValidationUtil;

/**
 * <p>
 * Default HTTP/1.x {@link HeadersValidator} implementation.
 * </p>
 * 
 * <p>
 * Name and value validation is borrowed from Netty's {@link DefaultHttpHeadersFactory#DEFAULT_NAME_VALIDATOR} and {@link DefaultHttpHeadersFactory#DEFAULT_NAME_VALIDATOR}.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
class DefaultHttp1xHeadersValidator implements HeadersValidator {

	@Override
	public void accept(CharSequence name, CharSequence value) {
		validateName(name);
		if(value != null) {
			this.validateValue(value);
		}
	}

	/**
	 * <p>
	 * Borrowed from {@link DefaultHttpHeadersFactory#DEFAULT_NAME_VALIDATOR}.
	 * </p>
	 * 
	 * @param name the header name
	 */
	private static void validateName(CharSequence name) {
		if (name == null || name.length() == 0) {
			throw new IllegalArgumentException("empty headers are not allowed [" + name + ']');
		}
		int index = HttpHeaderValidationUtil.validateToken(name);
		if (index != -1) {
			throw new IllegalArgumentException("a header name can only contain \"token\" characters, " + "but found invalid character 0x" + Integer.toHexString(name.charAt(index)) 
				+ " at index " + index + " of header '" + name + "'.");
		}
	}

	/**
	 * <p>
	 * Borrowed from {@link DefaultHttpHeadersFactory#DEFAULT_NAME_VALIDATOR}.
	 * </p>
	 * 
	 * @param name the header value
	 */
	private void validateValue(CharSequence value) {
		int index = HttpHeaderValidationUtil.validateValidHeaderValue(value);
		if (index != -1) {
			throw new IllegalArgumentException("a header value contains prohibited character 0x" + Integer.toHexString(value.charAt(index)) + " at index " + index + '.');
		}
	}
}