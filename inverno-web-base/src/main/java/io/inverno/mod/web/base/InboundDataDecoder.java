/*
 * Copyright 2024 Jeremy KUHN
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
package io.inverno.mod.web.base;

import io.inverno.mod.http.base.InboundData;
import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An inbound data consumer used to decode payload data to a single object or many objects.
 * </p>
 *
 * <p>
 * Implementors should rely on a {@link io.inverno.mod.base.converter.MediaTypeConverter} to decode a raw payload as a publisher of {@link ByteBuf} into a publisher of decoded objects.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see io.inverno.mod.base.converter.MediaTypeConverter
 *
 * @param <A> the type of the decoded object
 */
public interface InboundDataDecoder<A> extends InboundData<A> {

	/**
	 * <p>
	 * Decodes inbound data into one single object.
	 * </p>
	 *
	 * @return a mono emitting the decoded object
	 */
	Mono<A> one();

	/**
	 * <p>
	 * Decodes inbound data into many objects.
	 * </p>
	 *
	 * @return a flux emitting the decoded objects
	 */
	Flux<A> many();
}
