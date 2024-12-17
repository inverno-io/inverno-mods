/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.web.client;

import io.inverno.mod.http.client.InterceptedResponseBody;
import io.inverno.mod.web.base.OutboundDataEncoder;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Type;
import java.util.function.Function;
import org.reactivestreams.Publisher;

/**
 * <p>
 * An intercepted Web response body with payload encoding support.
 * </p>
 *
 * <p>
 * Implementors should rely on {@link io.inverno.mod.base.converter.MediaTypeConverter} to encode a payload based on the content type of the response.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public interface InterceptedWebResponseBody extends InterceptedResponseBody {

	@Override
	InterceptedWebResponseBody transform(Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer) throws IllegalStateException;

	/**
	 * <p>
	 * Returns an encoder to encode a payload based on the content type of the response.
	 * </p>
	 *
	 * @param <T> the type to encode
	 *
	 * @return a data encoder
	 */
	<T> OutboundDataEncoder<T> encoder();

	/**
	 * <p>
	 * Returns an encoder to encode a payload of the specified type based on the content type of the response.
	 * </p>
	 *
	 * @param <T>  the type to encode
	 * @param type a class of T
	 *
	 * @return a data encoder
	 */
	<T> OutboundDataEncoder<T> encoder(Class<T> type);

	/**
	 * <p>
	 * Returns an encoder to encode a payload of the specified type based on the content type of the response.
	 * </p>
	 *
	 * @param <T>  the type to encode
	 * @param type the type to encode
	 *
	 * @return a data encoder
	 */
	<T> OutboundDataEncoder<T> encoder(Type type);
}
