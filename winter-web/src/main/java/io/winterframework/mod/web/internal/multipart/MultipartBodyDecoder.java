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
package io.winterframework.mod.web.internal.multipart;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.util.CharsetUtil;
import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.mod.commons.resource.MediaTypes;
import io.winterframework.mod.web.Header;
import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.Part;
import io.winterframework.mod.web.internal.Charsets;
import io.winterframework.mod.web.internal.RequestBodyDecoder;
import io.winterframework.mod.web.internal.header.ContentDispositionCodec;
import io.winterframework.mod.web.internal.header.ContentTypeCodec;
import io.winterframework.mod.web.internal.header.GenericHeaderService;
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
public class MultipartBodyDecoder implements RequestBodyDecoder<Part> {
	
	private HeaderService httpHeaderFieldService;
	
	public MultipartBodyDecoder(HeaderService httpHeaderFieldService) {
		this.httpHeaderFieldService = httpHeaderFieldService;
	}
	
	public static void main(String[] args) {
		HeaderService headerService = new GenericHeaderService(List.of(new ContentTypeCodec(), new ContentDispositionCodec()));
		
		MultipartBodyDecoder decoder = new MultipartBodyDecoder(headerService);
		
		/*String multipart = "-----------------------------41716138319688775731431041856\n"
				+ "Content-Disposition: form-data; name=\"toto\"\n"
				+ "\n"
				+ "a\n"
				+ "-----------------------------41716138319688775731431041856\n"
				+ "Content-Disposition: form-data; name=\"tata\"\n"
				+ "\n"
				+ "b\n"
				+ "-----------------------------41716138319688775731431041856\n"
				+ "Content-Disposition: form-data; name=\"somefile\"; filename=\"Toto.java\"\n"
				+ "Content-Type: text/x-java\n"
				+ "\n"
				+ "public class Toto {\n"
				+ "\n"
				+ "	public static void main(String[] args) {\n"
				+ "		String toto = \"toto\";\n"
				+ "		String tata = \"tata\";\n"
				+ "		System.out.println(\"Hello \" + toto + \" \" + tata);\n"
				+ "	}\n"
				+ "}\n"
				+ "\n"
				+ "-----------------------------41716138319688775731431041856--\n"
				+ "";*/
		
		String multipart = "-----------------------------41716138319688775731431041856\n"
				+ "Content-Disposition: form-data; name=\"toto\"\n"
				+ "\n"
				+ "a\n"
				+ "-----------------------------41716138319688775731431041856\n"
				+ "Content-Disposition: form-data; name=\"tata\"\n"
				+ "Content-Type: multipart/mixed; boundary=---------------------------1234\n"
				+ "\n"
				+ "-----------------------------1234\n"
				+ "Content-Disposition: form-data; name=\"file1\"; filename=\"Toto.java\"\n"
				+ "Content-Type: text/x-java\n"
				+ "\n"
				+ "public class Toto {\n"
				+ "\n"
				+ "}\n"
				+ "-----------------------------1234\n"
				+ "Content-Disposition: form-data; name=\"file2\"; filename=\"Tata.java\"\n"
				+ "Content-Type: text/x-java\n"
				+ "\n"
				+ "public class Tata {}\n"
				+ "-----------------------------1234--\n"
				+ "-----------------------------41716138319688775731431041856\n"
				+ "Content-Disposition: form-data; name=\"tata\"\n"
				+ "\n"
				+ "b\n"
				+ "-----------------------------41716138319688775731431041856\n"
				+ "Content-Disposition: form-data; name=\"somefile\"; filename=\"Toto.java\"\n"
				+ "Content-Type: text/x-java\n"
				+ "\n"
				+ "public class Toto {\n"
				+ "\n"
				+ "	public static void main(String[] args) {\n"
				+ "		String toto = \"toto\";\n"
				+ "		String tata = \"tata\";\n"
				+ "		System.out.println(\"Hello \" + toto + \" \" + tata);\n"
				+ "	}\n"
				+ "}\n"
				+ "\n"
				+ "-----------------------------41716138319688775731431041856--";
		
		ByteBuf buffer = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(multipart, CharsetUtil.UTF_8));

		Headers.ContentType contentType = headerService.<Headers.ContentType>decode("content-type: multipart/form-data; boundary=---------------------------41716138319688775731431041856; charset=UTF-8");
		
