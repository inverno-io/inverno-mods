/**
 * 
 */
package io.winterframework.mod.web;

import java.nio.charset.Charset;
import java.util.Set;

import io.netty.buffer.ByteBuf;

/**
 * @author jkuhn
 *
 */
public interface HeaderCodec<A extends Header> {
	
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
