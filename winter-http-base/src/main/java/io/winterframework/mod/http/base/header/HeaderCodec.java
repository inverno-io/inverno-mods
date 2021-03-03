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

import java.nio.charset.Charset;
import java.util.Set;

import io.netty.buffer.ByteBuf;

/**
 * @author jkuhn
 *
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
	
	A decode(String name, String value);
	
	A decode(String name, ByteBuf buffer, Charset charset);
	
	String encode(A headerField);
	
	default void encode(A headerField, ByteBuf buffer, Charset charset) {
		buffer.writeCharSequence(this.encode(headerField), charset);
	}
	
	String encodeValue(A headerField);
	
	default void encodeValue(A headerField, ByteBuf buffer, Charset charset) {
		buffer.writeCharSequence(this.encodeValue(headerField), charset);
	}
	
	Set<String> getSupportedHeaderNames();
}
