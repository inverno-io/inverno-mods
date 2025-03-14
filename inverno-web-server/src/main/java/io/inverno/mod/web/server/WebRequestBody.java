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
import io.inverno.mod.http.server.RequestBody;
import io.inverno.mod.web.base.InboundDataDecoder;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Type;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A request body with payload decoding support.
 * </p>
 *
 * <p>
 * Implementors should rely on {@link MediaTypeConverter} to decode a payload based on the content type of the request.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see WebRequest
 * @see MediaTypeConverter
 */
public interface WebRequestBody extends RequestBody {

	@Override
	WebRequestBody transform(Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer);

	@Override
	WebRequestBody before(Mono<Void> before);

	@Override
	WebRequestBody after(Mono<Void> after);

	/**
	 * <p>
	 * Returns a decoder to decode the payload to the specified type based on the content type of the request.
	 * </p>
	 *
	 * @param <A>  the decoded type
	 * @param type a class of A
	 *
	 * @return a decoder
	 */
	<A> InboundDataDecoder<A> decoder(Class<A> type);

	/**
	 * <p>
	 * Returns a decoder to decode the payload to the specified type based on the content type of the request.
	 * </p>
	 *
	 * @param <A>  the decoded type
	 * @param type the decoded type
	 *
	 * @return a decoder
	 */
	<A> InboundDataDecoder<A> decoder(Type type);

	@Override
	Multipart<WebPart> multipart() throws IllegalStateException;
}