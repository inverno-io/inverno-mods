/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.http.server.internal;

import io.inverno.mod.base.Charsets;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.http.base.OutboundData;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ResponseBody;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpConstants;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

/**
 * <p>
 * Base {@link ResponseBody} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.10
 * 
 * @param <A> the response headers type
 * @param <B> the response body type
 */
public abstract class AbstractResponseBody<A extends AbstractResponseHeaders<?>, B extends AbstractResponseBody<A, B>> implements ResponseBody {
	
	/**
	 * Server Sent Events content type with default encoding.
	 */
	protected static final String SSE_CONTENT_TYPE = MediaTypes.TEXT_EVENT_STREAM + ";charset=" + Charsets.DEFAULT.displayName();

	/**
	 * The response headers.
	 */
	protected final A headers;
	
	/**
	 * The raw outbound data.
	 */
	protected OutboundData<ByteBuf> rawData;
	/**
	 * The string outbound data.
	 */
	protected OutboundData<CharSequence> stringData;
	/**
	 * The resource data.
	 */
	protected ResponseBody.Resource resourceData;
	/**
	 * The Server Sent Events raw data.
	 */
	protected ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> sseData;
	/**
	 * The Server Sent Events string data.
	 */
	protected ResponseBody.Sse<CharSequence, ResponseBody.Sse.Event<CharSequence>, ResponseBody.Sse.EventFactory<CharSequence, ResponseBody.Sse.Event<CharSequence>>> sseStringData;
	
	private MonoSink<Publisher<ByteBuf>> dataEmitter;
	private Publisher<ByteBuf> data;
	private boolean dataSet;
	private boolean subscribed;
	private Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer;

	/**
	 * <p>
	 * Creates a base response body.
	 * </p>
	 * 
	 * @param headers the response headers
	 */
	public AbstractResponseBody(A headers) {
		this.headers = headers;
	}
	
	/**
	 * <p>
	 * Sets response body data.
	 * </p>
	 * 
	 * <p>
	 * This method makes sure any pending buffer is released when the data publisher subscription is cancelled.
	 * </p>
	 * 
	 * @param data the response body data publisher
	 * 
	 * @throws IllegalStateException if response data have already been set
	 */
	protected void setData(Publisher<ByteBuf> data) throws IllegalStateException {
		if(this.subscribed && this.dataSet) {
			throw new IllegalStateException("Response data already sent");
		}
		
		Publisher<ByteBuf> transformedData = this.transformer != null ? this.transformer.apply(data) : data;
		if(transformedData instanceof Mono) {
			transformedData = ((Mono<ByteBuf>)transformedData)
				.doOnSubscribe(ign -> this.subscribed = true)
				.doOnDiscard(ByteBuf.class, ByteBuf::release);
		}
		else {
			transformedData = Flux.from(transformedData)
				.doOnSubscribe(ign -> this.subscribed = true)
				.doOnDiscard(ByteBuf.class, ByteBuf::release);
		}
		
		if(this.dataEmitter != null) {
			this.dataEmitter.success(transformedData);
		}
		else {
			this.data = transformedData;
		}
		this.dataSet = true;
	}
	