		decoder.decode(Flux.just(buffer), contentType)
			.subscribe(part -> {
				System.out.println("================================================================================");
				System.out.println("name: " + part.getName());
				part.getFilename().ifPresent(filename -> System.out.println("filename: " + filename));
				System.out.println("headers: ");
				System.out.println(part.headers().getAll().entrySet().stream()
					.flatMap(e -> {
						return e.getValue().stream();
					})
					.map(h -> "  - " + headerService.encode(h))
					.collect(Collectors.joining("\n"))
				);
				System.out.print("data: \n[");
				part.data().subscribe(
					chunk -> {
						System.out.print(chunk.toString(Charsets.UTF_8));
					},
					ex -> {
						
					},
					() -> {
						System.out.println("]\n================================================================================");
					}
				);
			},
			ex -> {
				ex.printStackTrace();
			},
			() -> {
				
			});
		
		
		/*long total = 0, min = Long.MAX_VALUE, max = 0;
		int count = 100000;
		for(int i=0;i<count;i++) {
			buffer.resetReaderIndex();
			long t0 = System.nanoTime();
			decoder.decode(Flux.just(buffer), contentType)
				.collectList().block();
			long te = System.nanoTime();
			total += te - t0;
			min = Math.min(min, te-t0);
			max = Math.max(max, te-t0);
		}
		System.out.println("AVG: " + (total / count) + ", MIN: " + min + ", MAX: " + max);*/
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
		// - if current form data is null, create one based on the content disposition
		// - emit current form data => we need the emitter
		
		// We need to parse all headers before emit the form part
		// headers should be kept in the context
		
		// between each header we must check that we do not have an empty line
		
		while(!this.skipOneLine(buffer)) {
			Header headerField = this.httpHeaderFieldService.decode(buffer, context.contentType.getCharset());
			if(headerField != null) {
				context.addDecodedHeader(headerField);
			}
			else {
				return null;
			}
		}
		
		// We have all headers, we can create the part
		Headers.ContentDisposition partContentDispositionHeader = context.<Headers.ContentDisposition>getDecodedHeader(Headers.CONTENT_DISPOSITION);
		if(partContentDispositionHeader == null || partContentDispositionHeader.getPartName() == null) {
			throw new MalformedBodyException("Missing content disposition");
		}
		
		Headers.ContentType partContentTypeHeader = context.<Headers.ContentType>getDecodedHeader(Headers.CONTENT_TYPE);
		
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
			
			Header partContentLengthHeader = context.<Header>getDecodedHeader(Headers.CONTENT_LENGTH);
			Long partSize = partContentLengthHeader != null ? Long.parseLong(partContentLengthHeader.getHeaderValue()) : null;
			
			context.startPart(new GenericPart(partName, partFilename, context.getAllDecodedHeaders(), partContentType, partCharset, partSize));
			
			return this::data;
		}
	}
	
	private DecoderTask data(ByteBuf buffer, BodyDataSubscriber context) {
		// - consume buffer until we hit boundary
		// - when consuming emit data to the current form data flux => we need an emitter
//		System.out.println("data");
		
		// loop on bytes
		// - if \r\n we can start check for boundary: delimiterIndex = 0 and delimiterReaderIndex = currentIndex - 2
		// - if we reach end of buffer and delimiterIndex != null we must return null and set buffer reader index to delimiterReaderIndex
		// - 

		// We emit retainedSlice() buffer which must be released in the Part implementation 
		
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
			if(delimiterIndex != null) {
				buffer.readerIndex(delimiterReaderIndex);
				if(readerIndex < delimiterReaderIndex) {
					partDataEmitter.next(buffer.retainedSlice(readerIndex, delimiterReaderIndex - readerIndex));
				}
			}
			else {
				if(readerIndex < buffer.readerIndex()) {
					partDataEmitter.next(buffer.retainedSlice(readerIndex, buffer.readerIndex() - readerIndex));
				}
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
			/*catch (MalformedBodyException e) {
				this.emitter.error(e);
				this.cancel();
			}*/ 
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
//					this.keepBuffer = value.alloc().buffer(buffer.readableBytes());
					this.keepBuffer = Unpooled.unreleasableBuffer(Unpooled.buffer(buffer.readableBytes()));
					this.keepBuffer.writeBytes(buffer);
				}
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
//				System.out.println("release " + count.incrementAndGet() + " " + this.keepBuffer.refCnt());
				this.keepBuffer = null;
			}
			else {
//				System.out.println("finally " + count.incrementAndGet());
			}
		}
	}
}
