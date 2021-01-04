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
	
	public static boolean isTokenCharacter(byte codePoint) {
		return Character.isLetterOrDigit(codePoint) || 
				codePoint == '!' || 
				codePoint == '#' ||
				codePoint == '$' ||
				codePoint == '%' ||
				codePoint == '\'' ||
				codePoint == '*' ||
				codePoint == '+' ||
				codePoint == '-' ||
				codePoint == '.' ||
				codePoint == '^' ||
				codePoint == '_' ||
				codePoint == '`' ||
				codePoint == '|' ||
				codePoint == '~';
	}
	
	public static boolean isToken(String token) {
		if(token.length() == 0) {
			return false;
		}
		byte[] tokenBytes = token.getBytes();
		for(int i=0;i<tokenBytes.length;i++) {
			if(!isTokenCharacter(tokenBytes[i])) {
				return false;
			}
		}
		return true;
	}
}
