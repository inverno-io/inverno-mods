/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.web.compiler.spi;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Indicates the reactive kind of a request body.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public enum RequestBodyReactiveKind {

	/**
	 * The body parameter is not of a reactive type.
	 */
	NONE,
	/**
	 * The body parameter is of type {@link Publisher Publisher&lt;T&gt;} where {@code T} represents the actual request body type.
	 */
	PUBLISHER,
	/**
	 * The body parameter is of type {@link Mono Mono&lt;T&gt;} where {@code T} represents the actual request body type.
	 */
	ONE,
	/**
	 * The body parameter is of type {@link Flux Flux&lt;T&gt;} where {@code T} represents the actual request body type.
	 */
	MANY;
}
