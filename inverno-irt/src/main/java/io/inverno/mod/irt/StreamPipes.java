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
package io.inverno.mod.irt;

import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * <p>
 * A collection of pipes used to transform streams including: filter, sort, map,
 * flatMap...
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 */
public final class StreamPipes {
	
	/**
	 * <p>
	 * Returns a pipe which converts a stream to filter out duplicates.
	 * </p>
	 * 
	 * @param <T> the type of element in the stream
	 * 
	 * @return a stream pipe
	 */
	public static <T> StreamPipe<T, T> distinct() {
		return source -> source.distinct();
	}
	
	/**
	 * <p>
	 * Returns a pipe which converts a stream to sort the elements in natural
	 * order.
	 * </p>
	 * 
	 * @param <T> the type of element in the stream
	 * 
	 * @return a stream pipe
	 */
	public static <T> StreamPipe<T, T> sort() {
		return source -> source.sorted();
	}
	
	/**
	 * <p>
	 * Returns a pipe which converts a stream to sort the elements using the
	 * specified comparator.
	 * </p>
	 * 
	 * @param <T> the type of element in the stream
	 * @param comparator a comparator
	 * 
	 * @return a stream pipe
	 */
	public static <T> StreamPipe<T, T> sort(Comparator<? super T> comparator) {
		return source -> source.sorted(comparator);
	}
	
	/**
	 * <p>
	 * Returns a pipe which converts a stream to filter out elements that do
	 * not pass the specified predicate test.
	 * </p>
	 * 
	 * @param <T> the type of element in the stream
	 * @param predicate a predicate
	 * 
	 * @return a stream pipe
	 */
	public static <T> StreamPipe<T, T> filter(Predicate<? super T> predicate) {
		return source -> source.filter(predicate);
	}
	
	/**
	 * <p>
	 * Returns a pipe which converts a stream to filter out elements based on
	 * their keys computed using the specified key selector that do
	 * not pass the specified predicate test.
	 * </p>
	 * 
	 * @param <T> the type of element in the stream
	 * @param <U> the type of the computed keys
	 * @param keySelector a key selector
	 * @param predicate a predicate
	 * 
	 * @return a stream pipe
	 */
	public static <T, U> StreamPipe<T, T> filter(Function<? super T, ? extends U> extractor, Predicate<? super U> predicate) {
		return source -> source.filter(v -> predicate.test(extractor.apply(v)));
	}
	
	/**
	 * <p>
	 * Returns a pipe which transforms the elements of a stream by applying the
	 * specified mapper function.
	 * </p>
	 * 
	 * @param <T> the type of element in the stream
	 * @param <R> the type of element in the resulting stream
	 * @param mapper a mapper function
	 * 
	 * @return a stream pipe
	 */
	public static <T, R> StreamPipe<T, R> map(Function<? super T,? extends R> mapper) {
		return source -> source.map(mapper);
	}
	
	/**
	 * <p>
	 * Returns a pipe which transforms the elements of the stream into streams by
	 * applying the specified mapper function, then flatten these inner stream into
	 * a single stream sequentially using concatenation.
	 * </p>
	 * 
	 * @param <T>    the type of element in the publisher
	 * @param <R>    the type of element in the resulting publisher
	 * @param mapper a mapper function
	 * 
	 * @return a stream pipe
	 */
	public static <T, R> StreamPipe<T, R> flatMap(Function<? super T,? extends Stream<? extends R>> mapper) {
		return source -> source.flatMap(mapper);
	}
}
