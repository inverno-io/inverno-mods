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
package io.inverno.mod.web.compiler.spi.client;

import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.web.compiler.spi.WebParameterInfo;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Describes a {@code WebPart} route parameter.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public interface WebClientPartParameterInfo extends WebParameterInfo {

	/**
	 * <p>
	 * Indicates the kind of a part body.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	enum PartBodyKind {
		/**
		 * The actual part body type is {@link ByteBuf}.
		 */
		RAW,
		/**
		 * {@link CharSequence} is assignable from the actual part body type.
		 */
		CHARSEQUENCE,
		/**
		 * The actual part body type is {@link Resource}.
		 */
		RESOURCE,
		/**
		 * The actual part body type is none of the above.
		 */
		ENCODED;
	}

	/**
	 * <p>
	 * Indicates the reactive kind of a part body.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	enum PartBodyReactiveKind {
		/**
		 * The part body parameter is not of a reactive type.
		 */
		NONE,
		/**
		 * The part body parameter is of type {@link Publisher Publisher&lt;T&gt;} where {@code T} represents the actual request body type.
		 */
		PUBLISHER,
		/**
		 * The part body parameter is of type {@link Mono Mono&lt;T&gt;} where {@code T} represents the actual request body type.
		 */
		ONE,
		/**
		 * The part body parameter is of type {@link Flux Flux&lt;T&gt;} where {@code T} represents the actual request body type.
		 */
		MANY;
	}

	/**
	 * <p>
	 * Returns the part file name.
	 * </p>
	 *
	 * @return a file name
	 */
	String getFilename();

	/**
	 * <p>
	 * Returns the part content type.
	 * </p>
	 *
	 * @return a content type
	 */
	String getContentType();

	/**
	 * <p>
	 * Returns the response body kind.
	 * </p>
	 *
	 * <p>
	 * SSE is not supported by Web client.
	 * </p>
	 *
	 * @return the response body kind
	 */
	PartBodyKind getBodyKind();

	/**
	 * <p>
	 * Returns the response body reactive kind.
	 * </p>
	 *
	 * <p>
	 * NONE is not supported by Web client: response must be reactive.
	 * </p>
	 *
	 * @return the response body reactive kind
	 */
	PartBodyReactiveKind getBodyReactiveKind();
}
