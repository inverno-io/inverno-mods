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
package io.winterframework.mod.web.internal.server;

import java.util.function.Consumer;

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpConstants;
import io.winterframework.mod.base.Charsets;
import io.winterframework.mod.base.resource.MediaTypes;
import io.winterframework.mod.web.InternalServerErrorException;
import io.winterframework.mod.web.NotFoundException;
import io.winterframework.mod.web.header.Headers;
import io.winterframework.mod.web.server.ResponseBody;
import io.winterframework.mod.web.server.ServerSentEvent;
import io.winterframework.mod.web.server.ServerSentEvent.Configurator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

/**
 * @author jkuhn
 *
 */
public class GenericResponseBody implements ResponseBody {
	
	private static final String SSE_CONTENT_TYPE = MediaTypes.TEXT_EVENT_STREAM + ";charset=utf-8";
	
	protected AbstractResponse response;
	
	protected ResponseBody.Raw dataBody;
	protected ResponseBody.Sse<ByteBuf> sseBody;
	protected ResponseBody.Resource resourceBody;
	
	private MonoSink<Publisher<ByteBuf>> dataEmitter;
	private Publisher<ByteBuf> data;
	
	private boolean dataSet;
	private boolean single;
	
	public GenericResponseBody(AbstractResponse response) {
		this.response = response;
	}
	
	protected final void setData(Publisher<ByteBuf> data) {
		if(this.dataSet) {
			throw new IllegalStateException("Response data already posted");
		}
		if(data instanceof Mono) {
			this.single = true;
		}
		if(this.dataEmitter != null) {
			this.dataEmitter.success(data);
		}
		this.data = data;
	}
	
	public Publisher<ByteBuf> getData() {
		if(this.data == null) {
			this.setData(Flux.switchOnNext(Mono.<Publisher<ByteBuf>>create(emitter -> this.dataEmitter = emitter)));
			this.dataSet = false;
		}
		return this.data;
	}

	public boolean isSingle() {
		return this.single;
	}
	
	@Override
	public void empty() {
		this.setData(Mono.empty());
	}

	@Override
	public Raw raw() {
		if(this.dataBody == null) {
			this.dataBody = new GenericRawResponseBody();
		}
		return this.dataBody;
	}

	@Override
	public Sse<ByteBuf> sse() {
		if(this.sseBody == null) {
			this.sseBody = new GenericSseResponseBody();
		}
		return this.sseBody;
	}
	
	@Override
	public Resource resource() {
		if(this.resourceBody == null) {
			this.resourceBody = new GenericResourceResponseBody();
		}
		return this.resourceBody;
	}
	
	protected class GenericRawResponseBody implements ResponseBody.Raw {

		@Override
		public void data(Publisher<ByteBuf> data) {
			GenericResponseBody.this.setData(data);
		}
		
		@Override
		public void data(String data) {
			this.data(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(data, Charsets.UTF_8))));
		}
		
		@Override
		public void data(byte[] data) {
			this.data(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(data))));
		}
	}

	protected class GenericSseResponseBody implements ResponseBody.Sse<ByteBuf> {

		@Override
		public void events(Publisher<ServerSentEvent<ByteBuf>> events) {
			GenericResponseBody.this.response.headers(headers -> headers
				.contentType(GenericResponseBody.SSE_CONTENT_TYPE)
			);
			
			GenericResponseBody.this.setData(Flux.from(events)
				.flatMapSequential(sse -> {
					StringBuilder sseMetaData = new StringBuilder();
					if(sse.getId() != null) {
						sseMetaData.append("id:").append(sse.getId()).append("\n");
					}
					if(sse.getEvent() != null) {
						sseMetaData.append("event:").append(sse.getEvent()).append("\n");
					}
					if(sse.getComment() != null) {
						sseMetaData.append(":").append(sse.getComment().replaceAll("\\r\\n|\\r|\\n", "\r\n:")).append("\n");
					}
					
					Flux<ByteBuf> sseData = Flux.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(sseMetaData, Charsets.UTF_8)));
					
					if(sse.getData() != null) {
						sseData = sseData
							.concatWith(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("data:", Charsets.UTF_8))))
							.concatWith(Flux.from(sse.getData())
								.map(chunk -> {
									ByteBuf escapedChunk = Unpooled.unreleasableBuffer(Unpooled.buffer(chunk.readableBytes(), Integer.MAX_VALUE));
									while(chunk.isReadable()) {
										byte nextByte = chunk.readByte();
										
										if(nextByte == HttpConstants.CR) {
											if(chunk.getByte(chunk.readerIndex()) == HttpConstants.LF) {
												chunk.readByte();
											}
											escapedChunk.writeCharSequence("\r\ndata:", Charsets.UTF_8);
										}
										else if(nextByte == HttpConstants.LF) {
											escapedChunk.writeCharSequence("\r\ndata:", Charsets.UTF_8);
										}
										else {
											escapedChunk.writeByte(nextByte);
										}
									}
									return escapedChunk;
								})
							);
					}
					sseData = sseData.concatWith(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("\r\n\r\n", Charsets.UTF_8))));
					return sseData;
				}));
		}

		@Override
		public ServerSentEvent<ByteBuf> create(Consumer<Configurator<ByteBuf>> configurer) {
			GenericServerSentEvent sse = new GenericServerSentEvent();
			configurer.accept(sse);
			return sse;
		}
	}
	
	protected class GenericResourceResponseBody implements ResponseBody.Resource {

		protected void populateHeaders(io.winterframework.mod.base.resource.Resource resource) {
			GenericResponseBody.this.response.headers(h -> {
				if(GenericResponseBody.this.response.headers().getContentLength() == null) {
					Long size = resource.size();
					if(size != null) {
						h.contentLength(size);
					}
				}
				
				if(GenericResponseBody.this.response.headers().getCharSequence(Headers.NAME_CONTENT_TYPE) == null) {
					String mediaType = resource.getMediaType();
					if(mediaType != null) {
						h.contentType(mediaType);
					}
				}
			});
		}
		
		@Override
		public void data(io.winterframework.mod.base.resource.Resource resource) {
			// Http2 doesn't support FileRegion so we have to read the resource and send it to the response data flux
			Boolean exists = resource.exists();
			if(exists == null || exists) {
				// In case of file resources we should always be able to determine existence
				// For other resources with a null exists we can still try, worst case scenario: 
				// internal server error
				this.populateHeaders(resource);
				GenericResponseBody.this.setData(resource.read().orElseThrow(() -> new InternalServerErrorException("Resource " + resource + " is not readable")));
			}
			else {
				throw new NotFoundException();
			}
		}
	}
}
