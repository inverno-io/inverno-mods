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

import java.util.stream.Stream;

/**
 * <p>
 * A stream pipe is used within a template to transform a stream before applying
 * templates to the elements of the stream.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 * @param <T> the type of the value in the stream to transform
 * @param <U> the type of the value in the resulting stream
 */
@FunctionalInterface
public interface StreamPipe<T, U> extends Pipe<Stream<T>, Stream<U>> {
	
	/**
	 * <p>
	 * Chains the specified stream pipe after this pipe.
	 * </p>
	 * 
	 * @param <V>   the type of the value in the resulting stream
	 * @param after the stream pipe to chain after this pipe
	 * 
	 * @return A stream pipe which first invokes this pipe and then the after pipe
	 */
	default <V> StreamPipe<T, V> and(StreamPipe<U, V> after) {
		return source -> after.apply(this.apply(source));
	}
}
