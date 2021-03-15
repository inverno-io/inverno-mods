/*
 * Copyright 2020 Jeremy KUHN
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
package io.winterframework.mod.http.server.internal.multipart;

import io.netty.buffer.ByteBuf;
import io.winterframework.mod.http.base.header.Headers;
import reactor.core.publisher.Flux;

/**
 * <p>
 * Base multipart payload decoder.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @param <A> the part type
 */
public interface MultipartDecoder<A> {

	/**
	 * <p>
	 * Decodes the specified payload data publisher formated according to the
	 * specified content type.
	 * </p>
	 * 
	 * @param data        the payload data publisher
	 * @param contentType the payload content type
	 * 
	 * @return a part publisher
	 */
	Flux<A> decode(Flux<ByteBuf> data, Headers.ContentType contentType);
}
