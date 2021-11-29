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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.BeanSocket;
import io.inverno.mod.base.Charsets;
import io.inverno.mod.http.base.header.Header;
import io.inverno.mod.http.base.header.HeaderCodec;
import io.inverno.mod.http.base.header.HeaderService;

/**
 * <p>
 * Generic {@link HeaderService} implementation.
 * </p>
 * 
 * <p>
 * This implementation uses multiple HTTP header codecs to encode/decode various
 * HTTP headers based on their name.
 * </p>
 * 
 * <p>
 * The {@link GenericHeaderCodec} is used when no other codec supports the
 * header to encode/decode.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Header
 * @see HeaderCodec
 */
@Bean(name = "headerService")
public class GenericHeaderService implements HeaderService {

	private Map<String, HeaderCodec<?>> codecs;
	
	private HeaderCodec<?> defaultCodec;
	
	/**
	 * <p>
	 * Creates a generic header service.
	 * </p>
	 */
	@BeanSocket
	public GenericHeaderService() throws IllegalStateException {
		this(null);
	}
	
	/**
	 * <p>
	 * Creates a generic header service with the specified list of HTTP header
	 * codecs.
	 * </p>
	 * 
	 * @param codecs a list of header codecs
	 * 
	 * @throws IllegalArgumentException if multiple codecs supporting the same
	 *                                  header name have been specified.
	 */
	public GenericHeaderService(List<HeaderCodec<?>> codecs) throws IllegalArgumentException {
		this.setHeaderCodecs(codecs);
		this.defaultCodec = this.codecs.get("*");
		if(this.defaultCodec == null) {
			this.defaultCodec = new GenericHeaderCodec();
		}
	}
	
