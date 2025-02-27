/*
 * Copyright 2021 Jeremy Kuhn
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
package io.inverno.mod.web.server;

import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.http.server.ResponseBody;
import io.inverno.mod.web.base.OutboundDataEncoder;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Type;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A response body with payload encoding support.
 * </p>
 *
 * <p>
 * Implementors should rely on {@link MediaTypeConverter} to encode a payload based on the content type of the response.
 * </p>
 *
 * <p>
 * If no content-type header is specified in the response when encoding the payload, implementors may use the definition of the route serving the resource and especially the produced media type when
 * specified to determine the converter to use.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see WebRequest
 * @see MediaTypeConverter
 */
public interface WebResponseBody extends ResponseBody {

	@Override
	WebResponseBody transform(Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer);

	@Override
	WebResponseBody before(Mono<Void> before);

	@Override
	WebResponseBody after(Mono<Void> after);

	/**
	 * <p>
	 * Returns an encoder to encode a payload based on the content type of the request.
	 * </p>
	 *
	 * @param <T> the type to encode
	 *
	 * @return an encoder
	 */
	<T> OutboundDataEncoder<T> encoder();

	/**
	 * <p>
	 * Returns an encoder to encode a payload of the specified type based on the content type of the request.
	 * </p>
	 *
	 * @param <T>  the type to encode
	 * @param type a class of T
	 *
	 * @return an encoder
	 */
	<T> OutboundDataEncoder<T> encoder(Class<T> type);

	/**
	 * <p>
	 * Returns an encoder to encode a payload of the specified type based on the content type of the request.
	 * </p>
	 *
	 * @param <T>  the type to encode
	 * @param type the type to encode
	 *
	 * @return an encoder
	 */
	<T> OutboundDataEncoder<T> encoder(Type type);

	/**
	 * <p>
	 * Returns a server-sent events encoder to encode event's data in the specified media type.
	 * </p>
	 *
	 * @param <T>       the type to encode
	 * @param mediaType the target media type
	 *
	 * @return a SSE encoder
	 */
	<T> WebResponseBody.SseEncoder<T> sseEncoder(String mediaType);

	/**
	 * <p>
	 * Returns a server-sent events encoder to encode event's data of the specified type in the specified media type.
	 * </p>
	 *
	 * @param <T>       the type to encode
	 * @param mediaType the target media type
	 * @param type      a class of T
	 *
	 * @return a SSE encoder
	 */
	<T> WebResponseBody.SseEncoder<T> sseEncoder(String mediaType, Class<T> type);

	/**
	 * <p>
	 * Returns a server-sent events encoder to encode event's data of the specified type in the specified media type.
	 * </p>
	 *
	 * @param <T>       the type to encode
	 * @param mediaType the target media type
	 * @param type      the type to encode
	 *
	 * @return a SSE encoder
	 */
	<T> WebResponseBody.SseEncoder<T> sseEncoder(String mediaType, Type type);

	/**
	 * <p>
	 * A server-sent events data producer used to encode data from a single object or many objects.
	 * </p>
	 *
	 * <p>
	 * Implementors should rely on a {@link MediaTypeConverter} to encode data as a publisher of objects to raw data as a publisher of {@link ByteBuf}.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 *
	 * @see MediaTypeConverter
	 *
	 * @param <A> the type of data to encode
	 */
	interface SseEncoder<A> extends ResponseBody.Sse<A, WebResponseBody.SseEncoder.Event<A>, WebResponseBody.SseEncoder.EventFactory<A>> {

		/**
		 * <p>
		 * A server-sent event with data encoding support.
		 * </p>
		 *
		 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
		 * @since 1.0
		 *
		 * @param <A> the type of data to encode
		 */
		interface Event<A> extends ResponseBody.Sse.Event<A>, OutboundDataEncoder<A> {

			@Override
			WebResponseBody.SseEncoder.Event<A> id(String id);

			@Override
			WebResponseBody.SseEncoder.Event<A> comment(String comment);

			@Override
			WebResponseBody.SseEncoder.Event<A> event(String event);
		}

		/**
		 * <p>
		 * A server-sent events factory with data encoding support.
		 * </p>
		 *
		 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
		 *
		 * @param <A> the type of data to encode
		 */
		interface EventFactory<A> extends ResponseBody.Sse.EventFactory<A, WebResponseBody.SseEncoder.Event<A>> {

		}
	}
}