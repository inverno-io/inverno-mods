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
package io.winterframework.mod.web.internal.header;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.util.CharsetUtil;
import io.winterframework.mod.web.AbstractHeaderCodec;

/**
 * @author jkuhn
 *
 */
public class ParameterizedHeaderCodec<A extends ParameterizedHeader, B extends ParameterizedHeader.AbstractBuilder<A, B>> extends AbstractHeaderCodec<A, B> {

	public static final char DEFAULT_DELIMITER = ';';
	
	protected final char delimiter;
	
	private final boolean allowEmptyValue; 
	
	private final boolean expectNoValue; 
	
	private final boolean allowFlagParameter;
	
	private final boolean allowSpaceInValue;
	
	private final boolean allowQuotedValue;
	
	public ParameterizedHeaderCodec(Supplier<B> builderSupplier, Set<String> supportedHeaderNames, char delimiter, boolean allowEmptyValue, boolean expectNoValue, boolean allowFlagParameter, boolean allowSpaceInValue, boolean allowQuotedValue) {
		super(builderSupplier, supportedHeaderNames);
		
		this.delimiter = delimiter;
		this.allowEmptyValue = allowEmptyValue;
		this.expectNoValue = expectNoValue;
		this.allowFlagParameter = allowFlagParameter;
		this.allowSpaceInValue = allowSpaceInValue;
		this.allowQuotedValue = allowQuotedValue;
		
		if(!this.allowEmptyValue && this.expectNoValue) {
			throw new IllegalArgumentException("Can't expect no value and not allow empty value");
		}
	}
	
	@Override
	public A decode(String name, String rawValue) {
		ByteBuf buffer = Unpooled.copiedBuffer(rawValue, CharsetUtil.UTF_8);
		buffer.writeByte(HttpConstants.LF);
		try {
			return this.decode(name, buffer, CharsetUtil.UTF_8);
		}
		finally {
			buffer.release();
		}
	}

