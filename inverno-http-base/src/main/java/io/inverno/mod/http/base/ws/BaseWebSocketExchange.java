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

import io.inverno.mod.http.base.ExchangeContext;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Represents a WebSocket exchange between a client and a server.
 * </p>
 * 
 * <p>
 * A WebSocket exchange is bidirectional and as a result provided an {@link Inbound} and an {@link Outbound} exposing WebSocket frames and messages respectively received and sent by a client or a 
 * server.
 * </p>
 * 
 * <p>
 * A WebSocket exchange can be created from an HTTP server exchange once the WebSocket opening handshake has completed or from an HTTP client to open a WebSocket connection to an HTTP server.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the type of the exchange context
 */
public interface BaseWebSocketExchange<A extends ExchangeContext> {
	
	/**
	 * <p>
	 * Returns the context attached to the exchange.
	 * </p>
	 * 
	 * @return the exchange context or null
	 */
	A context();
	
	/**
	 * <p>
	 * Returns the subprotocol that was negotiated with the client during the opening handshake.
	 * </p>
	 * 
	 * <p>
	 * Note that the handshake will fail and the WebSocket connection closed if no subprotocols could have been negotiated.
	 * </p>
	 * 
	 * @return a subprotocol or null if no subprotocol was specified by both client and server
	 */
	String getSubProtocol();
	
	/**
	 * <p>
	 * Returns the inbound part of the WebSocket exchange.
	 * </p>
	 * 
	 * <p>
	 * This basically corresponds to the stream of WebSocket frames received by the server from the client or by the client from the server.
	 * </p>
	 * 
	 * @return the inbound part
	 */
	BaseWebSocketExchange.Inbound inbound();
	
	/**
	 * <p>
	 * Returns the outbound part of the WebSocket exchange.
	 * </p>
	 * 
	 * <p>
	 * This basically corresponds to the stream of WebSocket frames sent by the server to the client or by the client to the server.
	 * </p>
	 * 
	 * @return the outbound part
	 */
	BaseWebSocketExchange.Outbound outbound();
	
	/**
	 * <p>
	 * Closes the WebSocket with the normal status ({@code 1000}).
	 * </p>
	 * 
	 * <p>
	 * If the WebSocket was already closed, this method does nothing.
	 * </p>
	 */
	default void close() {
		this.close(WebSocketStatus.NORMAL_CLOSURE, WebSocketStatus.NORMAL_CLOSURE.getReason());
	}
	
	/**
	 * <p>
	 * Closes the WebSocket with the specified status.
	 * </p>
	 * 
	 * <p>
	 * If the WebSocket was already closed, this method does nothing.
	 * </p>
	 * 
	 * @param status a WebSocket close status
	 */
	default void close(WebSocketStatus status) {
		this.close(status.getCode(), status.getReason());
	}
	
	/**
	 * <p>
	 * Closes the WebSocket with the specified status code.
	 * </p>
	 * 
	 * <p>
	 * If the WebSocket was already closed, this method does nothing.
	 * </p>
	 * 
	 * @param code a WebSocket close status code
	 */
	default void close(short code) {
		this.close(code, null);
	}
	
	/**
	 * <p>
	 * Closes the WebSocket with the normal status ({@code 1000}) and the specified reason.
	 * </p>
	 * 
	 * <p>
	 * A WebSocket close frame must have a payload length of 125 bytes or less, when {@code code + reason} exceeds this limit, the reason shall be truncated.
	 * </p>
	 * 
	 * <p>
	 * If the WebSocket was already closed, this method does nothing.
	 * </p>
	 * 
	 * @param reason a close reason
	 */
	default void close(String reason) {
		this.close(WebSocketStatus.NORMAL_CLOSURE.getCode(), reason);
	}
	
	/**
	 * <p>
	 * Closes the WebSocket with specified status and reason.
	 * </p>
	 * 
	 * <p>
	 * A WebSocket close frame must have a payload length of 125 bytes or less, when {@code code + reason} exceeds this limit, the reason shall be truncated.
	 * </p>
	 * 
	 * <p>
	 * If the WebSocket was already closed, this method does nothing.
	 * </p>
	 * 
	 * @param status a WebSocket close status
	 * @param reason a WebSocket close status code
	 */
	default void close(WebSocketStatus status, String reason) {
		this.close(status.getCode(), reason);
	}
	
