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
package io.inverno.mod.http.base.header;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;

/**
 * <p>
 * Provides a unified access to HTTP headers, giving the ability to decode or encode {@link Header} instances for various headers.
 * </p>
 *
 * <p>
 * Implementations can rely on multiple {@link HeaderCodec} to decode and encode headers based on the header name.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see Header
 * @see HeaderCodec
 */
public interface HeaderService {

	/**
	 * <p>
	 * Decodes the specified header field as defined by <a href="https://tools.ietf.org/html/rfc7230#section-3.2">RFC 7230 Section 3.2</a>.
	 * </p>
	 *
	 * @param <T>    the decoded header type
	 * @param header a raw header field
	 *
	 * @return a decoded header instance
	 */
	<T extends Header> T decode(String header);
	
	/**
	 * <p>
	 * Decodes the specified header field {@link ByteBuf} as defined by <a href="https://tools.ietf.org/html/rfc7230#section-3.2">RFC 7230 Section 3.2</a> using the specified charset.
	 * </p>
	 *
	 * @param <T>     the decoded header type
	 * @param buffer  a raw header field
	 * @param charset the charset to use for decoding
	 *
	 * @return a decoded header instance
	 */
	<T extends Header> T decode(ByteBuf buffer, Charset charset);
	
	/**
	 * <p>
	 * Decodes the specified raw header value for the specified header name.
	 * </p>
	 * 
	 * @param <T>     the decoded header type
	 * @param name  a header name
	 * @param value a header raw value
	 * 
	 * @return a decoded header instance
	 */
	<T extends Header> T decode(String name, String value);
	
	/**
	 * <p>
	 * Decodes the specified raw value {@link ByteBuf} for the specified header name using the specified charset.
	 * </p>
	 * 
	 * @param <T>     the decoded header type
	 * @param name    a header name
	 * @param buffer  a header raw value
	 * @param charset the charset to use for decoding
	 * 
	 * @return a decoded header instance
	 */
	<T extends Header> T decode(String name, ByteBuf buffer, Charset charset);
	
	/**
	 * <p>
	 * Encodes the specified header as a string.
	 * </p>
	 * 
	 * <p>
	 * The resulting value is a header field as defined by <a href="https://tools.ietf.org/html/rfc7230#section-3.2">RFC 7230 Section 3.2</a>.
	 * </p>
	 * 
	 * @param <T>    the encoded header type
	 * @param header the header to encode
	 * 
	 * @return the encoded header string
	 */
	<T extends Header> String encode(T header);
	
	/**
	 * <p>
	 * Encodes the specified header in the specified {@link ByteBuffer}
	 * using the specified charset.
	 * </p>
	 * 
	 * <p>
	 * The resulting value is a header field as defined by <a href="https://tools.ietf.org/html/rfc7230#section-3.2">RFC 7230 Section 3.2</a>.
	 * </p>
	 * 
	 * @param <T>     the encoded header type
	 * @param header  the header to encode
	 * @param buffer  the destination byte buffer
	 * @param charset the charset to use for encoding
	 */
	<T extends Header> void encode(T header, ByteBuf buffer, Charset charset);
	
	/**
	 * <p>
	 * Encodes the value of the specified header as a string.
	 * </p>
	 * 
	 * <p>
	 * The resulting value corresponds to the header field value as defined by <a href="https://tools.ietf.org/html/rfc7230#section-3.2">RFC 7230 Section 3.2</a>.
	 * </p>
	 * 
	 * @param <T>    the encoded header type
	 * @param header the header to encode
	 * @return the encoded header value
	 */
	<T extends Header> String encodeValue(T header);
	
	/**
	 * <p>
	 * Encodes the value of the specified header in the specified {@link ByteBuffer}
	 * using the specified charset.
	 * </p>
	 * 
	 * <p>
	 * The resulting value corresponds to the header field value as defined by <a href="https://tools.ietf.org/html/rfc7230#section-3.2">RFC 7230 Section 3.2</a>.
	 * </p>
	 * 
	 * @param <T>     the encoded header type
	 * @param header  the header to encode
	 * @param buffer  the destination byte buffer
	 * @param charset the charset to use for encoding
	 */
	<T extends Header> void encodeValue(T header, ByteBuf buffer, Charset charset);
	