	// This can be optimized if the buffer is backed by a byte array
	// It is apparently faster to navigate through the available byte array rather than the bytebuf
	@Override
	public A decode(String name, ByteBuf buffer, Charset charset) {
		int readerIndex = buffer.readerIndex();
		
		B builder = this.builderSupplier.get().headerName(name);
		boolean value = false;
		boolean end = false;
		Integer startIndex = null;
		Integer endIndex = null;
		String parameterName = null;
		boolean quoted = false;
		boolean blankValue = true;
		while(buffer.isReadable()) {
			byte nextByte = buffer.readByte();
			if(nextByte == HttpConstants.CR) {
				if(buffer.isReadable()) {
					if(buffer.getByte(buffer.readerIndex()) == HttpConstants.LF) {
						buffer.readByte();
						if(endIndex == null) {
							endIndex = buffer.readerIndex() - 2;
						}
						builder.headerValue(buffer.getCharSequence(readerIndex, buffer.readerIndex() - 2 - readerIndex, charset).toString());
						end = true;
						break;
					}
					else {
						buffer.readerIndex(readerIndex);
						throw new IllegalArgumentException("Malformed Header: Bad end of line");
					}
				}
				else {
					break;
				}
			}
			else if(nextByte == HttpConstants.LF) {
				if(endIndex == null) {
					endIndex = buffer.readerIndex() - 1;
				}
				builder.headerValue(buffer.getCharSequence(readerIndex, buffer.readerIndex() - 1 - readerIndex, charset).toString());
				end = true;
				break;
			}
			
			if(!value) {
				if(startIndex == null) {
					if(!this.allowSpaceInValue && Character.isWhitespace(nextByte)) {
						continue;
					}
					startIndex = buffer.readerIndex() - 1;
				}
				
				if(nextByte == ';') {
					if(this.expectNoValue) {
						buffer.readerIndex(readerIndex);
						throw new IllegalArgumentException("Malformed Header: expect no value");
					}
					if(endIndex == null) {
						endIndex = buffer.readerIndex() - 1;
					}
					if(startIndex == endIndex) {
						if(!this.allowEmptyValue) {
							buffer.readerIndex(readerIndex);
							throw new IllegalArgumentException("Malformed Header: empty value not allowed");
						}
						else {
							builder.parameterizedValue("");
						}
					}
					else {
						builder.parameterizedValue(buffer.getCharSequence(startIndex, endIndex - startIndex, charset).toString());
					}
					value = true;
					startIndex = endIndex = null;
				}
				else if(nextByte == '=') {
					if(!this.allowEmptyValue) {
						buffer.readerIndex(readerIndex);
						throw new IllegalArgumentException("Malformed Header: empty value not allowed");
					}
					if(endIndex == null) {
						endIndex = buffer.readerIndex() - 1;
					}
					builder.parameterizedValue(null);
					parameterName = buffer.getCharSequence(startIndex, endIndex - startIndex, charset).toString().trim();
					value = true;
					startIndex = endIndex = null;
				}
				else if(Character.isWhitespace(nextByte)) {
					if(!this.allowSpaceInValue) {
						if(endIndex == null) {
							endIndex = buffer.readerIndex() - 1;
						}
					}
					else {
						endIndex = null;
					}
				}
				else if(endIndex != null) {
					// There's a space inside the value 
					if(!this.allowSpaceInValue) {
						buffer.readerIndex(readerIndex);
						throw new IllegalArgumentException("Malformed Header: space not allowed in value");
					}
					endIndex = null;
				}
			}
			else {
				if(parameterName == null) {
					if(startIndex == null) {
						if(Character.isWhitespace(nextByte)) {
							continue;
						}
						startIndex = buffer.readerIndex() - 1;
					}
					if(nextByte == '=') {
						if(endIndex == null) {
							endIndex = buffer.readerIndex() - 1;
						}
						if(startIndex == endIndex) {
							buffer.readerIndex(readerIndex);
							throw new IllegalArgumentException("Malformed Header");
						}
						parameterName = buffer.getCharSequence(startIndex, endIndex - startIndex, charset).toString();
						startIndex = endIndex = null;
					}
					else if(nextByte == ';') {
						if(endIndex == null) {
							endIndex = buffer.readerIndex() - 1;
						}
						if(startIndex == endIndex) {
							buffer.readerIndex(readerIndex);
							throw new IllegalArgumentException("Malformed Header");
						}
						if(!this.allowFlagParameter) {
							buffer.readerIndex(readerIndex);
							throw new IllegalArgumentException("Malformed Header: flag parameter not allowed");
						}
						builder.parameter(buffer.getCharSequence(startIndex, endIndex - startIndex, charset).toString(), null);
						parameterName = null;
						startIndex = endIndex = null;
					}
					else if(Character.isWhitespace(nextByte)) {
						endIndex = buffer.readerIndex() - 1;
					}
					else if(!HeaderServiceImpl.isTokenCharacter(nextByte)) {
						buffer.readerIndex(readerIndex);
						throw new IllegalArgumentException("Malformed Header: invalid token character");
					}
					else if(endIndex != null) {
						// There's a space inside the name 
						buffer.readerIndex(readerIndex);
						throw new IllegalArgumentException("Malformed Header");
					}
				}
				else {
					if(startIndex == null) {
						if(!this.allowSpaceInValue && Character.isWhitespace(nextByte)) {
							continue;
						}
						startIndex = buffer.readerIndex() - 1;
						if(this.allowQuotedValue && nextByte == '"') {
							quoted = true;
							startIndex++;
							continue;
						}
					}
					else if(this.allowQuotedValue && !quoted && nextByte == '"' && blankValue) {
						quoted = true;
						startIndex = buffer.readerIndex();
						blankValue = false;
						continue;
					}
					
					if(nextByte == this.delimiter) {
						if(endIndex == null) {
							endIndex = buffer.readerIndex() - 1;
						}
						if(startIndex == endIndex) {
							buffer.readerIndex(readerIndex);
							throw new IllegalArgumentException("Malformed Header");
						}
						builder.parameter(parameterName, buffer.getCharSequence(startIndex, endIndex - startIndex, charset).toString());
						parameterName = null;
						startIndex = endIndex = null;
						quoted = false;
						blankValue = true;
					}
					else if(quoted && nextByte == '"' && buffer.getByte(buffer.readerIndex() - 1) != '\\') {
						endIndex = buffer.readerIndex() - 1;
					}
					else if(Character.isWhitespace(nextByte)) {
						if(!quoted) {
							if(!this.allowSpaceInValue) {
								if(endIndex == null) {
									endIndex = buffer.readerIndex() - 1;
								}
							}
							else {
								endIndex = null;
							}
						}
					}
					else if(endIndex != null) {
						// There's a space inside the value 
						if(!this.allowSpaceInValue) {
							buffer.readerIndex(readerIndex);
							throw new IllegalArgumentException("Malformed Header: space not allowed in value");
						}
						endIndex = null;
					}
					else if(blankValue){
						blankValue = false;
					}
				}
			}
		}
		
		if(end) {
			if(!value) {
				if(startIndex == null) {
					buffer.readerIndex(readerIndex);
					throw new IllegalArgumentException("Malformed Header");
				}
				if(this.expectNoValue) {
					buffer.readerIndex(readerIndex);
					throw new IllegalArgumentException("Malformed Header: expect no value");
				}
				builder.parameterizedValue(buffer.getCharSequence(startIndex, endIndex - startIndex, charset).toString());
			}
			else if(parameterName != null) {
				if(startIndex == null) {
					buffer.readerIndex(readerIndex);
					throw new IllegalArgumentException("Malformed Header");
				}
				builder.parameter(parameterName, buffer.getCharSequence(startIndex, endIndex - startIndex, charset).toString());
			}
			else if(startIndex != null) {
				if(endIndex == null) {
					endIndex = buffer.readerIndex() - 1;
				}
				if(startIndex == endIndex) {
					buffer.readerIndex(readerIndex);
					throw new IllegalArgumentException("Malformed Header");
				}
				if(!this.allowFlagParameter) {
					buffer.readerIndex(readerIndex);
					throw new IllegalArgumentException("Malformed Header: flag parameter not allowed");
				}
				builder.parameter(buffer.getCharSequence(startIndex, endIndex - startIndex, charset).toString(), null);
			}
			return builder.build();
		}
		// We need more bytes
		buffer.readerIndex(readerIndex);
		return null;
	}

	@Override
	public String encode(A headerField) {
		StringBuilder result = new StringBuilder();
		result.append(headerField.getHeaderName()).append(": ").append(this.encodeValue(headerField));
		return result.toString();
	}
	
	@Override
	public String encodeValue(A headerField) {
		StringBuilder result = new StringBuilder();
		
		result.append(headerField.getParameterizedValue());
		
		Map<String, String> parameters = headerField.getParameters();
		if(!parameters.isEmpty()) {
			parameters.entrySet().stream().forEach(e -> {
				result.append(this.delimiter).append(e.getKey()).append("=").append(e.getValue());
			});
		}
		return result.toString();
	}
}
