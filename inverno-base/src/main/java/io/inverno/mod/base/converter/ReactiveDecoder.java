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
 * A reactive decoder.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Decoder
 *
 * @param <From> the encoded type
 * @param <To>   the decoded type
 */
public interface ReactiveDecoder<From, To> extends Decoder<From, To> {

	/**
	 * <p>
	 * Decodes the specified stream of values whose type is represented by the
	 * specified class into a mono stream of values.
	 * </p>
	 * 
	 * @param <T>   the type of the decoded object
	 * @param value the stream of values to decode
	 * @param type  the class of the decoded object
	 * 
	 * @return a mono emitting the decoded value
	 */
	<T extends To> Mono<T> decodeOne(Publisher<From> value, Class<T> type);
	
	/**
	 * <p>
	 * Decodes the specified stream of values whose type is the specified type into
	 * a mono stream of value.
	 * </p>
	 * 
	 * @param <T>   the type of the decoded object
	 * @param value the stream of values to decode
	 * @param type  the type of the decoded object
	 * 
	 * @return a mono emitting the decoded value
	 */
	<T extends To> Mono<T> decodeOne(Publisher<From> value, Type type);
	
	/**
	 * <p>
	 * Decodes the specified stream of values whose type is represented by the
	 * specified class into a flux stream of values.
	 * </p>
	 * 
	 * @param <T>   the type of the decoded object
	 * @param value the stream of values to decode
	 * @param type  the class of the decoded object
	 * 
	 * @return a flux emitting the decoded values
	 */
	<T extends To> Flux<T> decodeMany(Publisher<From> value, Class<T> type);
	
	/**
	 * <p>
	 * Decodes the specified stream of values whose type is the specified type into
	 * a flux stream of values.
	 * </p>
	 * 
	 * @param <T>   the type of the decoded object
	 * @param value the stream of values to decode
	 * @param type  the type of the decoded object
	 * 
	 * @return a flux emitting the decoded values
	 */
	<T extends To> Flux<T> decodeMany(Publisher<From> value, Type type);
}
