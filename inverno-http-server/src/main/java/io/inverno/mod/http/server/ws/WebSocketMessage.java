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
package io.inverno.mod.http.server.ws;

import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;

/**
 * <p>
 * Represents a WebSocket message which can be fragmented into multiple data frames as defined by <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-5.4">RFC 6455 Section 5.4</a>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface WebSocketMessage {
	
	/**
	 * <p>
	 * WebSocket message type.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	enum Kind {
		/**
		 * Indicates a {@code TEXT} message.
		 */
		TEXT,
		/**
		 * Indicates a {@code BINARY} message.
		 */
		BINARY;
	}
	
	/**
	 * <p>
	 * A factory used to create WebSocket message.
	 * </p>
	 * 
	 * <p>
	 * It allows to create message that comply with server's configuration (e.g. max frame size...). The specified payload data publisher can be rearranged to comply with max frame size by splitting
	 * big fragments into smaller frames.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	interface Factory {
		
		/**
		 * <p>
		 * Creates a text message with the specified payload data stream.
		 * </p>
		 * 
		 * @param value payload data
		 * 
		 * @return a WebSocket message
		 */
		WebSocketMessage text(Publisher<String> value);
		
		/**
		 * <p>
		 * Creates a binary message with the specified payload data stream.
		 * </p>
		 * 
		 * @param value payload data
		 * 
		 * @return a WebSocket message
		 */
		WebSocketMessage binary(Publisher<ByteBuf> value);
	}
	
	/**
	 * <p>
	 * Returns the WebSocket message type.
	 * </p>
	 * 
	 * @return a WebSocket message type
	 */
	WebSocketMessage.Kind getKind();
	
	/**
	 * <p>
	 * Returns the frames that composes the message.
	 * </p>
	 * 
	 * <p>
	 * Note that the returned publisher is exclusive: it is only possible to subscribe to one of the publisher returned by {@link #frames() }, {@link #binary() } or {@link #text() }.
	 * </p>
	 * 
	 * @return a WebSocker frames publisher
	 */
	Publisher<WebSocketFrame> frames();
	
	/**
	 * <p>
	 * Returns the message payload binary data.
	 * </p>
	 * 
	 * <p>
	 * Note that the returned publisher is exclusive: it is only possible to subscribe to one of the publisher returned by {@link #frames() }, {@link #binary() } or {@link #text() }.
	 * </p>
	 * 
	 * @return a data publisher
	 */
	Publisher<ByteBuf> binary();
	
	/**
	 * <p>
	 * Returns the message payload text data.
	 * </p>
	 * 
	 * <p>
	 * Note that the returned publisher is exclusive: it is only possible to subscribe to one of the publisher returned by {@link #frames() }, {@link #binary() } or {@link #text() }.
	 * </p>
	 * 
	 * @return a text publisher
	 */
	Publisher<String> text();
}
