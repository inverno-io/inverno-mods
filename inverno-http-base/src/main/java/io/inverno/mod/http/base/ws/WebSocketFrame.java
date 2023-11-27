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
package io.inverno.mod.http.base.ws;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

/**
 * <p>
 * Represents a WebSocket frame as defined by <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-5">RFC 6455 Section 5</a>.
 * </p>
 * 
 * <p>
 * Just like {@link ByteBuf}, a WebSocket frame is reference counted to optimize memory. As a result, a WebSocket frame received in an inbound part of a WebSocket exchange must be released in the
 * WebSocket exchange handler if this is the final operation on the frame. Likewise when emitting a WebSocket frame in an outbound publisher of a WebSocket exchange, its reference count must be
 * greater than {@code 1} as it will be released after being sent to the endpoint.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface WebSocketFrame {
	
	/**
	 * <p>
	 * WebSocket frame type as defined by <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-5">RFC 6455 Section 5</a>.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	enum Kind {
		/**
		 * Indicates a {@code PING} control frame.
		 */
		PING,
		/**
		 * Indicates a {@code PONG} control frame.
		 */
		PONG,
		/**
		 * Indicates a {@code CLOSE} control frame.
		 */
		CLOSE,
		/**
		 * Indicates a {@code CONTINUATION} data frame.
		 */
		CONTINUATION,
		/**
		 * Indicates a {@code TEXT} data frame.
		 */
		TEXT,
		/**
		 * Indicates a {@code BINARY} data frame.
		 */
		BINARY;
	}
	
	/**
	 * <p>
	 * A factory used to create WebSocket frame.
	 * </p>
	 * 
	 * <p>
	 * It allows to create frames that comply with the configuration (e.g. max frame size...).
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	interface Factory {
		
		/**
		 * <p>
		 * Creates a final binary frame with the specified payload data.
		 * </p>
		 * 
		 * @param data payload data
		 * 
		 * @return a new WebSocket frame
		 * 
		 * @throws WebSocketException if there was an error creating the frame
		 */
		default WebSocketFrame binary(ByteBuf data) throws WebSocketException {
			return this.binary(data, true);
		}
		
		/**
		 * <p>
		 * Creates a binary frame with the specified payload data.
		 * </p>
		 * 
		 * @param data payload data
		 * @param finalFragment true to create a final frame, false otherwise
		 * 
		 * @return a new WebSocket frame
		 * 
		 * @throws WebSocketException if there was an error creating the frame
		 */
		WebSocketFrame binary(ByteBuf data, boolean finalFragment) throws WebSocketException;
		
		/**
		 * <p>
		 * Creates a final text frame with the specified payload data.
		 * </p>
		 * 
		 * @param text payload data
		 * 
		 * @return a new WebSocket frame
		 * 
		 * @throws WebSocketException if there was an error creating the frame
		 */
		default WebSocketFrame text(String text) throws WebSocketException {
			return this.text(text, true);
		}
		
		/**
		 * <p>
		 * Creates a text frame with the specified payload data.
		 * </p>
		 * 
		 * @param text payload data
		 * @param finalFragment true to create a final frame, false otherwise
		 * 
		 * @return a new WebSocket frame
		 * 
		 * @throws WebSocketException if there was an error creating the frame
		 */
		default WebSocketFrame text(String text, boolean finalFragment) throws WebSocketException {
			ByteBuf data;
			if (text == null || text.isEmpty()) {
				data = Unpooled.EMPTY_BUFFER;
			} 
			else {
				data = Unpooled.copiedBuffer(text, CharsetUtil.UTF_8);
			}
			return this.text(data, finalFragment);
		}
		
		/**
		 * <p>
		 * Creates a final text frame with the specified payload data.
		 * </p>
		 * 
		 * @param data payload data
		 * 
		 * @return a new WebSocket frame
		 * 
		 * @throws WebSocketException if there was an error creating the frame
		 */
		default WebSocketFrame text(ByteBuf data) throws WebSocketException {
			return this.text(data, true);
		}
		
		/**
		 * <p>
		 * Creates a text frame with the specified payload data.
		 * </p>
		 * 
		 * @param data payload data
		 * @param finalFragment true to create a final frame, false otherwise
		 * 
		 * @return a new WebSocket frame
		 * 
		 * @throws WebSocketException if there was an error creating the frame
		 */
		WebSocketFrame text(ByteBuf data, boolean finalFragment) throws WebSocketException;
		
		/**
		 * <p>
		 * Creates a continuation frame with the specified payload data.
		 * </p>
		 * 
		 * @param data payload data
		 * @param finalFragment true to create a final frame, false otherwise
		 * 
		 * @return a new WebSocket frame
		 * 
		 * @throws WebSocketException if there was an error creating the frame
		 */
		WebSocketFrame continuation(ByteBuf data, boolean finalFragment) throws WebSocketException;
		
		/**
		 * <p>
		 * Creates a ping frame with the specified payload data.
		 * </p>
		 * 
		 * <p>
		 * Note that a ping frame must have a payload length of 125 bytes or less as defined by <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-5.5">RFC 6455 Section 5.5</a>.
		 * </p>
		 * 
		 * @param data payload data
		 * 
		 * @return a new WebSocket frame
		 * 
		 * @throws WebSocketException if there was an error creating the frame
		 */
		WebSocketFrame ping(ByteBuf data) throws WebSocketException;
		
		/**
		 * <p>
		 * Creates a pong frame with the specified payload data.
		 * </p>
		 * 
		 * <p>
		 * Note that a pong frame must have a payload length of 125 bytes or less as defined by <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-5.5">RFC 6455 Section 5.5</a>.
		 * </p>
		 * 
		 * @param data payload data
		 * 
		 * @return a new WebSocket frame
		 * 
		 * @throws WebSocketException if there was an error creating the frame
		 */
		WebSocketFrame pong(ByteBuf data) throws WebSocketException;
	}
	
	/**
	 * <p>
	 * Returns the WebSocket frame type.
	 * </p>
	 * 
	 * @return a WebSocket frame type
	 */
	WebSocketFrame.Kind getKind();
	
	/**
	 * <p>
	 * Determines whether the frame is final.
	 * </p>
	 * 
	 * @return true if the frame is final, false otherwise
	 */
	boolean isFinal();
	
	/**
	 * <p>
	 * Returns the frame's payload data.
	 * </p>
	 * 
	 * @return the payload data
	 */
	ByteBuf getBinaryData();
	
	/**
	 * <p>
	 * Returns the frame's payload data as text.
	 * </p>
	 * 
	 * <p>
	 * This basically returns the frame's binary payload encode in UTF-8 as defined by <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-5.6">RFC 6455 Section 5.6</a>.
	 * </p>
	 * 
	 * @return the payload data as UTF-8 text
	 */
	String getTextData();
	
	/**
	 * <p>
	 * Returns a retained frame which shares the whole region of this frame's data.
	 * </p>
	 * 
	 * <p>
	 * A WebSocket frame is reference counted to optimize memory usage, as a result a frame can only be written once to the WebSocket endpoint (the frame and its data are released once the frame has 
	 * been sent). Using a retained duplicate allows to create multiple frames that share the same memory and that can be sent to multiple endpoints.
	 * </p>
	 * 
	 * @return a retained WebSocket frame
	 */
	WebSocketFrame retainedDuplicate();
	
	/**
	 * <p>
	 * Increases the reference count of the frame.
	 * </p>
	 * 
	 * <p>
	 * A WebSocket frame is reference counted, increasing the reference count allows to keep the frame into memory.
	 * </p>
	 * 
	 * @return this frame
	 */
	WebSocketFrame retain();
	
	/**
	 * <p>
	 * Decreases the reference count by {@code 1} of the frame and deallocates the frame if the reference count reaches at {@code 0}.
	 * </p>
	 * 
	 * <p>
	 * A WebSocket frame is reference counted, it must be released when processed in a final operation in order to release memory.
	 * </p>
	 * 
	 * @return this frame
	 */
	WebSocketFrame release();
	
	/**
	 * <p>
	 * Returns the current reference count of the frame.
	 * </p>
	 * 
	 * <p>
	 * If {@code 0}, it means the frame has been deallocated and can not be used anymore.
	 * </p>
	 * 
	 * @return the reference count
	 */
	int refCnt();
}
