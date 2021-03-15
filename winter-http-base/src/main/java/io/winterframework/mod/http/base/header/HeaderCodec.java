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
package io.winterframework.mod.http.base.header;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Set;

import io.netty.buffer.ByteBuf;

/**
 * <p>
 * A HTTP header codec is used to encode and decode HTTP headers.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Header
 * @see HeaderService
 * 
 * @param <A> the header type encoded/decoded by the codec
 */
public interface HeaderCodec<A extends Header> {

	/**
	 * Horizontal space
	 */
	byte SP = 32;

	/**
	 * Horizontal tab
	 */
	byte HT = 9;

	/**
	 * Carriage return
	 */
	byte CR = 13;

	/**
	 * Equals '='
	 */
	byte EQUALS = 61;

	/**
	 * Line feed character
	 */
	byte LF = 10;

	/**
	 * Colon ':'
	 */
	byte COLON = 58;

	/**
	 * Semicolon ';'
	 */
	byte SEMICOLON = 59;

	/**
	 * Comma ','
	 */
	byte COMMA = 44;

	/**
	 * Double quote '"'
	 */
	byte DOUBLE_QUOTE = '"';

	/**
	 * Horizontal space
	 */
	char SP_CHAR = (char) SP;
	
	/**
	 * <p>
	 * Decodes the specified raw header value for the specified header name.
	 * </p>
	 * 
	 * @param name  a header name
	 * @param value a header raw value
	 * 
	 * @return a decoded header instance
	 */
	A decode(String name, String value);
	
	/**
	 * <p>
	 * Decodes the specified raw value {@link ByteBuf} for the specified header
	 * name using the specified charset.
	 * </p>
	 * 
	 * @param name    a header name
	 * @param buffer  a header raw value
	 * @param charset the charset to use for decoding
	 * 
	 * @return a decoded header instance
	 */
	A decode(String name, ByteBuf buffer, Charset charset);
	
	/**
	 * <p>
	 * Encodes the specified header as a string.
	 * </p>
	 * 
	 * <p>
	 * The resulting value is a header field as defined by
	 * <a href="https://tools.ietf.org/html/rfc7230#section-3.2">RFC 7230 Section
	 * 3.2</a>.
	 * </p>
	 * 
	 * @param header the header to encode
	 * 
	 * @return the encoded header
	 */
	String encode(A header);
	
	/**
	 * <p>
	 * Encodes the specified header in the specified {@link ByteBuffer}
	 * using the specified charset.
	 * </p>
	 * 
	 * <p>
	 * The resulting value is a header field as defined by
	 * <a href="https://tools.ietf.org/html/rfc7230#section-3.2">RFC 7230 Section
	 * 3.2</a>.
	 * </p>
	 * 
	 * @param header  the header to encode
	 * @param buffer  the destination byte buffer
	 * @param charset the charset to use for encoding
	 */
	default void encode(A header, ByteBuf buffer, Charset charset) {
		buffer.writeCharSequence(this.encode(header), charset);
	}
	
	/**
	 * <p>
	 * Encodes the value of the specified header as a string.
	 * </p>
	 * 
	 * <p>
	 * The resulting value corresponds to the header field value as defined by
	 * <a href="https://tools.ietf.org/html/rfc7230#section-3.2">RFC 7230 Section
	 * 3.2</a>.
	 * </p>
	 * 
	 * @param header the header to encode
	 * @return the encoded header value
	 */
	String encodeValue(A header);

	/**
	 * <p>
	 * Encodes the value of the specified header in the specified {@link ByteBuffer}
	 * using the specified charset.
	 * </p>
	 * 
	 * <p>
	 * The resulting value corresponds to the header field value as defined by
	 * <a href="https://tools.ietf.org/html/rfc7230#section-3.2">RFC 7230 Section
	 * 3.2</a>.
	 * </p>
	 * 
	 * @param header  the header to encode
	 * @param buffer  the destination byte buffer
	 * @param charset the charset to use for encoding
	 */
	default void encodeValue(A header, ByteBuf buffer, Charset charset) {
		buffer.writeCharSequence(this.encodeValue(header), charset);
	}
	
	/**
	 * <p>
	 * Returns a list of header names supported by the codec.
	 * </p>
	 * 
	 * @return a list of header names
	 */
	Set<String> getSupportedHeaderNames();
}
