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

import io.inverno.mod.base.Charsets;
import io.inverno.mod.http.base.ws.WebSocketException;
import io.inverno.mod.http.base.ws.WebSocketFrame;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.CharsetUtil;

/**
 * <p>
 * Generic {@link WebSocketFrame} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericWebSocketFrame implements WebSocketFrame {
	
	/**
	 * The kind of frame (e.g. TEXT or BINARY)
	 */
	private final WebSocketFrame.Kind kind;
	
	/**
	 * The underlying Netty frame
	 */
	private final io.netty.handler.codec.http.websocketx.WebSocketFrame underlyingFrame;
	
	/**
	 * <p>
	 * Creates a generic WebSocket frame.
	 * </p>
	 * 
	 * <p>
	 * The frame type is determined from the specified underlying frame.
	 * </p>
	 * 
	 * @param underlyingFrame the underlying Netty WebSocket frame
	 */
	public GenericWebSocketFrame(io.netty.handler.codec.http.websocketx.WebSocketFrame underlyingFrame) {
		this(underlyingFrame, getKind(underlyingFrame));
	}
	
	/**
	 * <p>
	 * Creates a generic WebSocket frame.
	 * </p>
	 * 
	 * <p>
	 * This does not verify the specified kind which must correspond to the type of the specified underlying frame.
	 * </p>
	 * 
	 * @param underlyingFrame the underlying Netty WebSocket frame
	 * @param kind            the frame type
	 */
	public GenericWebSocketFrame(io.netty.handler.codec.http.websocketx.WebSocketFrame underlyingFrame, WebSocketFrame.Kind kind) {
		this.underlyingFrame = underlyingFrame;
		this.kind = kind;
	}
	
	/**
	 * <p>
	 * Returns the frame type corresponding to the specified frame.
	 * </p>
	 * 
	 * @param underlyingFrame an underlying Netty WebSocket frame
	 * 
	 * @return a frame type
	 * 
	 * @throws IllegalArgumentException if the specified frame does not correspond to a known type
	 */
	private static WebSocketFrame.Kind getKind(io.netty.handler.codec.http.websocketx.WebSocketFrame underlyingFrame) throws IllegalArgumentException {
		if(underlyingFrame instanceof TextWebSocketFrame) {
			return Kind.TEXT;
		}
		else if(underlyingFrame instanceof BinaryWebSocketFrame) {
			return Kind.BINARY;
		}
		else if(underlyingFrame instanceof ContinuationWebSocketFrame) {
			return Kind.CONTINUATION;
		}
		else if(underlyingFrame instanceof PingWebSocketFrame) {
			return Kind.PING;
		}
		else if(underlyingFrame instanceof PongWebSocketFrame) {
			return Kind.PONG;
		}
		else if(underlyingFrame instanceof CloseWebSocketFrame) {
			return Kind.CLOSE;
		}
		else {
			throw new IllegalArgumentException("Unsupported frame type: " + underlyingFrame.getClass());
		}
	}
	
	/**
	 * <p>
	 * Returns the underlying frame.
	 * </p>
	 * 
	 * @return the underlying Netty frame
	 */
	public io.netty.handler.codec.http.websocketx.WebSocketFrame getUnderlyingFrame() {
		return underlyingFrame;
	}

	@Override
	public Kind getKind() {
		return this.kind;
	}

	@Override
	public boolean isFinal() {
		return this.underlyingFrame.isFinalFragment();
	}

	@Override
	public ByteBuf getBinaryData() {
		return this.underlyingFrame.content();
	}

	@Override
	public String getTextData() {
		return this.underlyingFrame.content().toString(Charsets.UTF_8);
	}

	@Override
	public WebSocketFrame retain() {
		this.underlyingFrame.retain();
		return this;
	}

	@Override
	public WebSocketFrame retainedDuplicate() {
		return new GenericWebSocketFrame(this.underlyingFrame.retainedDuplicate(), this.kind);
	}

	@Override
	public WebSocketFrame release() {
		this.underlyingFrame.release();
		return this;
	}

	@Override
	public int refCnt() {
		return this.underlyingFrame.refCnt();
	}
	
	/**
	 * <p>
	 * Generic {@link WebSocketFrame.Factory} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	public static class GenericFactory implements WebSocketFrame.Factory {

		private final int maxFrameSize;
		
		/**
		 * <p>
		 * Creates a generic WebSocker frame factory.
		 * </p>
		 * 
		 * @param maxFrameSize the maximum size of a frame
		 */
		public GenericFactory(int maxFrameSize) {
			this.maxFrameSize = maxFrameSize;
		}
		
		/**
		 * <p>
		 * Checks that the specified payload data is valid for the specified frame type according to the configuration.
		 * </p>
		 *
		 * @param payload the payload data to test
		 * @param kind    the frame type
		 *
		 * @return the payload data
		 *
		 * @throws WebSocketException if the specified data are invalid according to the configuration
		 */
		private ByteBuf checkPayload(ByteBuf payload, WebSocketFrame.Kind kind) throws WebSocketException {
			if(payload == null) {
				return Unpooled.EMPTY_BUFFER;
			}
			
			int payloadLength = payload.readableBytes();
			if(payloadLength > this.maxFrameSize) {
				throw new WebSocketException("Max frame size exceeded: " + this.maxFrameSize);
			}
			
			// RFC6455 Section 5.5
			if((kind == Kind.PING || kind == Kind.PONG || kind == Kind.CLOSE) && payloadLength > 125) {
				throw new WebSocketException("Max control frame size exceeded: 125");
			}
			
			return payload;
		}
		
		@Override
		public WebSocketFrame binary(ByteBuf data, boolean finalFragment) throws WebSocketException {
			return new GenericWebSocketFrame(new BinaryWebSocketFrame(finalFragment, 0, this.checkPayload(data, WebSocketFrame.Kind.BINARY)), WebSocketFrame.Kind.BINARY);
		}

		@Override
		public WebSocketFrame text(ByteBuf data, boolean finalFragment) throws WebSocketException {
			return new GenericWebSocketFrame(new TextWebSocketFrame(finalFragment, 0, this.checkPayload(data, WebSocketFrame.Kind.TEXT)), WebSocketFrame.Kind.TEXT);
		}

		@Override
		public WebSocketFrame continuation(ByteBuf data, boolean finalFragment) throws WebSocketException {
			return new GenericWebSocketFrame(new ContinuationWebSocketFrame(finalFragment, 0, this.checkPayload(data, WebSocketFrame.Kind.CONTINUATION)), WebSocketFrame.Kind.CONTINUATION);
		}

		@Override
		public WebSocketFrame ping(ByteBuf data) throws WebSocketException {
			return new GenericWebSocketFrame(new PingWebSocketFrame(this.checkPayload(data, WebSocketFrame.Kind.PING)), WebSocketFrame.Kind.PING);
		}

		@Override
		public WebSocketFrame pong(ByteBuf data) throws WebSocketException {
			return new GenericWebSocketFrame(new PongWebSocketFrame(this.checkPayload(data, WebSocketFrame.Kind.PONG)), WebSocketFrame.Kind.PONG);
		}
		
		/**
		 * <p>
		 * Converts the specified webSocketFrame to an underlying WebSocket frame.
		 * </p>
		 * 
		 * @param webSocketFrame a WebSocket frame.
		 * 
		 * @return an underlying Netty frame
		 * 
		 * @throws WebSocketException if there was an error creating the frame
		 */
		public io.netty.handler.codec.http.websocketx.WebSocketFrame toUnderlyingWebSocketFrame(WebSocketFrame webSocketFrame) throws WebSocketException {
			if(webSocketFrame instanceof GenericWebSocketFrame) {
				return ((GenericWebSocketFrame)webSocketFrame).getUnderlyingFrame();
			}
			switch(webSocketFrame.getKind()) {
				case TEXT: {
					String text = webSocketFrame.getTextData();
					ByteBuf data;
					if (text == null || text.isEmpty()) {
						data = Unpooled.EMPTY_BUFFER;
					} 
					else {
						data = this.checkPayload(Unpooled.copiedBuffer(text, CharsetUtil.UTF_8), WebSocketFrame.Kind.TEXT);
					}
					return new TextWebSocketFrame(webSocketFrame.isFinal(), 0, data);
				}
				case BINARY: {
					return new BinaryWebSocketFrame(webSocketFrame.isFinal(), 0, this.checkPayload(webSocketFrame.getBinaryData(), WebSocketFrame.Kind.BINARY));
				}
				case CONTINUATION:{
					return new ContinuationWebSocketFrame(webSocketFrame.isFinal(), 0, this.checkPayload(webSocketFrame.getBinaryData(), WebSocketFrame.Kind.CONTINUATION));
				}
				case PING:{
					return new PingWebSocketFrame(this.checkPayload(webSocketFrame.getBinaryData(), WebSocketFrame.Kind.PING));
				}
				case PONG:{
					return new PongWebSocketFrame(this.checkPayload(webSocketFrame.getBinaryData(), WebSocketFrame.Kind.PONG));
				}
				default: {
					throw new WebSocketException("Unsupported frame kind: " + webSocketFrame.getKind());
				}
			}
		}
	}
}
