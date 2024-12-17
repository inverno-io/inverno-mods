/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.http.base.internal.ws;

import io.inverno.mod.http.base.ws.WebSocketFrame;
import io.inverno.mod.http.base.ws.WebSocketMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.CharsetUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link WebSocketMessage} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericWebSocketMessage implements WebSocketMessage {

	private final WebSocketMessage.Kind kind;
	
	private final Publisher<WebSocketFrame> frames;

	/**
	 * <p>
	 * Creates a generic WebSocket message.
	 * </p>
	 *
	 * @param kind   the message type
	 * @param frames the frames that composes the message
	 */
	public GenericWebSocketMessage(Kind kind, Publisher<WebSocketFrame> frames) {
		this.kind = kind;
		this.frames = frames;
	}
	
	@Override
	public Kind getKind() {
		return this.kind;
	}

	@Override
	public Publisher<WebSocketFrame> frames() {
		return this.frames;
	}

	@Override
	public Publisher<ByteBuf> raw() {
		return Flux.from(this.frames).map(WebSocketFrame::getRawData);
	}

	@Override
	public Mono<ByteBuf> rawReduced() {
		return Flux.from(this.raw()).reduceWith(
			() -> Unpooled.unreleasableBuffer(Unpooled.buffer()), 
			(acc, chunk) -> {
				try {
					return acc.writeBytes(chunk);
				}
				finally {
					chunk.release();
				}
			});
	}
	
	@Override
	public Publisher<String> string() {
		return Flux.from(this.frames).map(frame -> {
			try {
				return frame.getStringData();
			}
			finally {
				frame.release();
			}
		});
	}

	@Override
	public Mono<String> stringReduced() {
		return Flux.from(this.string()).reduceWith(
			StringBuilder::new,
			StringBuilder::append
		).map(StringBuilder::toString);
	}
	
	/**
	 * <p>
	 * Generic {@link WebSocketMessage.Factory} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	public static final class GenericFactory implements WebSocketMessage.Factory {

		/**
		 * The maximum frame size.
		 */
		private final int maxFrameSize;
		
		/**
		 * <p>
		 * Creates a generic WebSocket message factory.
		 * </p>
		 * 
		 * @param maxFrameSize the maximum size of a frame
		 */
		public GenericFactory(int maxFrameSize) {
			this.maxFrameSize = maxFrameSize;
		}

		@Override
		public WebSocketMessage text(String value) {
			return new GenericWebSocketMessage(WebSocketMessage.Kind.TEXT, Flux.fromStream(() -> this.toTextFrames(Unpooled.copiedBuffer(value, CharsetUtil.UTF_8), true, true).stream()));
		}
		
		@Override
		public WebSocketMessage text(Publisher<String> stream) {
			Flux<WebSocketFrame> frames;
			if(stream instanceof Mono) {
				frames = ((Mono<String>)stream)
					.flatMapIterable(value -> this.toTextFrames(Unpooled.copiedBuffer(value, CharsetUtil.UTF_8), true, true));
			}
			else {
				frames = Flux.defer(() -> {
					AtomicBoolean first = new AtomicBoolean(true);
					return Flux.from(stream)
						.flatMapIterable(value -> this.toTextFrames(Unpooled.copiedBuffer(value, CharsetUtil.UTF_8), first.getAndSet(false), false))
						.concatWithValues(new GenericWebSocketFrame(new ContinuationWebSocketFrame(true, 0, Unpooled.EMPTY_BUFFER), WebSocketFrame.Kind.CONTINUATION));
				});
			}
			return new GenericWebSocketMessage(WebSocketMessage.Kind.TEXT, frames);
		}

		@Override
		public WebSocketMessage text_raw(ByteBuf value) {
			return new GenericWebSocketMessage(WebSocketMessage.Kind.TEXT, Flux.fromStream(() -> this.toTextFrames(value, true, true).stream()));
		}

		@Override
		public WebSocketMessage text_raw(Publisher<ByteBuf> stream) {
			Flux<WebSocketFrame> frames;
			if(stream instanceof Mono) {
				frames = ((Mono<ByteBuf>)stream)
					.flatMapIterable(value -> this.toTextFrames(value, true, true));
			}
			else {
				frames = Flux.defer(() -> {
					AtomicBoolean first = new AtomicBoolean(true);
					return Flux.from(stream)
						.flatMapIterable(value -> this.toTextFrames(value, first.getAndSet(false), false))
						.concatWithValues(new GenericWebSocketFrame(new ContinuationWebSocketFrame(true, 0, Unpooled.EMPTY_BUFFER), WebSocketFrame.Kind.CONTINUATION));
				});
			}
			return new GenericWebSocketMessage(WebSocketMessage.Kind.TEXT, frames);
		}
		
		@Override
		public WebSocketMessage binary(ByteBuf value) {
			return new GenericWebSocketMessage(WebSocketMessage.Kind.BINARY, Flux.fromStream(() -> this.toBinaryFrames(value, true, true).stream()));
		}
		
		@Override
		public WebSocketMessage binary(Publisher<ByteBuf> stream) {
			Flux<WebSocketFrame> frames;
			if(stream instanceof Mono) {
				frames = ((Mono<ByteBuf>)stream)
					.flatMapIterable(value -> this.toBinaryFrames(value, true, true));
			}
			else {
				frames = Flux.defer(() -> {
					AtomicBoolean first = new AtomicBoolean(true);
					return Flux.from(stream)
						.flatMapIterable(value -> this.toBinaryFrames(value, first.getAndSet(false), false))
						.concatWithValues(new GenericWebSocketFrame(new ContinuationWebSocketFrame(true, 0, Unpooled.EMPTY_BUFFER), WebSocketFrame.Kind.CONTINUATION));
				});
			}
			return new GenericWebSocketMessage(WebSocketMessage.Kind.TEXT, frames);
		}
		
		/**
		 * <p>
		 * Converts the specified data into a list of WebSocket text frames, splitting the data into multiple frames when they exceeds the maximum frame size.
		 * </p>
		 * 
		 * @param data    the data to convert
		 * @param isFirst true if the specified data are the first part of the message, false otherwise
		 * @param isFinal true if the specified data are the last part of the message, false otherwise
		 * 
		 * @return a list of WebSocket text frames
		 */
		private List<WebSocketFrame> toTextFrames(ByteBuf data, boolean isFirst, boolean isFinal) {
			int size = data.readableBytes();
			if(size > this.maxFrameSize) {
				final int framesCount = (int)Math.ceil((double)size / (double)this.maxFrameSize);
				List<WebSocketFrame> frames = new ArrayList<>(framesCount);
				for(int i=0;i<framesCount;i++) {
					int offset = i * this.maxFrameSize;
					int length = Math.min(this.maxFrameSize, size - offset);
					ByteBuf framePayload = data.retainedSlice(offset, length);
					if(isFirst && i == 0) {
						frames.add(new GenericWebSocketFrame(new TextWebSocketFrame(false, 0, framePayload), WebSocketFrame.Kind.TEXT));
					}
					else {
						frames.add(new GenericWebSocketFrame(new ContinuationWebSocketFrame(isFinal && i == framesCount - 1, 0, framePayload), WebSocketFrame.Kind.CONTINUATION));
					}
				}
				return frames;
			}
			else {
				if(isFirst) {
					return List.of(new GenericWebSocketFrame(new TextWebSocketFrame(isFinal, 0, data), WebSocketFrame.Kind.TEXT));
				}
				else {
					return List.of(new GenericWebSocketFrame(new ContinuationWebSocketFrame(isFinal, 0, data), WebSocketFrame.Kind.TEXT));
				}
			}
		}
		
		/**
		 * <p>
		 * Converts the specified data into a list of WebSocket binary frames, splitting the data into multiple frames when they exceeds the maximum frame size.
		 * </p>
		 * 
		 * @param data    the data to convert
		 * @param isFirst true if the specified data are the first part of the message, false otherwise
		 * @param isFinal true if the specified data are the last part of the message, false otherwise
		 * 
		 * @return a list of WebSocket binary frames
		 */
		private List<WebSocketFrame> toBinaryFrames(ByteBuf data, boolean isFirst, boolean isFinal) {
			int size = data.readableBytes();
			if(size > this.maxFrameSize) {
				final int framesCount = (int)Math.ceil((double)size / (double)this.maxFrameSize);
				List<WebSocketFrame> frames = new ArrayList<>(framesCount);
				for(int i=0;i<framesCount;i++) {
					int offset = i * this.maxFrameSize;
					int length = Math.min(this.maxFrameSize, size - offset);
					ByteBuf framePayload = data.retainedSlice(offset, length);
					if(isFirst && i == 0) {
						frames.add(new GenericWebSocketFrame(new BinaryWebSocketFrame(false, 0, framePayload), WebSocketFrame.Kind.TEXT));
					}
					else {
						frames.add(new GenericWebSocketFrame(new ContinuationWebSocketFrame(isFinal && i == framesCount - 1, 0, framePayload), WebSocketFrame.Kind.CONTINUATION));
					}
				}
				return frames;
			}
			else {
				if(isFirst) {
					return List.of(new GenericWebSocketFrame(new BinaryWebSocketFrame(isFinal, 0, data), WebSocketFrame.Kind.TEXT));
				}
				else {
					return List.of(new GenericWebSocketFrame(new ContinuationWebSocketFrame(isFinal, 0, data), WebSocketFrame.Kind.TEXT));
				}
			}
		}
	}
}
