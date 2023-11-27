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
package io.inverno.mod.web.compiler.spi;

import javax.lang.model.type.TypeMirror;

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;
import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.web.WebResponseBody;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Describes the response body of a route.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public interface WebResponseBodyInfo {

	/**
	 * <p>
	 * Indicates the kind of a response body.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	public static enum ResponseBodyKind {
		/**
		 * The actual response body type is {@link ByteBuf}.
		 */
		RAW,
		/**
		 * {@link CharSequence} is assignable from the actual response body type.
		 */
		CHARSEQUENCE,
		/**
		 * The actual response body type is {@link ByteBuf}.
		 */
		SSE_RAW,
		/**
		 * {@link CharSequence} is assignable from the actual response body type.
		 */
		SSE_CHARSEQUENCE,
		/**
		 * The actual response body type is {@link WebResponseBody.SseEncoder.Event Event&lt;U&gt;} where {@code U} is not a {@link ByteBuf}.
		 */
		SSE_ENCODED,
		/**
		 * The actual response body type is {@link Resource}.
		 */
		RESOURCE,
		/**
		 * The actual response body type is {@link Void}.
		 */
		EMPTY,
		/**
		 * The actual response body type is none of the above.
		 */
		ENCODED;
	}
	
	/**
	 * <p>
	 * Indicates the reactive kind of a response body.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	public static enum ResponseBodyReactiveKind {
		/**
		 * The body is not of a reactive type.
		 */
		NONE,
		/**
		 * The body is of type {@link Publisher Publisher&lt;T&gt;} where {@code T} represents the actual response body type.
		 */
		PUBLISHER,
		/**
		 * The body is of type {@link Mono Mono&lt;T&gt;} where {@code T} represents the actual response body type.
		 */
		ONE,
		/**
		 * The body is of type {@link Flux Flux&lt;T&gt;} where {@code T} represents the actual response body type.
		 */
		MANY;
	}
	
	/**
	 * <p>
	 * Returns the actual type of the response body.
	 * </p>
	 *
	 * <p>
	 * When the response body is reactive, this corresponds to the type argument of the reactive type.
	 * </p>
	 *
	 * @return the actual body type
	 */
	TypeMirror getType();
	
	/**
	 * <p>
	 * Returns the response body kind.
	 * </p>
	 * 
	 * @return the response body kind
	 */
	ResponseBodyKind getBodyKind();
	
	/**
	 * <p>
	 * Returns the response body reactive kind.
	 * </p>
	 * 
	 * @return the response body reactive kind
	 */
	ResponseBodyReactiveKind getBodyReactiveKind();
	
}
