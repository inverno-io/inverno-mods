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
package io.winterframework.mod.web.header;

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;

/**
 * @author jkuhn
 *
 */
public interface HeaderService {

	<T extends Header> T decode(String header);
	
	<T extends Header> T decode(ByteBuf buffer, Charset charset);
	
	<T extends Header> T decode(String name, String value);
	
	<T extends Header> T decode(String name, ByteBuf buffer, Charset charset);
	
	<T extends Header> String encode(T headerField);
	
	<T extends Header> void encode(T headerField, ByteBuf buffer, Charset charset);
	
	<T extends Header> String encodeValue(T headerField);
	
	<T extends Header> void encodeValue(T headerField, ByteBuf buffer, Charset charset);
	
	/*
	 * https://tools.ietf.org/html/rfc7230#section-3.2.6
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
	
	/*
	 * https://tools.ietf.org/html/rfc7230#section-3.2
	 * https://tools.ietf.org/html/rfc5234#appendix-B.1
	 */
	public static boolean isContentCharacter(char character) {
		return (character > 31 && character < 127) || character == 9;
	}
	
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
