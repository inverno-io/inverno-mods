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

import java.nio.charset.Charset;
import java.util.Set;

import io.netty.buffer.ByteBuf;
import io.inverno.mod.http.base.header.AbstractHeaderCodec;
import io.inverno.mod.http.base.header.HeaderCodec;
import io.inverno.mod.http.base.header.HeaderService;

/**
 * <p>
 * Genric HTTP {@link HeaderCodec} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AbstractHeaderCodec
 */
public class GenericHeaderCodec extends AbstractHeaderCodec<GenericHeader, GenericHeader.Builder> {

	/**
	 * <p>
	 * Creates a generic HTTP header codec.
	 * </p>
	 */
	protected GenericHeaderCodec() {
		super(GenericHeader.Builder::new, Set.of("*"));
	}

	@Override
	public GenericHeader decode(String name, String value) {
		return this.builderSupplier.get().headerName(name).headerValue(value).build();
	}

	@Override
	public GenericHeader decode(String name, ByteBuf buffer, Charset charset) {
		int readerIndex = buffer.readerIndex();
		
		GenericHeader.Builder builder = this.builderSupplier.get().headerName(name);
		
		Integer startIndex = null;
		Integer endIndex = null;
		while(buffer.isReadable()) {
			byte nextByte = buffer.readByte();
			
			if(startIndex == null && Character.isWhitespace(nextByte)) {
				continue;
			}
			else {
				if(startIndex == null) {
					 startIndex = buffer.readerIndex() - 1;
				}
				
				if(nextByte == CR) {
					if(buffer.getByte(buffer.readerIndex()) == LF) {
						buffer.readByte();
						endIndex = buffer.readerIndex() - 2;
						if(startIndex == endIndex) {
							buffer.readerIndex(readerIndex);
							throw new MalformedHeaderException(name);
						}
						return builder.headerValue(buffer.slice(startIndex, endIndex - startIndex).toString(charset)).build();
					}
				}
				else if(nextByte == LF) {
					endIndex = buffer.readerIndex() - 1;
					return builder.headerValue(buffer.slice(startIndex, endIndex - startIndex).toString(charset)).build();
				}
				else if(!HeaderService.isContentCharacter((char)nextByte)) {
					throw new MalformedHeaderException(name + ": Invalid character " + (char)nextByte);
				}
			}
		}
		buffer.readerIndex(readerIndex);
		// TODO returning null might not be the proper way to tell that we don't have enough data...
		return null;
	}

	@Override
	public String encode(GenericHeader headerField) {
		StringBuilder result = new StringBuilder();
		result.append(headerField.getHeaderName()).append(": ").append(this.encodeValue(headerField));
		return result.toString();
	}

	@Override
	public String encodeValue(GenericHeader headerField) {
		return headerField.getHeaderValue();
	}
}
