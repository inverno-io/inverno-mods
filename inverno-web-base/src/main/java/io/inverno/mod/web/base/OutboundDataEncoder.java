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

import io.inverno.mod.http.base.OutboundData;
import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An outbound data producer used to encode payload data from a single object or many objects.
 * </p>
 *
 * <p>
 * Implementors should rely on a {@link io.inverno.mod.base.converter.MediaTypeConverter} to encode payload data as a publisher of objects to a raw payload as a publisher of {@link ByteBuf}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see io.inverno.mod.base.converter.MediaTypeConverter
 *
 * @param <A> the type of the object to encode
 */
public interface OutboundDataEncoder<A> extends OutboundData<A> {

	/**
	 * <p>
	 * Encodes many objects.
	 * </p>
	 *
	 * @param <T>   the type of the object to encode
	 * @param value the list of objects to encode
	 */
	default <T extends A> void many(Iterable<T> value) {
		this.many(Flux.fromIterable(value));
	}

	/**
	 * <p>
	 * Encodes many objects.
	 * </p>
	 *
	 * @param <T>   the type of the object to encode
	 * @param value a flux emitting the objects to encode
	 */
	<T extends A> void many(Flux<T> value);

	/**
	 * <p>
	 * Encodes one object.
	 * </p>
	 *
	 * @param <T>   the type of the object to encode
	 * @param value the object to encode
	 */
	default <T extends A> void one(T value) {
		this.one(Mono.just(value));
	}

	/**
	 * <p>
	 * Encodes one object.
	 * </p>
	 *
	 * @param <T>   the type of the object to encode
	 * @param value a mono emitting the object to encode
	 */
	<T extends A> void one(Mono<T> value);
}
