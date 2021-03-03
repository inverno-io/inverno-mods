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
package io.winterframework.mod.http.server.internal;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpConstants;
import io.winterframework.mod.base.Charsets;
import io.winterframework.mod.base.resource.MediaTypes;
import io.winterframework.mod.http.base.InternalServerErrorException;
import io.winterframework.mod.http.base.NotFoundException;
import io.winterframework.mod.http.base.header.Headers;
import io.winterframework.mod.http.server.ResponseBody;
import io.winterframework.mod.http.server.ResponseData;
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
	
	protected ResponseData<ByteBuf> rawData;
	protected ResponseBody.Resource resourceData;
	protected ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> sseData;
	
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
	public ResponseData<ByteBuf> raw() {
		if(this.rawData == null) {
			this.rawData = new GenericResponseBodyRawData();
		}
		return this.rawData;
	}
	
	@Override
	public Resource resource() {
		if(this.resourceData == null) {
			this.resourceData = new GenericResponseBodyResourceData();
		}
		return this.resourceData;
	}
	
	@Override
	public ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> sse() {
		if(this.sseData == null) {
			this.sseData = new GenericResponseBodySseData();
		}
		return this.sseData;
	}
	
	protected class GenericResponseBodyRawData implements ResponseData<ByteBuf> {

		@SuppressWarnings("unchecked")
		@Override
		public <T extends ByteBuf> void stream(Publisher<T> data) {
			GenericResponseBody.this.setData((Publisher<ByteBuf>) data);
		}

		@Override
		public <T extends ByteBuf> void value(T data) {
			this.stream(Mono.just(data));
		}
	}
	
	protected class GenericResponseBodyResourceData implements ResponseBody.Resource {

		protected void populateHeaders(io.winterframework.mod.base.resource.Resource resource) {
			GenericResponseBody.this.response.headers(h -> {
				if(GenericResponseBody.this.response.headers().getContentLength() == null) {
					resource.size().ifPresent(h::contentLength);
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
		public void value(io.winterframework.mod.base.resource.Resource resource) {
			// Http2 doesn't support FileRegion so we have to read the resource and send it to the response data flux
			if(resource.exists().orElse(true)) {
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

	protected class GenericResponseBodySseData implements ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> {
		
		@Override
		public void from(BiConsumer<ResponseBody.Sse.EventFactory<ByteBuf, Event<ByteBuf>>, ResponseData<ResponseBody.Sse.Event<ByteBuf>>> data) {
			data.accept(this::create, this::stream);
		}
		
		protected <T extends ResponseBody.Sse.Event<ByteBuf>> void stream(Publisher<T> value) {
			GenericResponseBody.this.response.headers(headers -> headers
				.contentType(GenericResponseBody.SSE_CONTENT_TYPE)
			);
			
			GenericResponseBody.this.setData(Flux.from(value)
				.cast(GenericEvent.class)
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
					if(sse.getData() != null) {
						sseMetaData.append("data:");
					}
					
					Flux<ByteBuf> sseData = Flux.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(sseMetaData, Charsets.UTF_8)));
					
					if(sse.getData() != null) {
						sseData = sseData
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
		
		protected ResponseBody.Sse.Event<ByteBuf> create(Consumer<ResponseBody.Sse.Event<ByteBuf>> configurer) {
			GenericEvent sse = new GenericEvent();
			configurer.accept(sse);
			return sse;
		}
		
		protected final class GenericEvent implements ResponseBody.Sse.Event<ByteBuf> {
			
			private String id;
			
			private String comment;
			
			private String event;
			
			private Publisher<ByteBuf> data;

			@SuppressWarnings("unchecked")
			@Override
			public <T extends ByteBuf> void stream(Publisher<T> data) {
				this.data = (Publisher<ByteBuf>) data;
			}

			@Override
			public <T extends ByteBuf> void value(T data) {
				this.data = Mono.just(data);
			}

			@Override
			public GenericEvent id(String id) {
				this.id = id;
				return this;
			}

			@Override
			public GenericEvent comment(String comment) {
				this.comment = comment;
				return this;
			}

			@Override
			public GenericEvent event(String event) {
				this.event = event;
				return this;
			}
			
			public String getId() {
				return id;
			}
			
			public String getComment() {
				return comment;
			}
			
			public String getEvent() {
				return event;
			}
			
			public Publisher<ByteBuf> getData() {
				return data;
			}
		}
	}
}