	/**
	 * <p>
	 * Returns the response body data publisher.
	 * </p>
	 * 
	 * <p>
	 * The data publisher MUST be subscribed to produce a response body.
	 * </p>
	 * 
	 * @return the response body data publisher
	 */
	public final Publisher<ByteBuf> getData() {
		if(this.data == null) {
			this.data = Flux.switchOnNext(Mono.<Publisher<ByteBuf>>create(emitter -> this.dataEmitter = emitter));
		}
		return this.data;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public final B transform(Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer) throws IllegalArgumentException {
		if(this.subscribed && this.dataSet) {
			throw new IllegalStateException("Response data already sent");
		}

		if(this.transformer == null) {
			this.transformer = transformer;
		}
		else {
			this.transformer = this.transformer.andThen(transformer);
		}

		if(this.dataSet) {
			this.data = transformer.apply(this.data);
		}
		return (B)this;
	}
	
	@Override
	public void empty() {
		this.setData(Mono.just(Unpooled.EMPTY_BUFFER));
	}
	
	@Override
	public OutboundData<ByteBuf> raw() {
		if(this.rawData == null) {
			this.rawData = new RawOutboundData();
		}
		return this.rawData;
	}

	/**
	 * <p>
	 * Generic raw {@link OutboundData} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.10
	 */
	protected class RawOutboundData implements OutboundData<ByteBuf> {

		@SuppressWarnings("unchecked")
		@Override
		public <T extends ByteBuf> void stream(Publisher<T> data) {
			AbstractResponseBody.this.setData((Publisher<ByteBuf>) data);
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T extends CharSequence> OutboundData<T> string() {
		if(this.stringData == null) {
			this.stringData = new StringOutboundData();
		}
		return (OutboundData<T>)this.stringData;
	}
	
	/**
	 * <p>
	 * Generic string {@link OutboundData} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.10
	 */
	protected class StringOutboundData implements OutboundData<CharSequence> {

		@Override
		public <T extends CharSequence> void stream(Publisher<T> value) throws IllegalStateException {
			Publisher<ByteBuf> data;
			if(value instanceof Mono) {
				data = ((Mono<T>)value).map(chunk -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(chunk, Charsets.DEFAULT)));
			}
			else {
				data = Flux.from(value).map(chunk -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(chunk, Charsets.DEFAULT)));
			}
			AbstractResponseBody.this.setData(data);
		}
		
		@Override
		public <T extends CharSequence> void value(T value) throws IllegalStateException {
			AbstractResponseBody.this.setData(value != null ? Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value, Charsets.DEFAULT))) : Mono.empty());
		}
	}
	
	@Override
	public ResponseBody.Resource resource() {
		if(this.resourceData == null) {
			this.resourceData = new ResourceOutboundData();
		}
		return this.resourceData;
	}
	
	/**
	 * <p>
	 * Generic {@link ResponseBody.Resource} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.10
	 */
	protected class ResourceOutboundData implements ResponseBody.Resource {

		/**
		 * <p>
		 * Tries to determine resource content type in which case sets the content type
		 * header.
		 * </p>
		 * 
		 * @param resource the resource
		 */
		protected void populateHeaders(io.inverno.mod.base.resource.Resource resource) {
			if(AbstractResponseBody.this.headers.getContentLength() == null) {
				resource.size().ifPresent(AbstractResponseBody.this.headers::contentLength);
			}
			
			if(!AbstractResponseBody.this.headers.contains(Headers.NAME_CONTENT_TYPE)) {
				String mediaType = resource.getMediaType();
				if(mediaType != null) {
					AbstractResponseBody.this.headers.contentType(mediaType);
				}
			}
			
			if(!AbstractResponseBody.this.headers.contains(Headers.NAME_LAST_MODIFIED)) {
				resource.lastModified().ifPresent(lastModified -> AbstractResponseBody.this.headers.set(Headers.NAME_LAST_MODIFIED, Headers.FORMATTER_RFC_5322_DATE_TIME.format(lastModified.toInstant())));
			}
		}
		
		@Override
		public void value(io.inverno.mod.base.resource.Resource resource) {
			Objects.requireNonNull(resource);
			// In case of file resources we should always be able to determine existence
			// For other resources with a null exists we can still try, worst case scenario: 
			// internal server error
			if(resource.exists().orElse(true)) {
				this.populateHeaders(resource);
				AbstractResponseBody.this.setData(resource.read());
			}
			else {
				throw new NotFoundException();
			}
		}
	}
	
	@Override
	public ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> sse() {
		if(this.sseData == null) {
			this.sseData = new SseRawOutboundData();
		}
		return this.sseData;
	}
	
	/**
	 * <p>
	 * Generic raw {@link ResponseBody.Sse} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.10
	 */
	protected class SseRawOutboundData implements ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> {
		
		@Override
		public void from(BiConsumer<ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>, OutboundData<ResponseBody.Sse.Event<ByteBuf>>> data) {
			data.accept(this::create, this::stream);
		}
		
		/**
		 * <p>
		 * Raw server-sent events producer.
		 * </p>
		 * 
		 * @param <T>   the server-sent event type
		 * @param value the server-sent events publisher
		 */
		protected <T extends ResponseBody.Sse.Event<ByteBuf>> void stream(Publisher<T> value) {
			AbstractResponseBody.this.headers.contentType(AbstractResponseBody.SSE_CONTENT_TYPE);
			AbstractResponseBody.this.setData(Flux.from(value)
				.flatMapSequential(sse -> {
					StringBuilder sseMetaData = new StringBuilder();
					if(((GenericEvent)sse).getId() != null) {
						sseMetaData.append("id:").append(((GenericEvent)sse).getId()).append("\n");
					}
					if(((GenericEvent)sse).getEvent() != null) {
						sseMetaData.append("event:").append(((GenericEvent)sse).getEvent()).append("\n");
					}
					if(((GenericEvent)sse).getComment() != null) {
						sseMetaData.append(":").append(((GenericEvent)sse).getComment().replaceAll("\\r\\n|\\r|\\n", "\r\n:")).append("\n");
					}
					if(((GenericEvent)sse).getData() != null) {
						sseMetaData.append("data:");
					}
					
					Flux<ByteBuf> sseData = Flux.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(sseMetaData, Charsets.UTF_8)));
					
					if(((GenericEvent)sse).getData() != null) {
						sseData = sseData
							.concatWith(Flux.from(((GenericEvent)sse).getData())
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
		
		/**
		 * <p>
		 * Raw server-sent events factory.
		 * </p>
		 * 
		 * @param configurer a raw server-sent event configurer
		 * 
		 * @return a new raw server-sent event
		 */
		protected ResponseBody.Sse.Event<ByteBuf> create(Consumer<ResponseBody.Sse.Event<ByteBuf>> configurer) {
			GenericEvent sse = new GenericEvent();
			configurer.accept(sse);
			return sse;
		}
		
		/**
		 * <p>
		 * Generic raw {@link ResponseBody.Sse.Event} implementation.
		 * </p>
		 * 
		 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
		 * @since 1.10
		 */
		protected static final class GenericEvent implements ResponseBody.Sse.Event<ByteBuf> {
			
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
	
	@Override
	@SuppressWarnings("unchecked")
	public ResponseBody.Sse<CharSequence, ResponseBody.Sse.Event<CharSequence>, ResponseBody.Sse.EventFactory<CharSequence, ResponseBody.Sse.Event<CharSequence>>> sseString() {
		if(this.sseStringData == null) {
			this.sseStringData = new SseStringOutboundData();
		}
		return this.sseStringData;
	}
	
	/**
	 * <p>
	 * Generic string {@link ResponseBody.Sse} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.10
	 */
	protected class SseStringOutboundData implements ResponseBody.Sse<CharSequence, ResponseBody.Sse.Event<CharSequence>, ResponseBody.Sse.EventFactory<CharSequence, ResponseBody.Sse.Event<CharSequence>>> {

		@Override
		public void from(BiConsumer<ResponseBody.Sse.EventFactory<CharSequence, Sse.Event<CharSequence>>, OutboundData<ResponseBody.Sse.Event<CharSequence>>> data) {
			data.accept(this::create, this::stream);
		}
		
		/**
		 * <p>
		 * String server-sent events producer.
		 * </p>
		 * 
		 * @param <T>   the server-sent event type
		 * @param value the server-sent events publisher
		 */
		protected <T extends ResponseBody.Sse.Event<CharSequence>> void stream(Publisher<T> value) {
			AbstractResponseBody.this.headers.contentType(AbstractResponseBody.SSE_CONTENT_TYPE);
			AbstractResponseBody.this.setData(Flux.from(value)
				.flatMapSequential(sse -> {
					StringBuilder sseMetaData = new StringBuilder();
					if(((GenericEvent)sse).getId() != null) {
						sseMetaData.append("id:").append(((GenericEvent)sse).getId()).append("\n");
					}
					if(((GenericEvent)sse).getEvent() != null) {
						sseMetaData.append("event:").append(((GenericEvent)sse).getEvent()).append("\n");
					}
					if(((GenericEvent)sse).getComment() != null) {
						sseMetaData.append(":").append(((GenericEvent)sse).getComment().replaceAll("\\r\\n|\\r|\\n", "\r\n:")).append("\n");
					}
					if(((GenericEvent)sse).getData() != null) {
						sseMetaData.append("data:");
					}
					
					Flux<ByteBuf> sseData = Flux.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(sseMetaData, Charsets.UTF_8)));
					
					if(((GenericEvent)sse).getData() != null) {
						sseData = sseData
							.concatWith(Flux.from(((GenericEvent)sse).getData())
								.map(chunk -> {
									ByteBuf escapedChunk = Unpooled.unreleasableBuffer(Unpooled.buffer(chunk.length(), Integer.MAX_VALUE));
									for(int i=0;i<chunk.length();i++) {
										char nextChar = chunk.charAt(i);
										if(nextChar == HttpConstants.CR) {
											if(i < chunk.length() - 1 && chunk.charAt(i+1) == HttpConstants.LF) {
												i++;
											}
											escapedChunk.writeCharSequence("\r\ndata:", Charsets.UTF_8);
										}
										else if(nextChar == HttpConstants.LF) {
											escapedChunk.writeCharSequence("\r\ndata:", Charsets.UTF_8);
										}
										else {
											escapedChunk.writeByte(nextChar);
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
		
		/**
		 * <p>
		 * String server-sent events factory.
		 * </p>
		 * 
		 * @param configurer a string server-sent event configurer
		 * 
		 * @return a new string server-sent event
		 */
		protected ResponseBody.Sse.Event<CharSequence> create(Consumer<ResponseBody.Sse.Event<CharSequence>> configurer) {
			GenericEvent sse = new GenericEvent();
			configurer.accept(sse);
			return sse;
		}
		
		/**
		 * <p>
		 * Generic string {@link ResponseBody.Sse.Event} implementation.
		 * </p>
		 * 
		 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
		 * @since 1.10
		 */
		protected static final class GenericEvent implements ResponseBody.Sse.Event<CharSequence> {
			
			private String id;
			
			private String comment;
			
			private String event;
			
			private Publisher<CharSequence> data;

			@SuppressWarnings("unchecked")
			@Override
			public <T extends CharSequence> void stream(Publisher<T> data) {
				this.data = (Publisher<CharSequence>) data;
			}

			@Override
			public <T extends CharSequence> void value(T data) {
				this.data = Mono.justOrEmpty(data);
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
			
			public Publisher<CharSequence> getData() {
				return data;
			}
		}
	}
}
