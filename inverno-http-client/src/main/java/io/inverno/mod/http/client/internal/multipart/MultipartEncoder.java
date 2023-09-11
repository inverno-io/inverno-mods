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
package io.inverno.mod.http.client.internal.multipart;

import io.inverno.mod.http.base.header.Headers;
import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;

/**
 * <p>
 * Base multipart payload encoder.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @param <A> the type of data sent in the part
 */
public interface MultipartEncoder<A> {

	/**
	 * <p>
	 * Encodes the specified payload data publisher formated according to the specified content type.
	 * </p>
	 *
	 * @param data        the data to encode
	 * @param contentType the content type
	 *
	 * @return a stream of bytes
	 */
	Flux<ByteBuf> encode(Flux<A> data, Headers.ContentType contentType);
}
