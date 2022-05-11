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
package io.inverno.mod.http.base.internal.header;

import io.inverno.mod.base.Charsets;
import io.inverno.mod.http.base.header.AbstractHeaderCodec;
import io.inverno.mod.http.base.header.HeaderCodec;
import io.inverno.mod.http.base.header.HeaderService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * <p>
 * A generic parameterized {@link HeaderCodec} implementation used to
 * encode/decode various parameterized headers.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ParameterizedHeader
 * @see HeaderCodec
 * 
 * @param <A> parameterized header type
 * @param <B> parameterized header builder type
 */
public class ParameterizedHeaderCodec<A extends ParameterizedHeader, B extends ParameterizedHeader.AbstractBuilder<A, B>> extends AbstractHeaderCodec<A, B> {

	/**
	 * Default parameter delimiter.
	 */
	public static final char DEFAULT_PARAMETER_DELIMITER = ';';
	
	/**
	 * Default value delimiter.
	 */
	public static final char DEFAULT_VALUE_DELIMITER = ',';
	
	/**
	 * The value delimiter.
	 */
	protected final char valueDelimiter;
	
	/**
	 * The parameter delimiter.
	 */
	protected final char parameterDelimiter;
	
	/**
	 * The value delimiter. 
	 */
	protected final char parameterValueDelimiter;
	
	private final boolean allowEmptyValue; 
	
	private final boolean expectNoValue; 
	
	private final boolean allowFlagParameter;
	
	private final boolean allowSpaceInValue;
	
	private final boolean allowQuotedValue;
	
	private final boolean allowMultiple;
	
	/**
	 * <p>
	 * Creates a parameterized header codec with the specified header builder supplier, list of supported header names, value delimiter, parameter delimiter, parameter value delimiter and options.
	 * </p>
	 *
	 * @param builderSupplier         a supplier to create header builder instances when decoding a header
	 * @param supportedHeaderNames    the list of header names supported by the codec
	 * @param valueDelimiter          a value delimiter
	 * @param parameterDelimiter      a parameter delimiter
	 * @param parameterValueDelimiter a parameter value delimiter
	 * @param allowEmptyValue         allow empty parameterized value
	 * @param expectNoValue           expect no parameterized value
	 * @param allowFlagParameter      allow flag parameters (ie. parameter with no value)
	 * @param allowSpaceInValue       allow space in values
	 * @param allowQuotedValue        allow quoted values
	 * @param allowMultiple           allow multiple header values
	 */
	public ParameterizedHeaderCodec(Supplier<B> builderSupplier, Set<String> supportedHeaderNames, char valueDelimiter, char parameterDelimiter, char parameterValueDelimiter, boolean allowEmptyValue, boolean expectNoValue, boolean allowFlagParameter, boolean allowSpaceInValue, boolean allowQuotedValue, boolean allowMultiple) {
		super(builderSupplier, supportedHeaderNames);
		
		this.valueDelimiter = valueDelimiter;
		this.parameterDelimiter = parameterDelimiter;
		this.parameterValueDelimiter = parameterValueDelimiter;
		this.allowEmptyValue = allowEmptyValue;
		this.expectNoValue = expectNoValue;
		this.allowFlagParameter = allowFlagParameter;
		this.allowSpaceInValue = allowSpaceInValue;
		this.allowQuotedValue = allowQuotedValue;
		this.allowMultiple = allowMultiple;
		
		if(!this.allowEmptyValue && this.expectNoValue) {
			throw new IllegalArgumentException("Can't expect no value and not allow empty value");
		}
	}
	
	@Override
	public A decode(String name, String rawValue) {
		ByteBuf buffer = Unpooled.wrappedBuffer(rawValue.getBytes(Charsets.DEFAULT));
		try {
			return this.decode(name, buffer, Charsets.DEFAULT, true);
		}
		finally {
			buffer.release();
		}
	}
	
	@Override
	public A decode(String name, ByteBuf buffer, Charset charset) {
		return this.decode(name, buffer, charset, false);
	}

