/**
 * 
 */
package io.winterframework.mod.web.internal.header;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpConstants;
import io.winterframework.core.annotation.Bean;
import io.winterframework.mod.web.Header;
import io.winterframework.mod.web.HeaderCodec;
import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.internal.Charsets;

/**
 * @author jkuhn
 *
 */
@Bean
public class HeaderServiceImpl implements HeaderService {

	private Map<String, HeaderCodec<?>> codecs;
	
	private HeaderCodec<?> defaultCodec;
	
	public HeaderServiceImpl(List<HeaderCodec<?>> codecs) {
		this.codecs = new HashMap<>();
		
		for(HeaderCodec<?> codec : codecs) {
			for(String supportedHeaderName : codec.getSupportedHeaderNames()) {
				supportedHeaderName = supportedHeaderName.toLowerCase();
				// TODO at some point this is an issue in Spring as well, we should fix this in winter
				// provide annotation for sorting at compile time and be able to inject maps as well 
				// - annotations defined on the beans with some meta data
				// - annotations defined on multiple bean socket to specify sorting for list, array or sets
				// - we can also group by key to inject a map => new multi socket type
				// - this is a bit tricky as for selector when it comes to the injection of list along with single values 
				HeaderCodec<?> previousCodec = this.codecs.put(supportedHeaderName, codec);
				if(previousCodec != null) {
					throw new IllegalStateException("Multiple codecs found for header " + supportedHeaderName + ": " + previousCodec.toString() + ", " + codec.toString());
				}
			}
		}
		
		this.defaultCodec = this.codecs.get("*");
		if(this.defaultCodec == null) {
			this.defaultCodec = new GenericHeaderCodec();
		}
	}
	
	@Override
	public <T extends Header> T decode(String header) {
		ByteBuf buffer = Unpooled.copiedBuffer(header, Charsets.UTF_8);
		buffer.writeByte(HttpConstants.LF);
		try {
			return this.decode(buffer, Charsets.UTF_8);
		}
		finally {
			buffer.release();
		}
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
		
		T result = (T)this.getCodec(name).decode(name, buffer, charsetOrDefault);
		if(result == null) {
			buffer.readerIndex(readerIndex);
		}
		return result;
	}

	@Override
	public <T extends Header> String encode(T headerField) {
		return this.getCodec(headerField.getHeaderName()).encode(headerField);
	}

	@Override
	public <T extends Header> void encode(T headerField, ByteBuf buffer, Charset charset) {
		Charset charsetOrDefault = Charsets.orDefault(charset);
		this.getCodec(headerField.getHeaderName()).encode(headerField, buffer, charsetOrDefault);
	}

	@Override
	public <T extends Header> T decode(String name, String value) {
		ByteBuf buffer = Unpooled.copiedBuffer(value, Charsets.UTF_8);
		buffer.writeByte(HttpConstants.LF);
		try {
			return this.decode(name, buffer, Charsets.UTF_8);
		}
		finally {
			buffer.release();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Header> T decode(String name, ByteBuf buffer, Charset charset) {
		return (T)this.getCodec(name).decode(name, buffer, charset);
	}

	@Override
	public <T extends Header> String encodeValue(T headerField) {
		return this.getCodec(headerField.getHeaderName()).encodeValue(headerField);
	}

	@Override
	public <T extends Header> void encodeValue(T headerField, ByteBuf buffer, Charset charset) {
		Charset charsetOrDefault = Charsets.orDefault(charset);
		this.getCodec(headerField.getHeaderName()).encodeValue(headerField, buffer, charsetOrDefault);
	}
	
	@SuppressWarnings("unchecked")
	private <T extends Header> HeaderCodec<T> getCodec(String name) {
		HeaderCodec<?> codec = this.codecs.get(name);
		if(codec == null) {
			codec = this.defaultCodec;
		}
		return (HeaderCodec<T>) codec;
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
					 throw new IllegalArgumentException("Malformed Header: empty name");
				 }
				 return buffer.slice(startIndex, endIndex - startIndex).toString(charset).toLowerCase();
			 }
			 else if(Character.isWhitespace(nextByte)) {
				 // There's a white space between the header name and the colon
				 buffer.readerIndex(readerIndex);
				 throw new IllegalArgumentException("Malformed Header: white space");
			 }
			 else if(!HeaderService.isTokenCharacter(nextByte)) {
				 buffer.readerIndex(readerIndex);
				 buffer.readerIndex(readerIndex);
				 throw new IllegalArgumentException("Malformed Header: " + (buffer.readerIndex()-1) + " " +buffer.toString(Charsets.UTF_8) + " " + String.valueOf(Character.toChars(nextByte)));
			 }
		}
		buffer.readerIndex(readerIndex);
		return null;
	}
}