	/**
	 * <p>
	 * Sets the header codecs used to encode and decode headers.
	 * </p>
	 * 
	 * @param codecs a list of header codecs
	 * 
	 * @throws IllegalArgumentException if multiple codecs supporting the same
	 *                                  header name have been specified.
	 */
	public void setHeaderCodecs(List<HeaderCodec<?>> codecs) {
		this.codecs = new HashMap<>();
		if(codecs != null) {
			for(HeaderCodec<?> codec : codecs) {
				for(String supportedHeaderName : codec.getSupportedHeaderNames()) {
					supportedHeaderName = supportedHeaderName.toLowerCase();
					// TODO at some point this is an issue in Spring as well, we should fix this in Inverno
					// provide annotation for sorting at compile time and be able to inject maps as well 
					// - annotations defined on the beans with some meta data
					// - annotations defined on multiple bean socket to specify sorting for list, array or sets
					// - we can also group by key to inject a map => new multi socket type
					// - this is a bit tricky as for selector when it comes to the injection of list along with single values 
					HeaderCodec<?> previousCodec = this.codecs.put(supportedHeaderName, codec);
					if(previousCodec != null) {
						throw new IllegalArgumentException("Multiple codecs found for header " + supportedHeaderName + ": " + previousCodec.toString() + ", " + codec.toString());
					}
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Header> T decode(String header) {
		String[] nameValue = this.splitNameValue(header);
		
		if(nameValue == null) {
			return null;
		}
		
		return this.<T>getHeaderCodec(nameValue[0]).orElse((HeaderCodec<T>)this.defaultCodec).decode(nameValue[0], nameValue[1]);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Header> T decode(ByteBuf buffer, Charset charset) {
		int readerIndex = buffer.readerIndex();
		Charset charsetOrDefault = Charsets.orDefault(charset);
		String name = this.readName(buffer, charsetOrDefault);
		
		if(name == null) {
			return null;
		}
		
		T result = this.<T>getHeaderCodec(name).orElse((HeaderCodec<T>)this.defaultCodec).decode(name, buffer, charsetOrDefault);
		if(result == null) {
			buffer.readerIndex(readerIndex);
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Header> String encode(T header) {
		return this.<T>getHeaderCodec(header.getHeaderName()).orElse((HeaderCodec<T>)this.defaultCodec).encode(header);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Header> void encode(T header, ByteBuf buffer, Charset charset) {
		Charset charsetOrDefault = Charsets.orDefault(charset);
		this.<T>getHeaderCodec(header.getHeaderName()).orElse((HeaderCodec<T>)this.defaultCodec).encode(header, buffer, charsetOrDefault);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Header> T decode(String name, String value) {
		return this.<T>getHeaderCodec(name).orElse((HeaderCodec<T>)this.defaultCodec).decode(name, value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Header> T decode(String name, ByteBuf buffer, Charset charset) {
		return this.<T>getHeaderCodec(name).orElse((HeaderCodec<T>)this.defaultCodec).decode(name, buffer, charset);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Header> String encodeValue(T header) {
		return this.<T>getHeaderCodec(header.getHeaderName()).orElse((HeaderCodec<T>)this.defaultCodec).encodeValue(header);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Header> void encodeValue(T header, ByteBuf buffer, Charset charset) {
		Charset charsetOrDefault = Charsets.orDefault(charset);
		this.<T>getHeaderCodec(header.getHeaderName()).orElse((HeaderCodec<T>)this.defaultCodec).encodeValue(header, buffer, charsetOrDefault);
	}
	
	/**
	 * <p>
	 * Returns the header codec for the specified header name.
	 * </p>
	 * 
	 * @param <T>  the header type
	 * @param name the header name
	 * 
	 * @return an optional returning the header codec or an empty optional if
	 *         there's no codec for the specified header name
	 */
	@SuppressWarnings("unchecked")
	public <T extends Header> Optional<HeaderCodec<T>> getHeaderCodec(String name) {
		return Optional.ofNullable((HeaderCodec<T>)this.codecs.get(name));
	}
	
	private String readName(ByteBuf buffer, Charset charset) {
		int readerIndex = buffer.readerIndex();
		Integer startIndex = null;
		Integer endIndex = null;
		while(buffer.isReadable()) {
			byte nextByte = buffer.readByte();

			if(startIndex == null && Character.isWhitespace(nextByte)) {
				continue;
			}
			 
			if(startIndex == null) {
				startIndex = buffer.readerIndex() - 1;
			}
			 
			if(nextByte == ':') {
				endIndex = buffer.readerIndex() - 1;
				if(startIndex == endIndex) {
					buffer.readerIndex(readerIndex);
					throw new MalformedHeaderException("Malformed Header: empty name");
				}
				return buffer.slice(startIndex, endIndex - startIndex).toString(charset).toLowerCase();
			}
			else if(Character.isWhitespace(nextByte)) {
				// There's a white space between the header name and the colon
				buffer.readerIndex(readerIndex);
				throw new MalformedHeaderException("Malformed Header: name can't contain white space");
			}
			else if(!HeaderService.isTokenCharacter((char)nextByte)) {
				buffer.readerIndex(readerIndex);
				throw new MalformedHeaderException("Malformed Header: " + (buffer.readerIndex()-1) + " " + buffer.toString(Charsets.UTF_8) + " " + String.valueOf(Character.toChars(nextByte)));
			}
		}
		buffer.readerIndex(readerIndex);
		return null;
	}
	
	private String[] splitNameValue(String header) {
		Integer startIndex = null;
		Integer endIndex = null;
		for(int i=0;i<header.length();i++) {
			char nextChar = header.charAt(i);
			
			if(startIndex == null && Character.isWhitespace(nextChar)) {
				continue;
			}
			
			if(startIndex == null) {
				startIndex = i;
			}
			
			if(nextChar == ':') {
				endIndex = i;
				if(startIndex == endIndex) {
					throw new MalformedHeaderException("Malformed Header: empty name");
				}
				return new String[] {header.substring(startIndex, endIndex).toLowerCase(), header.substring(i+1)};
			}
			else if(Character.isWhitespace(nextChar)) {
				throw new MalformedHeaderException("Malformed Header: name can't contain white space");
			}
			else if(!HeaderService.isTokenCharacter(nextChar)) {
				throw new MalformedHeaderException("Malformed Header: " + i + " " + header + " " + nextChar);
			}
		}
		return null;
	}
	
	/**
	 * <p>
	 * Header codecs socket.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see GenericHeaderService
	 */
	@Bean( name = "headerCodecs" )
	public static interface HeaderCodecsSocket extends Supplier<List<HeaderCodec<?>>> {}
}
