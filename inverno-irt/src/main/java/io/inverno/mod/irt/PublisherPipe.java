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

import org.reactivestreams.Publisher;

/**
 * <p>
 * A publisher pipe is used within a template to transform a publisher before applying
 * templates to the emitted elements.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 * @param <T> the type of the value emitted by the publisher to transform
 * @param <U> the type of the value emitted by the resulting publisher 
 */
@FunctionalInterface
public interface PublisherPipe<T, U> extends Pipe<Publisher<T>, Publisher<U>> {

	/**
	 * <p>
	 * Chains the specified publisher pipe after this pipe.
	 * </p>
	 * 
	 * @param <V>   the type of the value emitted by the resulting publisher
	 * @param after the publisher pipe to chain after this pipe
	 * 
	 * @return A publisher pipe which first invokes this pipe and then the after pipe
	 */
	default <V> PublisherPipe<T, V> and(PublisherPipe<U, V> after) {
		return source -> after.apply(this.apply(source));
	}
}
