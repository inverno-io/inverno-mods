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
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.AsciiString;
import io.netty.util.internal.PlatformDependent;

/**
 * <p>
 * Default HTTP/2 {@link HeadersValidator} implementation.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
class DefaultHttp2HeadersValidator implements HeadersValidator {
	
	@Override
	public void accept(CharSequence name, CharSequence value) {
		validateName(name);
		if(value != null) {
			this.validateValue(value);
		}
	}
	
	/**
	 * <p>
	 * Borrowed from {@link DefaultHttp2Headers#HTTP2_NAME_VALIDATOR}.
	 * </p>
	 * 
	 * @param name the header name
	 */
	private static void validateName(CharSequence name) {
		if(name == null || name.length() == 0) {
			PlatformDependent.throwException(Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "empty headers are not allowed [%s]", name));
		}

		if(Http2Headers.PseudoHeaderName.hasPseudoHeaderFormat(name)) {
			if (!Http2Headers.PseudoHeaderName.isPseudoHeader(name)) {
				PlatformDependent.throwException(Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Invalid HTTP/2 pseudo-header '%s' encountered.", name));
			}
			// no need for lower-case validation, we trust our own pseudo header constants
			return;
		}

		if(name instanceof AsciiString) {
			final int index;
			try {
				index = ((AsciiString) name).forEachByte(b -> !AsciiString.isUpperCase(b));
			} 
			catch(Http2Exception e) {
				PlatformDependent.throwException(e);
				return;
			} 
			catch(Throwable t) {
				PlatformDependent.throwException(Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, t, "unexpected error. invalid header name [%s]", name));
				return;
			}

			if(index != -1) {
				PlatformDependent.throwException(Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "invalid header name [%s]", name));
			}
		} 
		else {
			for(int i = 0; i < name.length(); ++i) {
				if(AsciiString.isUpperCase(name.charAt(i))) {
					PlatformDependent.throwException(Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "invalid header name [%s]", name));
				}
			}
		}
	}
	
	private void validateValue(CharSequence value) {
		int index = HttpHeaderValidationUtil.validateValidHeaderValue(value);
		if(index != -1) {
			throw new IllegalArgumentException("a header value contains prohibited character 0x" + Integer.toHexString(value.charAt(index)) + " at index " + index + '.');
		}
	}
}
