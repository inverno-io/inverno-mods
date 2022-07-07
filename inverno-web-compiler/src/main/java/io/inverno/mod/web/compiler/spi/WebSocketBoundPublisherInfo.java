/*
 * Copyright 2022 Jeremy KUHN
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

import io.netty.buffer.ByteBuf;
import javax.lang.model.type.TypeMirror;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Describes a general WebSocket bound publisher (inbound or outbound)
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface WebSocketBoundPublisherInfo {
	
	/**
	 * <p>
	 * Indicates the kind of a bound.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	enum BoundKind {
		/**
		 * The actual bound type is {@link ByteBuf} and corresponds to a single WebSocket message after reduction of non-final frames.
		 */
		RAW_REDUCED,
		/**
		 * The actual bound type is {@link Mono Mono&lt;ByteBuf&gt;} and corresponds to a single WebSocket message after reduction of non-final frames.
		 */
		RAW_REDUCED_ONE,
		/**
		 * The actual bound type is {@link Publisher Publisher&lt;ByteBuf&gt;} and corresponds to a single WebSocket message without reduction of non-final frames.
		 */
		RAW_PUBLISHER,
		/**
		 * The actual bound type is {@link Flux Flux&lt;ByteBuf&gt;} and corresponds to a single WebSocket message without reduction of non-final frames.
		 */
		RAW_MANY,
		/**
		 * The actual bound type extends {@link CharSequence} and corresponds to a single WebSocket message after reduction of non-final frames.
		 */
		CHARSEQUENCE_REDUCED,
		/**
		 * The actual bound type extends {@link Mono Mono&lt;CharSequence&gt;} and corresponds to a single WebSocket message after reduction of non-final frames.
		 */
		CHARSEQUENCE_REDUCED_ONE,
		/**
		 * The actual bound type extends {@link Publisher Publisher&lt;CharSequence&gt;} and corresponds to a single WebSocket message without reduction of non-final frames.
		 */
		CHARSEQUENCE_PUBLISHER,
		/**
		 * The actual bound type extends {@link Flux Flux&lt;CharSequence&gt;} and corresponds to a single WebSocket message without reduction of non-final frames.
		 */
		CHARSEQUENCE_MANY,
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
	 * Indicates the reactive kind of a bound.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	enum BoundReactiveKind {
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
	 * Returns the actual type of the bound.
	 * </p>
	 *
	 * <p>
	 * When the bound is reactive, this corresponds to the type argument of the reactive type.
	 * </p>
	 *
	 * @return the actual body type
	 */
	BoundKind getBoundKind();
	
	/**
	 * <p>
	 * Returns the bound reactive kind.
	 * </p>
	 * 
	 * @return the bound reactive kind
	 */
	BoundReactiveKind getBoundReactiveKind();
	
	/**
	 * <p>
	 * Returns the actual type of WebSocket bound messages.
	 * </p>
	 *
	 * <p>
	 * When the bound is reactive, this corresponds to the type argument of the reactive type.
	 * </p>
	 *
	 * @return the actual message type
	 */
	TypeMirror getType();
}
