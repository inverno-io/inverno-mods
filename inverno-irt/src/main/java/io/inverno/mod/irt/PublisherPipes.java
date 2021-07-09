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

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;

/**
 * <p>
 * A collection of pipes used to transform publishers including: filter, sort,
 * map, flatMap...
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 */
public final class PublisherPipes {

	/**
	 * <p>
	 * Returns a pipe which converts a publisher to filter out duplicates.
	 * </p>
	 * 
	 * @param <T> the type of element in the publisher
	 * 
	 * @return a publisher pipe
	 */
	public static <T> PublisherPipe<T, T> distinct() {
		return source -> Flux.from(source).distinct();
	}
	
	/**
	 * <p>
	 * Returns a pipe which converts a publisher to filter out duplicates based on
	 * their keys computed using the specified key selector.
	 * </p>
	 * 
	 * @param <T> the type of element in the publisher
	 * @param <U> the type of the computed keys
	 * @param keySelector a key selector
	 * 
	 * @return a publisher pipe
	 */
	public static <T, U> Function<? super Publisher<T>, ? extends Publisher<T>> distinct(Function<? super T,? extends U> keySelector) {
		return source -> Flux.from(source).distinct(keySelector);
	}
	
	/**
	 * <p>
	 * Returns a pipe which converts a publisher to sort the elements in natural
	 * order.
	 * </p>
	 * 
	 * @param <T> the type of element in the publisher
	 * 
	 * @return a publisher pipe
	 */
	public static <T> PublisherPipe<T, T> sort() {
		return source -> Flux.from(source).sort();
	}
	
	/**
	 * <p>
	 * Returns a pipe which converts a publisher to sort the elements using the
	 * specified comparator.
	 * </p>
	 * 
	 * @param <T> the type of element in the publisher
	 * @param comparator a comparator
	 * 
	 * @return a publisher pipe
	 */
	public static <T> PublisherPipe<T, T> sort(Comparator<? super T> comparator) {
		return source -> Flux.from(source).sort(comparator);
	}
	
	/**
	 * <p>
	 * Returns a pipe which converts a publisher to filter out elements that do
	 * not pass the specified predicate test.
	 * </p>
	 * 
	 * @param <T> the type of element in the publisher
	 * @param predicate a predicate
	 * 
	 * @return a publisher pipe
	 */
	public static <T> PublisherPipe<T, T> filter(Predicate<? super T> predicate) {
		return source -> Flux.from(source).filter(predicate);
	}
	
	/**
	 * <p>
	 * Returns a pipe which converts a publisher to filter out elements based on
	 * their keys computed using the specified key selector that do
	 * not pass the specified predicate test.
	 * </p>
	 * 
	 * @param <T> the type of element in the publisher
	 * @param <U> the type of the computed keys
	 * @param keySelector a key selector
	 * @param predicate a predicate
	 * 
	 * @return a publisher pipe
	 */
	public static <T, U> PublisherPipe<T, T> filter(Function<? super T, ? extends U> keySelector, Predicate<? super U> predicate) {
		return source -> Flux.from(source).filter(v -> predicate.test(keySelector.apply(v)));
	}
	
	/**
	 * <p>
	 * Returns a pipe which transforms the elements of a publisher by applying the
	 * specified mapper function.
	 * </p>
	 * 
	 * @param <T> the type of element in the publisher
	 * @param <R> the type of element in the resulting publisher
	 * @param mapper a mapper function
	 * 
	 * @return a publisher pipe
	 */
	public static <T, R> PublisherPipe<T, R> map(Function<? super T,? extends R> mapper) {
		return source -> Flux.from(source).map(mapper);
	}
	
	/**
	 * <p>
	 * Returns a pipe which transforms the elements of a publisher into publishers
	 * by applying the specified mapper function then flatten, these inner
	 * publishers into a single publisher, sequentially and preserving order using
	 * concatenation.
	 * </p>
	 * 
	 * @param <T>    the type of element in the publisher
	 * @param <R>    the type of element in the resulting publisher
	 * @param mapper a mapper function
	 * 
	 * @return a publisher pipe
	 */
	public static <T, R> PublisherPipe<T, R> concatMap(Function<? super T,? extends Publisher<? extends R>> mapper) {
		return source -> Flux.from(source).concatMap(mapper);
	}
	
	/**
	 * <p>
	 * Returns a pipe which transforms the elements of a publisher into publishers
	 * by applying the specified mapper function, then flatten these inner
	 * publishers into a single publisher through merging, which allow them to
	 * interleave.
	 * </p>
	 * 
	 * @param <T>    the type of element in the publisher
	 * @param <R>    the type of element in the resulting publisher
	 * @param mapper a mapper function
	 * 
	 * @return a publisher pipe
	 */
	public static <T, R> PublisherPipe<T, R> flatMap(Function<? super T,? extends Publisher<? extends R>> mapper) {
		return source -> Flux.from(source).flatMap(mapper);
	}
	
	/**
	 * <p>
	 * Returns a pipe which transforms the elements of a publisher into publishers
	 * by applying the specified mapper function, then flatten these inner
	 * publishers into a single Flux, but merge them in the order of their source
	 * element.
	 * </p>
	 * 
	 * @param <T>    the type of element in the publisher
	 * @param <R>    the type of element in the resulting publisher
	 * @param mapper a mapper function
	 * 
	 * @return a publisher pipe
	 */
	public static <T, R> PublisherPipe<T, R> flatMapSequential(Function<? super T,? extends Publisher<? extends R>> mapper) {
		return source -> Flux.from(source).flatMapSequential(mapper);
	}
}
