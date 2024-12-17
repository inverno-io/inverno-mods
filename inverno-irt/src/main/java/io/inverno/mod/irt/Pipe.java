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

import java.util.function.Function;

/**
 * <p>
 * A pipe is used within a template to transform a value before applying templates.
 * </p>
 * 
 * <p>
 * Pipes can be chained using the {@link #and(Pipe)} method in order to apply multiple transformations.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 * @param <T> the type of the value to transform
 * @param <U> the type of the value resulting from the transformation 
 */
@FunctionalInterface
public interface Pipe<T, U> extends Function<T, U> {

	/**
	 * <p>
	 * Chains the specified pipe after this pipe.
	 * </p>
	 * 
	 * @param <V>   the type of the value returned by the resulting transformation
	 * @param after the pipe to chain after this pipe
	 * 
	 * @return A pipe which first invokes this pipe and then the after pipe
	 */
	default <V> Pipe<T, V> and(Pipe<U, V> after) {
		return source -> after.apply(this.apply(source));
	}
	
	/**
	 * <p>
	 * Applies the specified pipe to the specified source value.
	 * </p>
	 * 
	 * @param <T> the type of the source value to transform
	 * @param <U> the type of the value resulting from the transformation
	 * @param source the source value to transform
	 * @param pipe the pipe to apply
	 * 
	 * @return the transformed value
	 */
	static <T, U> U apply(T source, Pipe<T, U> pipe) {
		return pipe.apply(source);
	}
}
