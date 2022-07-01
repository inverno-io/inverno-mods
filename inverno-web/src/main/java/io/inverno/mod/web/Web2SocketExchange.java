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
package io.inverno.mod.web;

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ws.WebSocketExchange;
import java.lang.reflect.Type;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A WebSocket exchange that extends the HTTP server {@link WebSocketExchange} with features for the Web.
 * </p>
 * 
 * <p>
 * It supports inbound and outbound message decoding and encoding based on the negotiated sub protocol which is interpreted as a compact application media types (see 
 * {@link MediaTypes#normalizeApplicationMediaType(java.lang.String)}).
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface Web2SocketExchange<A extends ExchangeContext> extends WebSocketExchange<A> {

	@Override
	public WebRequest request();

	@Override
	Web2SocketExchange<A> finalizer(Mono<Void> finalizer);

	@Override
	public Web2SocketExchange.Inbound inbound();

	@Override
	public Web2SocketExchange.Outbound outbound();

	/**
	 * <p>
	 * Extends {@link WebSocketExchange.Inbound} to support WebSocket message decoding.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	interface Inbound extends WebSocketExchange.Inbound {
		
		/**
		 * <p>
		 * Decodes inbound text messages to the specified type.
		 * </p>
		 * 
		 * <p>
		 * The negociated subprotocol shall be used to determine the converter to use to decode the raw message, the subprotocol is then assumed to be a compact application media type 
		 * (see {@link MediaTypes#normalizeApplicationMediaType(java.lang.String)}).
		 * </p>
		 *
		 * @param <A>  the decoded message type
		 * @param type the decoded message type
		 *
		 * @return a publisher of decoded message
		 */
		default <A> Publisher<A> decodeTextMessages(Class<A> type) {
			return this.decodeTextMessages((Type)type);
		}
		
		/**
		 * <p>
		 * Decodes inbound text messages to the specified type.
		 * </p>
		 * 
		 * <p>
		 * The negociated subprotocol shall be used to determine the converter to use to decode the raw message, the subprotocol is then assumed to be a compact application media type 
		 * (see {@link MediaTypes#normalizeApplicationMediaType(java.lang.String)}).
		 * </p>
		 *
		 * @param <A>  the decoded message type
		 * @param type the decoded message type
		 *
		 * @return a publisher of decoded message
		 */
		<A> Publisher<A> decodeTextMessages(Type type);
		
		/**
		 * <p>
		 * Decodes inbound binary messages to the specified type.
		 * </p>
		 * 
		 * <p>
		 * The negociated subprotocol shall be used to determine the converter to use to decode the raw message, the subprotocol is then assumed to be a compact application media type 
		 * (see {@link MediaTypes#normalizeApplicationMediaType(java.lang.String)}).
		 * </p>
		 *
		 * @param <A>  the decoded message type
		 * @param type the decoded message type
		 *
		 * @return a publisher of decoded message
		 */
		default <A> Publisher<A> decodeBinaryMessages(Class<A> type) {
			return this.decodeBinaryMessages((Type)type);
		}
		
		/**
		 * <p>
		 * Decodes inbound binary messages to the specified type.
		 * </p>
		 * 
		 * <p>
		 * The negociated subprotocol shall be used to determine the converter to use to decode the raw message, the subprotocol is then assumed to be a compact application media type 
		 * (see {@link MediaTypes#normalizeApplicationMediaType(java.lang.String)}).
		 * </p>
		 *
		 * @param <A>  the decoded message type
		 * @param type the decoded message type
		 *
		 * @return a publisher of decoded message
		 */
		<A> Publisher<A> decodeBinaryMessages(Type type);
	}
	
	/**
	 * <p>
	 * Extends {@link WebSocketExchange.Outbound} to support WebSocket message encoding.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	interface Outbound extends WebSocketExchange.Outbound {
		
		/**
		 * <p>
		 * Encodes the specified messages to WebSocket text messages sent to the client.
		 * </p>
		 * 
		 * <p>
		 * The negociated subprotocol shall be used to determine the converter to use to encode the message, the subprotocol is then assumed to be a compact application media type (see
		 * {@link MediaTypes#normalizeApplicationMediaType(java.lang.String)}).
		 * </p>
		 *
		 * @param <T>      the type of message to encode
		 * @param messages the messages to send
		 */
		default <T> void encodeTextMessages(Publisher<T> messages) {
			this.encodeTextMessages(messages, (Type)null);
		}
		
		/**
		 * <p>
		 * Encodes the specified messages of the specified type to WebSocket text messages sent to the client.
		 * </p>
		 * 
		 * <p>
		 * The negociated subprotocol shall be used to determine the converter to use to encode the message, the subprotocol is then assumed to be a compact application media type (see
		 * {@link MediaTypes#normalizeApplicationMediaType(java.lang.String)}).
		 * </p>
		 *
		 * @param <T>      the type of message to encode
		 * @param messages the messages to send
		 * @param type     the type of message to encode
		 */
		default <T> void encodeTextMessages(Publisher<T> messages, Class<T> type) {
			this.encodeTextMessages(messages, (Type)type);
		}
		
		/**
		 * <p>
		 * Encodes the specified messages of the specified type to WebSocket text messages sent to the client.
		 * </p>
		 * 
		 * <p>
		 * The negociated subprotocol shall be used to determine the converter to use to encode the message, the subprotocol is then assumed to be a compact application media type (see
		 * {@link MediaTypes#normalizeApplicationMediaType(java.lang.String)}).
		 * </p>
		 *
		 * @param <T>      the type of message to encode
		 * @param messages the messages to send
		 * @param type     the type of message to encode
		 */
		<T> void encodeTextMessages(Publisher<T> messages, Type type);
		
		/**
		 * <p>
		 * Encodes the specified messages to WebSocket binary messages sent to the client.
		 * </p>
		 * 
		 * <p>
		 * The negociated subprotocol shall be used to determine the converter to use to encode the message, the subprotocol is then assumed to be a compact application media type (see
		 * {@link MediaTypes#normalizeApplicationMediaType(java.lang.String)}).
		 * </p>
		 *
		 * @param <T>      the type of message to encode
		 * @param messages the messages to send
		 */
		default <T> void encodeBinaryMessages(Publisher<T> messages) {
			this.encodeBinaryMessages(messages, (Type)null);
		}
		
		/**
		 * <p>
		 * Encodes the specified messages of the specified type to WebSocket binary messages sent to the client.
		 * </p>
		 * 
		 * <p>
		 * The negociated subprotocol shall be used to determine the converter to use to encode the message, the subprotocol is then assumed to be a compact application media type (see
		 * {@link MediaTypes#normalizeApplicationMediaType(java.lang.String)}).
		 * </p>
		 *
		 * @param <T>      the type of message to encode
		 * @param messages the messages to send
		 * @param type     the type of message to encode
		 */
		default <T> void encodeBinaryMessages(Publisher<T> messages, Class<T> type) {
			this.encodeBinaryMessages(messages, (Type)type);
		}
		
		/**
		 * <p>
		 * Encodes the specified messages of the specified type to WebSocket binary messages sent to the client.
		 * </p>
		 * 
		 * <p>
		 * The negociated subprotocol shall be used to determine the converter to use to encode the message, the subprotocol is then assumed to be a compact application media type (see
		 * {@link MediaTypes#normalizeApplicationMediaType(java.lang.String)}).
		 * </p>
		 *
		 * @param <T>      the type of message to encode
		 * @param messages the messages to send
		 * @param type     the type of message to encode
		 */
		<T> void encodeBinaryMessages(Publisher<T> messages, Type type);
	}
}
