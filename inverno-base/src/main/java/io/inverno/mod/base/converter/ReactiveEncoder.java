/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.base.converter;

import java.lang.reflect.Type;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A reactive encoder.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Encoder
 *
 * @param <From> the decoded type
 * @param <To>   the encoded type
 */
public interface ReactiveEncoder<From, To> extends Encoder<From, To> {

	/**
	 * <p>
	 * Encodes a mono stream of values to a stream of values.
	 * </p>
	 * 
	 * @param <T>   the type of the decoded object
	 * @param value the mono stream of values to encode
	 * 
	 * @return a stream of encoded values
	 */
	<T extends From> Publisher<To> encodeOne(Mono<T> value);
	
	/**
	 * <p>
	 * Encodes a mono stream of values whose type is represented by the specified
	 * class to a stream of values.
	 * </p>
	 * 
	 * @param <T>   the type of the decoded object
	 * @param value the mono stream of values to encode
	 * @param type  the class of the decoded object
	 * 
	 * @return a stream of encoded values
	 */
	<T extends From> Publisher<To> encodeOne(Mono<T> value, Class<T> type);
	
	/**
	 * <p>
	 * Encodes a mono stream of values whose type is the specified type to a stream
	 * of values.
	 * </p>
	 * 
	 * @param <T>   the type of the decoded object
	 * @param value the mono stream of values to encode
	 * @param type  the type of the decoded object
	 * 
	 * @return a stream of encoded values
	 */
	<T extends From> Publisher<To> encodeOne(Mono<T> value, Type type);
	
	/**
	 * <p>
	 * Encodes a flux stream of values to a stream of values.
	 * </p>
	 * 
	 * @param <T>   the type of the decoded object
	 * @param value the flux stream of values to encode
	 * 
	 * @return a stream of encoded values
	 */
	<T extends From> Publisher<To> encodeMany(Flux<T> value);
	
	/**
	 * <p>
	 * Encodes a flux stream of values whose type is represented by the specified
	 * class to a stream of values.
	 * </p>
	 * 
	 * @param <T>   the type of the decoded object
	 * @param value the flux stream of values to encode
	 * @param type  the class of the decoded object
	 * 
	 * @return a stream of encoded values
	 */
	<T extends From> Publisher<To> encodeMany(Flux<T> value, Class<T> type);

	/**
	 * <p>
	 * Encodes a flux stream of values whose type is the specified type to a stream
	 * of values.
	 * </p>
	 * 
	 * @param <T>   the type of the decoded object
	 * @param value the flux stream of values to encode
	 * @param type  the type of the decoded object
	 * 
	 * @return a stream of encoded values
	 */
	<T extends From> Publisher<To> encodeMany(Flux<T> value, Type type);
}
