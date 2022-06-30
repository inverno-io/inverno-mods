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
package io.inverno.mod.http.server.internal.http1x.ws;

import io.inverno.mod.http.server.HttpServerConfiguration;
import io.inverno.mod.http.server.ws.WebSocketFrame;
import io.inverno.mod.http.server.ws.WebSocketMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.CharsetUtil;
import java.util.ArrayList;
import java.util.Arrays;
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
	public Publisher<ByteBuf> binary() {
		return Flux.from(this.frames).map(WebSocketFrame::getBinaryData);
	}

	@Override
	public Publisher<String> text() {
		return Flux.from(this.frames).map(frame -> {
			try {
				return frame.getTextData();
			}
			finally {
				frame.release();
			}
		});
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

		private final int maxFrameSize;
		
		/**
		 * <p>
		 * Creates a generic WebSocket message factory.
		 * </p>
		 * 
		 * @param configuration the server configuration
		 */
		public GenericFactory(HttpServerConfiguration configuration) {
			this.maxFrameSize = configuration.ws_max_frame_size();
		}

		@Override
		public WebSocketMessage text(Publisher<String> value) {
			Flux<WebSocketFrame> frames;
			if(value instanceof Mono) {
				frames = ((Mono<String>)value)
					.flatMapIterable(text -> {
						ByteBuf[] splitPayload = this.splitData(Unpooled.copiedBuffer(text, CharsetUtil.UTF_8));
						if(splitPayload.length == 1) {
							return List.of((WebSocketFrame)new GenericWebSocketFrame(new TextWebSocketFrame(true, 0, splitPayload[0]), WebSocketFrame.Kind.TEXT));
						}
						else {
							List<WebSocketFrame> payloadFrames = new ArrayList<>(splitPayload.length);
							for(int i = 0;i < splitPayload.length;i++) {
								WebSocketFrame frame;
								if(i == 0) {
									frame = new GenericWebSocketFrame(new TextWebSocketFrame(false, 0, splitPayload[i]), WebSocketFrame.Kind.TEXT);
								}
								else if(i == splitPayload.length - 1) {
									frame = new GenericWebSocketFrame(new ContinuationWebSocketFrame(false, 0, splitPayload[i]), WebSocketFrame.Kind.CONTINUATION);
								}
								else {
									frame = new GenericWebSocketFrame(new ContinuationWebSocketFrame(true, 0, splitPayload[i]), WebSocketFrame.Kind.CONTINUATION);
								}
								payloadFrames.add(frame);
							}
							return payloadFrames;
						}
					});
			}
			else {
				frames = Flux.defer(() -> {
					AtomicBoolean first = new AtomicBoolean(true);
					return Flux.from(value)
						.flatMapIterable(text -> Arrays.asList(this.splitData(Unpooled.copiedBuffer(text, CharsetUtil.UTF_8))))
						.map(chunk -> {
							if(first.get()) {
								first.set(false);
								return (WebSocketFrame)new GenericWebSocketFrame(new TextWebSocketFrame(false, 0, chunk), WebSocketFrame.Kind.TEXT);
							}
							else {
								return (WebSocketFrame)new GenericWebSocketFrame(new ContinuationWebSocketFrame(false, 0, chunk), WebSocketFrame.Kind.CONTINUATION);
							}
						})
						.concatWithValues(new GenericWebSocketFrame(new ContinuationWebSocketFrame(true, 0, Unpooled.EMPTY_BUFFER), WebSocketFrame.Kind.CONTINUATION));
				});
			}
			return new GenericWebSocketMessage(WebSocketMessage.Kind.TEXT, frames);
		}
		
		@Override
		public WebSocketMessage binary(Publisher<ByteBuf> value) {
			Flux<WebSocketFrame> frames;
			if(value instanceof Mono) {
				frames = ((Mono<ByteBuf>)value)
					.flatMapIterable(data -> {
						ByteBuf[] splitPayload = this.splitData(data);
						if(splitPayload.length == 1) {
							return List.of((WebSocketFrame)new GenericWebSocketFrame(new BinaryWebSocketFrame(true, 0, splitPayload[0]), WebSocketFrame.Kind.BINARY));
						}
						else {
							List<WebSocketFrame> payloadFrames = new ArrayList<>(splitPayload.length);
							for(int i = 0;i < splitPayload.length;i++) {
								WebSocketFrame frame;
								if(i == 0) {
									frame = new GenericWebSocketFrame(new BinaryWebSocketFrame(false, 0, splitPayload[i]), WebSocketFrame.Kind.BINARY);
								}
								else if(i == splitPayload.length - 1) {
									frame = new GenericWebSocketFrame(new ContinuationWebSocketFrame(false, 0, splitPayload[i]), WebSocketFrame.Kind.CONTINUATION);
								}
								else {
									frame = new GenericWebSocketFrame(new ContinuationWebSocketFrame(true, 0, splitPayload[i]), WebSocketFrame.Kind.CONTINUATION);
								}
								payloadFrames.add(frame);
							}
							return payloadFrames;
						}
					});
			}
			else {
				frames = Flux.defer(() -> {
					AtomicBoolean first = new AtomicBoolean(true);
					return Flux.from(value)
						.flatMapIterable(data -> Arrays.asList(this.splitData(data)))
						.map(chunk -> {
							if(first.get()) {
								first.set(false);
								return (WebSocketFrame)new GenericWebSocketFrame(new BinaryWebSocketFrame(false, 0, chunk), WebSocketFrame.Kind.BINARY);
							}
							else {
								return (WebSocketFrame)new GenericWebSocketFrame(new ContinuationWebSocketFrame(false, 0, chunk), WebSocketFrame.Kind.CONTINUATION);
							}
						})
						.concatWithValues(new GenericWebSocketFrame(new ContinuationWebSocketFrame(true, 0, Unpooled.EMPTY_BUFFER), WebSocketFrame.Kind.CONTINUATION));
				});
			}
			return new GenericWebSocketMessage(WebSocketMessage.Kind.BINARY, frames);
		}
		
		/**
		 * <p>
		 * Splits the specified buffer if it is bigger than the maximum frame size.
		 * </p>
		 * 
		 * @param data the data to consider
		 * 
		 * @return an array of data
		 */
		private ByteBuf[] splitData(ByteBuf data) {
			int size = data.readableBytes();
			if(size > this.maxFrameSize) {
				int framesCount = (int)Math.ceil((double)size / (double)this.maxFrameSize);
				
				ByteBuf[] splitData = new ByteBuf[framesCount];
				for(int i=0;i<framesCount;i++) {
					int offset = i * this.maxFrameSize;
					int length = Math.min(this.maxFrameSize, size - offset);
					splitData[i] = data.slice(offset, length);
				}
				return splitData;
			}
			return new ByteBuf[] { data };
		}
	}
}