	/**
	 * <p>
	 * Decodes the specified raw value ByteBuf for the specified header name using the specified charset.
	 * </p>
	 *
	 * name - a header name buffer - a header raw value charset - the charset to use for decoding
	 *
	 * @param name    a header name
	 * @param buffer  a header raw value
	 * @param charset the charset to use for decoding
	 * @param lf      true to indicate that a silent LF byte is present after the last readable byte in the buffer to indicate the end of the header
	 *
	 * @return a decoded header instance
	 */
	private A decode(String name, ByteBuf buffer, Charset charset, boolean lf) {
		// This can be optimized if the buffer is backed by a byte array
		// It is apparently faster to navigate through the available byte array rather than the bytebuf
		int readerIndex = buffer.readerIndex();
		
		B builder = this.builderSupplier.get().headerName(name);
		boolean value = false;
		boolean end = false;
		Integer startIndex = null;
		Integer endIndex = null;
		String parameterName = null;
		boolean quoted = false;
		boolean blankValue = true;
		boolean endSingle = false;
		while(true) {
			byte nextByte;
			if(buffer.isReadable()) {
				nextByte = buffer.readByte();
				if(nextByte == HeaderCodec.CR) {
					if(buffer.isReadable()) {
						if(buffer.getByte(buffer.readerIndex()) == HeaderCodec.LF) {
							buffer.readByte();
							if(endIndex == null) {
								endIndex = buffer.readerIndex() - 2;
							}
							builder.headerValue(buffer.getCharSequence(readerIndex, buffer.readerIndex() - 2 - readerIndex, charset).toString());
							end = true;
						}
						else {
							buffer.readerIndex(readerIndex);
							throw new MalformedHeaderException(name + ": Bad end of line");
						}
					}
					else {
						break;
					}
				}
				else if(nextByte == HeaderCodec.LF) {
					if(endIndex == null) {
						endIndex = buffer.readerIndex() - 1;
					}
					builder.headerValue(buffer.getCharSequence(readerIndex, buffer.readerIndex() - 1 - readerIndex, charset).toString());
					end = true;
				}
				else if(nextByte == this.parameterValueDelimiter && this.allowMultiple && !quoted) {
					if(endIndex == null) {
						endIndex = buffer.readerIndex() - 1;
					}
					endSingle = true;
				}
				else if(!HeaderService.isContentCharacter((char)nextByte)) {
					throw new MalformedHeaderException(name + ": Invalid character " + (char)nextByte);
				}
			}
			else if(lf) {
				nextByte = HeaderCodec.LF;
				if(endIndex == null) {
					endIndex = buffer.readerIndex();
				}
				builder.headerValue(buffer.getCharSequence(readerIndex, buffer.readerIndex() - readerIndex, charset).toString());
				end = true;
			}
			else {
				break;
			}
			
			if(end || endSingle) {
				if(!value) {
					if(startIndex == null) {
						buffer.readerIndex(readerIndex);
						throw new MalformedHeaderException(name);
					}
					if(this.expectNoValue) {
						buffer.readerIndex(readerIndex);
						throw new MalformedHeaderException(name + ": expect no value");
					}
					builder.parameterizedValue(buffer.getCharSequence(startIndex, endIndex - startIndex, charset).toString());
				}
				else if(parameterName != null) {
					if(startIndex == null) {
						buffer.readerIndex(readerIndex);
						throw new MalformedHeaderException(name);
					}
					builder.parameter(parameterName, buffer.getCharSequence(startIndex, endIndex - startIndex, charset).toString());
				}
				else if(startIndex != null) {
					if(endIndex == null) {
						endIndex = buffer.readerIndex() - 1;
					}
					if(startIndex.equals(endIndex)) {
						buffer.readerIndex(readerIndex);
						throw new MalformedHeaderException(name);
					}
					if(!this.allowFlagParameter) {
						buffer.readerIndex(readerIndex);
						throw new MalformedHeaderException(name + ": flag parameters not allowed");
					}
					builder.parameter(buffer.getCharSequence(startIndex, endIndex - startIndex, charset).toString(), null);
				}
				
				if(end) {
					return builder.build();
				}
				else {
					// consider next value
					value = false;
					end = false;
					startIndex = null;
					endIndex = null;
					parameterName = null;
					quoted = false;
					blankValue = true;
					endSingle = false;
					continue;
				}
			}
			
			if(!value) {
				if(startIndex == null) {
					if(!this.allowSpaceInValue && Character.isWhitespace(nextByte)) {
						continue;
					}
					startIndex = buffer.readerIndex() - 1;
				}
				
				if(nextByte == this.valueDelimiter) {
					if(this.expectNoValue) {
						buffer.readerIndex(readerIndex);
						throw new MalformedHeaderException(name + ": expect no value");
					}
					if(endIndex == null) {
						endIndex = buffer.readerIndex() - 1;
					}
					if(startIndex.equals(endIndex)) {
						if(!this.allowEmptyValue) {
							buffer.readerIndex(readerIndex);
							throw new MalformedHeaderException(name + ": empty value not allowed");
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
						throw new MalformedHeaderException(name + ": empty value not allowed");
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
						throw new MalformedHeaderException(name + ": space not allowed in value");
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
						if(startIndex.equals(endIndex)) {
							buffer.readerIndex(readerIndex);
							throw new MalformedHeaderException(name);
						}
						parameterName = buffer.getCharSequence(startIndex, endIndex - startIndex, charset).toString();
						startIndex = endIndex = null;
					}
					else if(nextByte == this.parameterDelimiter) {
						if(endIndex == null) {
							endIndex = buffer.readerIndex() - 1;
						}
						if(startIndex.equals(endIndex)) {
							buffer.readerIndex(readerIndex);
							throw new MalformedHeaderException(name);
						}
						if(!this.allowFlagParameter) {
							buffer.readerIndex(readerIndex);
							throw new MalformedHeaderException(name + ": flag parameters not allowed");
						}
						builder.parameter(buffer.getCharSequence(startIndex, endIndex - startIndex, charset).toString(), null);
						parameterName = null;
						startIndex = endIndex = null;
					}
					else if(Character.isWhitespace(nextByte)) {
						endIndex = buffer.readerIndex() - 1;
					}
					else if(!HeaderService.isTokenCharacter((char)nextByte)) {
						buffer.readerIndex(readerIndex);
						throw new MalformedHeaderException(name + ": invalid character " + (char)nextByte);
					}
					else if(endIndex != null) {
						// There's a space inside the name 
						buffer.readerIndex(readerIndex);
						throw new MalformedHeaderException(name);
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
					
					if(nextByte == this.parameterDelimiter) {
						if(endIndex == null) {
							endIndex = buffer.readerIndex() - 1;
						}
						if(startIndex.equals(endIndex)) {
							buffer.readerIndex(readerIndex);
							throw new MalformedHeaderException(name);
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
							throw new MalformedHeaderException(name + ": space not allowed in value");
						}
						endIndex = null;
					}
					else if(blankValue){
						blankValue = false;
					}
				}
			}
		}
		// We need more bytes
		buffer.readerIndex(readerIndex);
		return null;
	}
	
	@Override
	public String encode(A header) {
		StringBuilder result = new StringBuilder();
		result.append(header.getHeaderName()).append(": ").append(this.encodeValue(header));
		return result.toString();
	}
	
	@Override
	public String encodeValue(A header) {
		StringBuilder result = new StringBuilder();
		
		if(!this.expectNoValue) {
			result.append(header.getParameterizedValue());
		}
		
		Map<String, String> parameters = header.getParameters();
		if(!parameters.isEmpty()) {
			result.append(this.valueDelimiter);
			for(Map.Entry<String, String> e : parameters.entrySet()) {
				result.append(e.getKey()).append("=").append(e.getValue()).append(this.parameterDelimiter);
			}
		}
		return result.substring(0, result.length() - 1);
	}
}
