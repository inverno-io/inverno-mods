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

import java.io.IOException;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpConstants;
import io.winterframework.mod.commons.resource.MediaTypes;
import io.winterframework.mod.web.Charsets;
import io.winterframework.mod.web.InternalServerErrorException;
import io.winterframework.mod.web.NotFoundException;
import io.winterframework.mod.web.Response;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.ServerSentEvent;
import io.winterframework.mod.web.ServerSentEvent.Configurator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

/**
 * @author jkuhn
 *
 */
public class GenericResponseBody implements ResponseBody {
	
	protected GenericResponse response;
	
	protected ResponseBody.Raw dataBody;
	protected ResponseBody.Sse<ByteBuf> sseBody;
	protected ResponseBody.Resource resourceBody;
	
	private MonoSink<Flux<ByteBuf>> dataEmitter;
	private Flux<ByteBuf> data;
	
	private long size;
	private long chunkCount;
	private boolean dataSet;
	
	public GenericResponseBody(GenericResponse response) {
		this.response = response;
	}
	
	protected final void setData(Flux<ByteBuf> data) {
		if(this.dataSet) {
			throw new IllegalStateException("Response data already posted");
		}
		if(this.dataEmitter != null) {
			this.dataEmitter.success(data);
		}
		else {
			this.data = data.bufferUntil(s -> {
				this.chunkCount++;
				this.size += s.readableBytes();
				return this.chunkCount >= 2;
			})
			.flatMapIterable(l -> {
				Long contentLength = this.response.getHeaders().getSize();
				if(this.chunkCount < 2) {
					// Response has one chunk...
					if(contentLength == null) {
						// ...and no content length
						this.response.getHeaders().size(this.size);
					}
					else if(this.size != contentLength){
						// ...and the content length doesn't match the actual size
						throw new IllegalStateException("Response content length doesn't match the actual response size");
					}
				}
				else if(contentLength != null && contentLength < this.size) {
					throw new IllegalStateException("Response content length exceeded");
				}
				return l;
			})
			.doOnComplete(() -> {
				if(this.chunkCount == 0 && this.response.getHeaders().getSize() == null) {
					this.response.getHeaders().size(0);
				}
			})
			.doOnError(t -> t.printStackTrace());
		}
		this.dataSet = true;
	}

	public Flux<ByteBuf> getData() {
		if(this.data == null) {
			this.setData(Flux.switchOnNext(Mono.<Flux<ByteBuf>>create(emitter -> this.dataEmitter = emitter)));
			this.dataSet = false;
		}
		return this.data;
	}
	
	public long getChunkCount() {
		return this.chunkCount;
	}
	
	public long getSize() {
		return this.size;
	}

	@Override
	public Response<Void> empty() {
		this.setData(Flux.empty());
		return this.response.<Void>map(responseBody -> null);
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
		public Response<Raw> data(Publisher<ByteBuf> data) {
			GenericResponseBody.this.setData(Flux.from(data));
			return GenericResponseBody.this.response.<ResponseBody.Raw>map(responseBody -> responseBody.raw());
		}
		
		@Override
		public Response<Raw> data(String data) {
			return this.data(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(data, Charsets.UTF_8))));
		}
		
		@Override
		public Response<Raw> data(byte[] data) {
			return this.data(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(data))));
		}
	}

	protected class GenericSseResponseBody implements ResponseBody.Sse<ByteBuf> {

		@Override
		public Response<Sse<ByteBuf>> events(Publisher<ServerSentEvent<ByteBuf>> events) {
			GenericResponseBody.this.response.headers(headers -> headers
				.contentType(MediaTypes.TEXT_EVENT_STREAM)
				.charset(Charsets.UTF_8)
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
						// We need to make create a new "data:..." line if we encounter an end of line...
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
			
			return GenericResponseBody.this.response.<ResponseBody.Sse<ByteBuf>>map(responseBody -> responseBody.sse());
		}

		@Override
		public ServerSentEvent<ByteBuf> create(Consumer<Configurator<ByteBuf>> configurer) {
			GenericServerSentEvent sse = new GenericServerSentEvent();
			configurer.accept(sse);
			return sse;
		}
	}
	
	protected class GenericResourceResponseBody implements ResponseBody.Resource {

		protected void populateHeaders(io.winterframework.mod.commons.resource.Resource resource) {
			GenericResponseBody.this.response.headers(h -> {
				if(GenericResponseBody.this.response.getHeaders().getSize() == null) {
					Long size;
					try {
						size = resource.size();
						if(size != null) {
							h.size(size);
						}
					} 
					catch (IOException e) {
						// TODO maybe a debug log?
					}
				}
				
				if(GenericResponseBody.this.response.getHeaders().getContentType() == null) {
					try {
						String mediaType = resource.getMediaType();
						if(mediaType != null) {
							h.contentType(mediaType);
						}
					} 
					catch (IOException e) {
						// TODO maybe a debug log? 
					}
				}
			});
		}
		
		@Override
		public Response<Resource> data(io.winterframework.mod.commons.resource.Resource resource) {
			// Http2 doesn't support FileRegion so we have to read the resource and send it to the response data flux
			try {
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
			catch (IOException e) {
				throw new InternalServerErrorException("Error while reading resource " + resource, e);
			}
			return GenericResponseBody.this.response.<ResponseBody.Resource>map(responseBody -> responseBody.resource());
		}
	}
}
