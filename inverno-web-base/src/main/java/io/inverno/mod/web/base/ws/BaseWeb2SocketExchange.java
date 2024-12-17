package io.inverno.mod.web.base.ws;

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.ws.BaseWebSocketExchange;
import java.lang.reflect.Type;
import org.reactivestreams.Publisher;

/**
 * <p>
 * Base Web WebSocket exchange supporting message encoding and decoding.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the exchange context type
 */
public interface BaseWeb2SocketExchange<A extends ExchangeContext> extends BaseWebSocketExchange<A> {

	@Override
	BaseWeb2SocketExchange.Inbound inbound();

	@Override
	BaseWeb2SocketExchange.Outbound outbound();

	/**
	 * <p>
	 * Extends {@link BaseWebSocketExchange.Inbound} to support WebSocket message decoding.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	interface Inbound extends BaseWebSocketExchange.Inbound {

		/**
		 * <p>
		 * Decodes inbound text messages to the specified type.
		 * </p>
		 *
		 * <p>
		 * The negotiated subprotocol shall be used to determine the converter to use to decode the raw message, the subprotocol is then assumed to be a compact application media type
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
		 * The negotiated subprotocol shall be used to determine the converter to use to decode the raw message, the subprotocol is then assumed to be a compact application media type
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
		 * The negotiated subprotocol shall be used to determine the converter to use to decode the raw message, the subprotocol is then assumed to be a compact application media type
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
		 * The negotiated subprotocol shall be used to determine the converter to use to decode the raw message, the subprotocol is then assumed to be a compact application media type
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
	 * Extends {@link BaseWebSocketExchange.Outbound} to support WebSocket message encoding.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	interface Outbound extends BaseWebSocketExchange.Outbound {

		@Override
		BaseWeb2SocketExchange.Outbound closeOnComplete(boolean closeOnComplete);

		/**
		 * <p>
		 * Encodes the specified messages to WebSocket text messages sent to the client.
		 * </p>
		 *
		 * <p>
		 * The negotiated subprotocol shall be used to determine the converter to use to encode the message, the subprotocol is then assumed to be a compact application media type (see
		 * {@link MediaTypes#normalizeApplicationMediaType(java.lang.String)}).
		 * </p>
		 *
		 * @param <T>      the type of message to encode
		 * @param messages the messages to send
		 */
		default <T> void encodeTextMessages(Publisher<T> messages) {
			this.encodeTextMessages(messages, (Type) null);
		}

		/**
		 * <p>
		 * Encodes the specified messages of the specified type to WebSocket text messages sent to the client.
		 * </p>
		 *
		 * <p>
		 * The negotiated subprotocol shall be used to determine the converter to use to encode the message, the subprotocol is then assumed to be a compact application media type (see
		 * {@link MediaTypes#normalizeApplicationMediaType(java.lang.String)}).
		 * </p>
		 *
		 * @param <T>      the type of message to encode
		 * @param messages the messages to send
		 * @param type     the type of message to encode
		 */
		default <T> void encodeTextMessages(Publisher<T> messages, Class<T> type) {
			this.encodeTextMessages(messages, (Type) type);
		}

		/**
		 * <p>
		 * Encodes the specified messages of the specified type to WebSocket text messages sent to the client.
		 * </p>
		 *
		 * <p>
		 * The negotiated subprotocol shall be used to determine the converter to use to encode the message, the subprotocol is then assumed to be a compact application media type (see
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
		 * The negotiated subprotocol shall be used to determine the converter to use to encode the message, the subprotocol is then assumed to be a compact application media type (see
		 * {@link MediaTypes#normalizeApplicationMediaType(java.lang.String)}).
		 * </p>
		 *
		 * @param <T>      the type of message to encode
		 * @param messages the messages to send
		 */
		default <T> void encodeBinaryMessages(Publisher<T> messages) {
			this.encodeBinaryMessages(messages, (Type) null);
		}

		/**
		 * <p>
		 * Encodes the specified messages of the specified type to WebSocket binary messages sent to the client.
		 * </p>
		 *
		 * <p>
		 * The negotiated subprotocol shall be used to determine the converter to use to encode the message, the subprotocol is then assumed to be a compact application media type (see
		 * {@link MediaTypes#normalizeApplicationMediaType(java.lang.String)}).
		 * </p>
		 *
		 * @param <T>      the type of message to encode
		 * @param messages the messages to send
		 * @param type     the type of message to encode
		 */
		default <T> void encodeBinaryMessages(Publisher<T> messages, Class<T> type) {
			this.encodeBinaryMessages(messages, (Type) type);
		}

		/**
		 * <p>
		 * Encodes the specified messages of the specified type to WebSocket binary messages sent to the client.
		 * </p>
		 *
		 * <p>
		 * The negotiated subprotocol shall be used to determine the converter to use to encode the message, the subprotocol is then assumed to be a compact application media type (see
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
