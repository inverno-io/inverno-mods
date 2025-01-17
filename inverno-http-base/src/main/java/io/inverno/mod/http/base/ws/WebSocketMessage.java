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
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

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
		BINARY
	}
	
	/**
	 * <p>
	 * A factory used to create WebSocket message.
	 * </p>
	 * 
	 * <p>
	 * It allows to create message that comply with the configuration (e.g. max frame size...). The specified payload data publisher can be rearranged to comply with max frame size by splitting big 
	 * fragments into smaller frames.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	interface Factory {
		
		/**
		 * <p>
		 * Creates a text message with the specified payload data.
		 * </p>
		 * 
		 * @param value payload data
		 * 
		 * @return a WebSocket message
		 */
		WebSocketMessage text(String value);
		
		/**
		 * <p>
		 * Creates a text message with the specified payload data stream.
		 * </p>
		 * 
		 * @param stream payload data stream
		 * 
		 * @return a WebSocket message
		 */
		WebSocketMessage text(Publisher<String> stream);
		
		/**
		 * <p>
		 * Creates a text message with the specified raw payload data.
		 * </p>
		 * 
		 * @param value raw payload data
		 * 
		 * @return a WebSocket message
		 */
		WebSocketMessage text_raw(ByteBuf value);
		
		/**
		 * <p>
		 * Creates a text message with the specified raw payload data stream.
		 * </p>
		 * 
		 * @param stream raw payload data stream
		 * 
		 * @return a WebSocket message
		 */
		WebSocketMessage text_raw(Publisher<ByteBuf> stream);
		
		/**
		 * <p>
		 * Creates a binary message with the specified payload data.
		 * </p>
		 * 
		 * @param value payload data
		 * 
		 * @return a WebSocket message
		 */
		WebSocketMessage binary(ByteBuf value);
		
		/**
		 * <p>
		 * Creates a binary message with the specified payload data stream.
		 * </p>
		 * 
		 * @param stream payload data stream
		 * 
		 * @return a WebSocket message
		 */
		WebSocketMessage binary(Publisher<ByteBuf> stream);
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
	 * Note that the returned publisher is exclusive: it is only possible to subscribe to one of the publisher returned by {@code frames() }, {@link #raw() }, {@link #rawReduced() }, {@link #string() } or {@link #stringReduced() }.
	 * </p>
	 * 
	 * @return a WebSocket frames publisher
	 */
	Publisher<WebSocketFrame> frames();
	
	/**
	 * <p>
	 * Returns the message payload raw data stream.
	 * </p>
	 * 
	 * <p>
	 * Note that the returned publisher is exclusive: it is only possible to subscribe to one of the publisher returned by {@link #frames() }, {@code raw() }, {@link #rawReduced() }, {@link #string() } or {@link #stringReduced() }.
	 * </p>
	 * 
	 * @return a data publisher
	 */
	Publisher<ByteBuf> raw();
	
	/**
	 * <p>
	 * Returns the message payload reduced raw data.
	 * </p>
	 * 
	 * <p>
	 * This method basically reduces the result of {@link #raw() }.
	 * </p>
	 * 
	 * <p>
	 * Note that the returned publisher is exclusive: it is only possible to subscribe to one of the publisher returned by {@link #frames() }, {@link #raw() }, {@code rawReduced() }, {@link #string() } or {@link #stringReduced() }.
	 * </p>
	 * 
	 * @return a mono emitting the binary payload
	 */
	Mono<ByteBuf> rawReduced();
	
	/**
	 * <p>
	 * Returns the message payload string data stream.
	 * </p>
	 * 
	 * <p>
	 * Note that the returned publisher is exclusive: it is only possible to subscribe to one of the publisher returned by {@link #frames() }, {@link #raw() }, {@link #rawReduced() }, {@code string() } or {@link #stringReduced() }.
	 * </p>
	 * 
	 * @return a text publisher
	 */
	Publisher<String> string();
	
	/**
	 * <p>
	 * Returns the message payload reduced string data stream.
	 * </p>
	 * 
	 * <p>
	 * This method basically reduces the result of {@link #string() }.
	 * </p>
	 * 
	 * <p>
	 * Note that the returned publisher is exclusive: it is only possible to subscribe to one of the publisher returned by {@link #frames() }, {@link #raw() }, {@link #rawReduced() }, {@link #string() } or {@code stringReduced() }.
	 * </p>
	 * 
	 * @return a mono emitting the text payload
	 */
	Mono<String> stringReduced();
}