	/**
	 * <p>
	 * Closes the WebSocket with specified status code and reason.
	 * </p>
	 * 
	 * <p>
	 * A WebSocket close frame must have a payload length of 125 bytes or less, when {@code code + reason} exceeds this limit, the reason shall be truncated.
	 * </p>
	 * 
	 * <p>
	 * A WebSocket close frame must have a payload length of 125 bytes or less, when {@code code + reason} exceeds this limit, the reason shall be truncated.
	 * </p>
	 * 
	 * <p>
	 * If the WebSocket was already closed, this method does nothing.
	 * </p>
	 * 
	 * @param code   a WebSocket close status code
	 * @param reason a WebSocket close status code
	 */
	void close(short code, String reason);
	
	/**
	 * <p>
	 * Represents the inbound part of a WebSocket exchange.
	 * </p>
	 * 
	 * <p>
	 * It exposes the frames or messages received by the server from the client or by the client from the server. It is only possible to subscribe to one of the exposed publishers, subscribing to both
	 * frames and messages will result in an {@link IllegalStateException}.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	interface Inbound {
		
		/**
		 * <p>
		 * Returns the received frames.
		 * </p>
		 * 
		 * <p>
		 * Subscription to the returned publisher is exclusive, subscribing to another publisher will fail.
		 * </p>
		 * 
		 * @return a publisher of WebSocket frames
		 */
		Publisher<WebSocketFrame> frames();
		
		/**
		 * <p>
		 * Returns the received messages.
		 * </p>
		 * 
		 * <p>
		 * Subscription to the returned publisher is exclusive, subscribing to another publisher will fail.
		 * </p>
		 * 
		 * @return a publisher of WebSocket messages
		 */
		Publisher<WebSocketMessage> messages();
		
		/**
		 * <p>
		 * Returns the received text messages.
		 * </p>
		 * 
		 * <p>
		 * Subscription to the returned publisher is exclusive, subscribing to another publisher will fail.
		 * </p>
		 * 
		 * <p>
		 * The resulting publisher filters out any received message that is not a text message.
		 * </p>
		 * 
		 * @return a publisher of WebSocket text messages
		 */
		Publisher<WebSocketMessage> textMessages();
		
		/**
		 * <p>
		 * Returns the received binary messages.
		 * </p>
		 * 
		 * <p>
		 * Subscription to the returned publisher is exclusive, subscribing to another publisher will fail.
		 * </p>
		 * 
		 * <p>
		 * The resulting publisher filters out any received message that is not a binary message.
		 * </p>
		 * 
		 * @return a publisher of WebSocket binary messages
		 */
		Publisher<WebSocketMessage> binaryMessages();
	}
	
	/**
	 * <p>
	 * Represents the outbound part of a WebSocket exchange.
	 * </p>
	 * 
	 * <p>
	 * It allows to set the stream of frames or messages that must be sent to the client by the server or to the server by the client. It is only possible to specify either a stream of frames or a 
	 * stream of messages.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	interface Outbound {
		
		/**
		 * <p>
		 * Specifies whether the WebSocket exchange should be closed when the frames publisher completes successfully.
		 * </p>
		 *
		 * <p>
		 * When the WebSocket frames publisher completes, it is sometimes desirable to keep the socket opened in order to consume yet to be received messages sent by the remote endpoint before or
		 * after receiving the close frame. Note that when the publisher terminates in error, this is considered a failure and the WebSocket exchange is always closed.
		 * </p>
		 * 
		 * <p>
		 * The default behaviour is to close the WebSocket exchange.
		 * </p>
		 *
		 * @param closeOnComplete true to close the WebSocket exchange when the frames publisher completes, false otherwise
		 * 
		 * @return the outound
		 */
		Outbound closeOnComplete(boolean closeOnComplete);
		
		/**
		 * <p>
		 * Sets the stream of frames to send.
		 * </p>
		 * 
		 * <p>
		 * WebSocket frames should be created using the provided {@link WebSocketFrame.Factory}.
		 * </p>
		 * 
		 * <p>
		 * By default, the WebSocket exchange is closed when the frames publisher completes, invoke {@link #closeOnComplete(boolean) } to change that behaviour.
		 * </p>
		 * 
		 * @param frames a function that returns a publisher of WebSocket frames created using the provided factory
		 */
		void frames(Function<WebSocketFrame.Factory, Publisher<WebSocketFrame>> frames);
		
		/**
		 * <p>
		 * Sets the stream of messages to send.
		 * </p>
		 * 
		 * <p>
		 * WebSocket messages should be created using the provided {@link WebSocketMessage.Factory}.
		 * </p>
		 * 
		 * <p>
		 * By default, the WebSocket exchange is closed when the messages publisher completes, invoke {@link #closeOnComplete(boolean) } to change that behaviour.
		 * </p>
		 * 
		 * @param messages a function that returns a publisher of WebSocket messages created using the provided factory
		 */
		void messages(Function<WebSocketMessage.Factory, Publisher<WebSocketMessage>> messages);
	}
}
