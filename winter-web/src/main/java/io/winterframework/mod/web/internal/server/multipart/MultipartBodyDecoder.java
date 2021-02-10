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
package io.winterframework.mod.web.internal.server.multipart;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpConstants;
import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.mod.base.converter.ObjectConverter;
import io.winterframework.mod.base.resource.MediaTypes;
import io.winterframework.mod.web.header.Header;
import io.winterframework.mod.web.header.HeaderService;
import io.winterframework.mod.web.header.Headers;
import io.winterframework.mod.web.server.Part;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.SignalType;

/**
 * https://www.w3.org/Protocols/rfc1341/7_2_Multipart.html
 * 
 * @author jkuhn
 *
 */
@Bean(visibility = Visibility.PRIVATE)
public class MultipartBodyDecoder implements MultipartDecoder<Part> {
	
	private final HeaderService headerService;
	
	private final ObjectConverter<String> parameterConverter;
	
	public MultipartBodyDecoder(HeaderService headerService, ObjectConverter<String> parameterConverter) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
	}
	
	public Flux<Part> decode(Flux<ByteBuf> data, Headers.ContentType contentType) {
		if(contentType == null || !contentType.getMediaType().equalsIgnoreCase(MediaTypes.MULTIPART_FORM_DATA)) {
			throw new IllegalArgumentException("Content type is not " + MediaTypes.MULTIPART_FORM_DATA);
		}
		if(contentType.getBoundary() == null) {
			throw new IllegalArgumentException("Missing multipart form data boundary");
		}
		
		// Why parts flux is sequential: we have to emit all parts before we can send response back to the client?
		// => request data flux emits chunk, the same thread is used for
		// subscription as a result no parts can be emitted before the chunk is fully
		// processed, this explain why we received batch of parts: they all result from
		// one single request data chunk
		return Flux.create(emitter -> {
			data.subscribe(new BodyDataSubscriber(contentType, emitter));
		});
	}
	
	private DecoderTask boundary(ByteBuf buffer, BodyDataSubscriber context) throws MalformedBodyException {
		String delimiter = context.getDelimiter();
		
		int readerIndex = buffer.readerIndex();
		if(!this.skipControlCharacters(buffer)) {
			buffer.readerIndex(readerIndex);
		}
		this.skipOneLine(buffer);
		
		readerIndex = buffer.readerIndex();
		
		int delimiterPos = 0;
        int len = delimiter.length();
        while(buffer.isReadable() && delimiterPos < len) {
            byte nextByte = buffer.readByte();
            if(nextByte == delimiter.charAt(delimiterPos)) {
                delimiterPos++;
            } 
            else {
            	// We were expecting a valid delimiter but we didn't find it
            	throw new MalformedBodyException("No delimiter found");
            }
        }
        if(delimiterPos < len - 1) {
        	// Not enough data
        	buffer.readerIndex(readerIndex);
        	return null;
        }
        
        // determine if we have an opening or closing delimiter
        if(buffer.isReadable()) {
        	// \r\n | \n => opening
        	// -- => closing
        	byte nextByte = buffer.readByte();
        	if(nextByte == HttpConstants.CR) {
        		if(buffer.isReadable()) {
        			nextByte = buffer.readByte();
        			if(nextByte == HttpConstants.LF) {
        				// opening boundary
        				return this::headers;
        			}
        			else {
        				// We were expecting a valid delimiter but we didn't find it
        				throw new MalformedBodyException("No delimiter found");
        			}
        		}
        	}
        	else if(nextByte == HttpConstants.LF) {
        		// opening boundary
				return this::headers;
        	}
        	else if(nextByte == '-') {
        		if(buffer.isReadable()) {
        			nextByte = buffer.readByte();
        			if(nextByte == '-') {
        				// TODO if we want to use this for multipart/mixed then we must also look for \r\n 
        				
        				// closing boundary
        				return this::end;
        			}
        		}
        	}
        	
        }
        // Not enough data
        buffer.readerIndex(readerIndex);
        return null;
	}
	
	private DecoderTask headers(ByteBuf buffer, BodyDataSubscriber context) throws MalformedBodyException {
		while(!this.skipOneLine(buffer)) {
			Header headerField = this.headerService.decode(buffer, context.contentType.getCharset());
			if(headerField != null) {
				context.addDecodedHeader(headerField);
			}
			else {
				return null;
			}
		}
		
		// We have all headers, we can create the part
		Headers.ContentDisposition partContentDispositionHeader = context.<Headers.ContentDisposition>getDecodedHeader(Headers.NAME_CONTENT_DISPOSITION);
		if(partContentDispositionHeader == null || partContentDispositionHeader.getPartName() == null) {
			throw new MalformedBodyException("Missing content disposition");
		}
		
		Headers.ContentType partContentTypeHeader = context.<Headers.ContentType>getDecodedHeader(Headers.NAME_CONTENT_TYPE);
		
		if(partContentTypeHeader != null && partContentTypeHeader.getMediaType().equalsIgnoreCase(MediaTypes.MULTIPART_MIXED)) {
			context.startMultipartMixed(partContentTypeHeader);
			return this::boundary;
		}
		else {
			String partName = partContentDispositionHeader.getPartName();
			String partFilename = partContentDispositionHeader.getFilename();
			
			if(context.isMultipartMixed() && partFilename == null) {
				throw new MalformedBodyException("Field not supported in mixed multipart");
			}
			
			String partContentType = partContentTypeHeader != null ? partContentTypeHeader.getMediaType() : null;
			if(partFilename != null && partContentType == null) {
				partContentType = MediaTypes.APPLICATION_OCTET_STREAM;
			}
			Charset partCharset = partContentTypeHeader != null && partContentTypeHeader.getCharset() != null ? partContentTypeHeader.getCharset() : context.contentType.getCharset();
			
			Header partContentLengthHeader = context.<Header>getDecodedHeader(Headers.NAME_CONTENT_LENGTH);
			Long partContentLength = partContentLengthHeader != null ? Long.parseLong(partContentLengthHeader.getHeaderValue()) : null;
			
			context.startPart(new GenericPart(this.parameterConverter, partName, partFilename, context.getAllDecodedHeaders(), partContentType, partCharset, partContentLength));
			
			return this::data;
		}
	}
	
	private DecoderTask data(ByteBuf buffer, BodyDataSubscriber context) {
		String delimiter = context.getDelimiter();
		
		int readerIndex = buffer.readerIndex();
		
		int delimiterLength = delimiter.length();
		Integer delimiterIndex = null;
		Integer delimiterReaderIndex = null;
		while(buffer.isReadable()) {
			byte nextByte = buffer.readByte();
			if(nextByte == HttpConstants.CR) {
				if(!buffer.isReadable()) {
					delimiterIndex = 0;
					delimiterReaderIndex = buffer.readerIndex() - 1;
				}
				else if(buffer.getByte(buffer.readerIndex()) == HttpConstants.LF) {
					buffer.readByte();
					delimiterIndex = 0;
					delimiterReaderIndex = buffer.readerIndex() - 2;
				}
			}
			else if(nextByte == HttpConstants.LF) {
				delimiterIndex = 0;
				delimiterReaderIndex = buffer.readerIndex() - 1;
			}
			else if(delimiterIndex != null) {
				if(nextByte == delimiter.codePointAt(delimiterIndex)) {
					delimiterIndex++;
					if(delimiterIndex == delimiterLength) {
						// We found the delimiter
						if(context.getPart().getDataEmitter().isPresent()) {
							FluxSink<ByteBuf> partDataEmitter = context.getPart().getDataEmitter().get();
							if(readerIndex < delimiterReaderIndex) {
								partDataEmitter.next(buffer.retainedSlice(readerIndex, delimiterReaderIndex - readerIndex));
							}
						}
						return this::end;
					}
				}
				else {
					delimiterIndex = delimiterReaderIndex = null;
				}
			}
		}
		
		if(context.getPart().getDataEmitter().isPresent()) {
			FluxSink<ByteBuf> partDataEmitter = context.getPart().getDataEmitter().get();
			if(readerIndex < buffer.readerIndex()) {
				partDataEmitter.next(buffer.retainedSlice(readerIndex, buffer.readerIndex() - readerIndex));
			}
		}
		return null;
	}
	
	private DecoderTask end(ByteBuf buffer, BodyDataSubscriber context) {
		context.endPart();
		int readerIndex = buffer.readerIndex();
		if(buffer.readableBytes() >= 2) {
			if(buffer.readByte() == '-' && buffer.readByte() == '-') {
				if(context.isMultipartMixed()) {
					context.endMultipartMixed();
					return this::boundary;
				}
				else {
					context.complete();
					return null;
				}
			}
			else {
				buffer.readerIndex(readerIndex);
				this.skipOneLine(buffer);
				return this::headers;
			}
		}
		else {
			return null;
		}
	}
	
	private boolean skipControlCharacters(ByteBuf buffer) {
		try {
			for (;;) {
	            char c = (char) buffer.readUnsignedByte();
	            if (!Character.isISOControl(c) && !Character.isWhitespace(c)) {
	            	buffer.readerIndex(buffer.readerIndex() - 1);
	                break;
	            }
	        }
        } 
		catch (IndexOutOfBoundsException e1) {
			return false;
        }
        return true;
	}
	
	private boolean skipOneLine(ByteBuf buffer) {
        if (!buffer.isReadable()) {
            return false;
        }
        byte nextByte = buffer.readByte();
        if (nextByte == HttpConstants.CR) {
            if (!buffer.isReadable()) {
            	buffer.readerIndex(buffer.readerIndex() - 1);
                return false;
            }
            nextByte = buffer.readByte();
            if (nextByte == HttpConstants.LF) {
                return true;
            }
            buffer.readerIndex(buffer.readerIndex() - 2);
            return false;
        }
        if (nextByte == HttpConstants.LF) {
            return true;
        }
        buffer.readerIndex(buffer.readerIndex() - 1);
        return false;
    }
	
	@FunctionalInterface
	private static interface DecoderTask {
		
		DecoderTask run(ByteBuf buffer, BodyDataSubscriber context) throws MalformedBodyException;
	}
	
	private class BodyDataSubscriber extends BaseSubscriber<ByteBuf> {
		
		private final Headers.ContentType contentType;
		private final FluxSink<Part> emitter;
		
		private ByteBuf keepBuffer;
		
		private final String delimiter;
		
		private String mixedDelimiter;
		
		private DecoderTask task;
		
		private GenericPart part;
		
		private Map<String, List<Header>> decodedHeaders;
		
		public BodyDataSubscriber(Headers.ContentType contentType, FluxSink<Part> emitter) {
			this.contentType = contentType;
			this.delimiter = "--" + contentType.getBoundary();
			this.task = MultipartBodyDecoder.this::boundary;
			this.emitter = emitter;
			this.emitter.onCancel(() -> this.cancel());
		}
		
		public String getDelimiter() {
			return this.mixedDelimiter != null ? this.mixedDelimiter : this.delimiter;
		}
		
		public void startMultipartMixed(Headers.ContentType partContentType) throws MalformedBodyException {
			if(this.mixedDelimiter != null) {
				throw new MalformedBodyException("Nested multipart mixed not allowed");
			}
			if(partContentType.getBoundary() == null) {
				throw new MalformedBodyException("Missing multipart mixed boundary");
			}
			this.mixedDelimiter = "--" + partContentType.getBoundary();
			this.part = null;
			if(this.decodedHeaders != null) {
				this.decodedHeaders.clear();
			}
		}
		
		public boolean isMultipartMixed() {
			return this.mixedDelimiter != null;
		}
		
		public void endMultipartMixed() {
			this.mixedDelimiter = null;
			this.part = null;
			if(this.decodedHeaders != null) {
				this.decodedHeaders.clear();
			}
		}
		
		public void startPart(GenericPart part) {
			this.part = part;
			this.emitter.next(this.part);
		}
		
		public GenericPart getPart() {
			return this.part;
		}
		
		public void endPart() {
			if(this.part != null) {
				this.part.getDataEmitter().ifPresent(emitter -> emitter.complete());
				this.part = null;
				if(this.decodedHeaders != null) {
					this.decodedHeaders.clear();
				}
			}
		}
		
		public void complete() {
			this.emitter.complete();
			this.cancel();
		}
		
		public void addDecodedHeader(Header decodedHeaderField) {
			if(this.decodedHeaders == null) {
				this.decodedHeaders = new LinkedHashMap<>();
			}
			List<Header> headerFieldList = this.decodedHeaders.get(decodedHeaderField.getHeaderName());
			if(headerFieldList == null) {
				headerFieldList = new ArrayList<>();
				this.decodedHeaders.put(decodedHeaderField.getHeaderName(), headerFieldList);
			}
			headerFieldList.add(decodedHeaderField);
		}
		
		public Map<String, List<Header>> getAllDecodedHeaders() {
			return this.decodedHeaders;
		}
		
		@SuppressWarnings("unchecked")
		public <T> List<T> getDecodedHeaders(String name) {
			return (List<T>)this.decodedHeaders.get(name);
		}
		
		public <T> T getDecodedHeader(String name) {
			List<T> headers = this.<T>getDecodedHeaders(name);
			if(headers == null || headers.isEmpty()) {
				return null;
			}
			if(headers.size() > 1) {
				throw new IllegalStateException("Invalid request");
			}
			return headers.get(0);
		}
		
		@Override
		protected void hookOnNext(ByteBuf value) {
			try {
				final ByteBuf buffer;
				if(this.keepBuffer != null && this.keepBuffer.isReadable()) {
					buffer = Unpooled.wrappedBuffer(this.keepBuffer, value);
				}
				else {
					buffer = value;
				}
				
				try {
					DecoderTask currentTask = this.task;
					while( (currentTask = currentTask.run(buffer, this)) != null) {
						this.task = currentTask;
					}
				}
				catch (Exception e) {
					this.emitter.error(e);
					this.cancel();
				}
				
				if(buffer.isReadable()) {
					if(this.keepBuffer != null) {
						this.keepBuffer.discardReadBytes();
						this.keepBuffer.writeBytes(buffer);
					}
					else {
						this.keepBuffer = Unpooled.unreleasableBuffer(Unpooled.buffer(buffer.readableBytes()));
						this.keepBuffer.writeBytes(buffer);
					}
				}
			}
			finally {
				// TODO This hasn't been tested
				// In case there are remaining bytes, these are not released so we must release them in some ways
				value.release();
			}
		}
		
		@Override
		protected void hookOnError(Throwable throwable) {
			this.emitter.error(throwable);
		}
		
		@Override
		protected void hookOnComplete() {
			this.emitter.complete();
		}
		
		@Override
		protected void hookFinally(SignalType type) {
			if(this.keepBuffer != null) {
				this.keepBuffer.release();
				this.keepBuffer = null;
			}
		}
	}
}