	/**
	 * <p>
	 * Determines whether the specified character is a valid header token character as defined by <a href="https://tools.ietf.org/html/rfc7230#section-3.2.6">RFC 7230 Section 3.2.6</a>.
	 * </p>
	 * 
	 * @param character the character to test
	 * 
	 * @return true if the character is a header token character, false otherwise
	 */
	public static boolean isTokenCharacter(char character) {
		return Character.isLetterOrDigit(character) || 
				character == '!' || 
				character == '#' ||
				character == '$' ||
				character == '%' ||
				character == '\'' ||
				character == '*' ||
				character == '+' ||
				character == '-' ||
				character == '.' ||
				character == '^' ||
				character == '_' ||
				character == '`' ||
				character == '|' ||
				character == '~';
	}
	
	/**
	 * <p>
	 * Determines whether the specified value is a valid header token as defined by <a href="https://tools.ietf.org/html/rfc7230#section-3.2.6">RFC 7230 Section 3.2.6</a>.
	 * </p>
	 * 
	 * @param value the value to test
	 * 
	 * @return true if the value is a header token, false otherwise
	 */
	public static boolean isToken(String value) {
		if(value == null || value.length() == 0) {
			return false;
		}
		for(int i=0;i<value.length();i++) {
			if(!isTokenCharacter(value.charAt(i))) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * <p>
	 * Determines whether the specified character is a valid header base64 character as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7235#section-4.2">RFC 7235 Section 2.1</a>.
	 * </p>
	 *
	 * @param character the character to test
	 *
	 * @return true if the character is a header token68 character, false otherwise
	 */
	public static boolean isB64TokenCharacter(char character) {
		return Character.isLetterOrDigit(character) || 
				character == '-' || 
				character == '.' ||
				character == '_' ||
				character == '~' ||
				character == '+' ||
				character == '/';
	}
	
	/**
	 * <p>
	 * Determines whether the specified value is a valid header base64 token as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7235#section-4.2">RFC 7235 Section 2.1</a>.
	 * </p>
	 *
	 * @param value the value to test
	 *
	 * @return true if the value is a header base64 token, false otherwise
	 */
	public static boolean isB64Token(String value) {
		if(value == null || value.length() == 0) {
			return false;
		}
		for(int i=0;i<value.length();i++) {
			if(!isB64TokenCharacter(value.charAt(i))) {
				if(value.charAt(i) == '=') {
					for(;i<value.length();i++) {
						if(value.charAt(i) != '=') {
							return false;
						}
					}
				}
				else {
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * <p>
	 * Determines whether the specified character is a valid header content character as defined by <a href="https://tools.ietf.org/html/rfc7230#section-3.2.6">RFC 7230 Section 3.2.6</a> and 
	 * <a href="https://tools.ietf.org/html/rfc5234#appendix-B.1">RFC 5234 Appendix B.1</a>
	 * </p>
	 * 
	 * @param character the character to test
	 * 
	 * @return true if the character is a header content character, false otherwise
	 */
	public static boolean isContentCharacter(char character) {
		return (character > 31 && character < 127) || character == 9;
	}
	
	/**
	 * <p>
	 * Determines whether the specified value is a valid header content as defined by <a href="https://tools.ietf.org/html/rfc7230#section-3.2.6">RFC 7230 Section 3.2.6</a> and
	 * <a href="https://tools.ietf.org/html/rfc5234#appendix-B.1">RFC 5234 Appendix B.1</a>
	 * </p>
	 * 
	 * @param value the value to test
	 * 
	 * @return true if the value is a header content, false otherwise
	 */
	public static boolean isContent(String value) {
		if(value == null || value.length() == 0) {
			return false;
		}
		
		byte[] valueBytes = value.getBytes();
		for(int i=0;i<valueBytes.length;i++) {
			if(!isContentCharacter(value.charAt(i))) {
				return false;
			}
		}
		return true;
	}
}
