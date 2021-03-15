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
package io.winterframework.mod.web.compiler.spi;

import javax.lang.model.type.TypeMirror;

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;
import io.winterframework.mod.http.base.Parameter;
import io.winterframework.mod.web.WebPart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Describes the request body parameter in a route.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public interface WebRequestBodyParameterInfo extends WebParameterInfo {

	/**
	 * <p>
	 * Indicates the kind of a request body.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	public static enum RequestBodyKind {
		/**
		 * The actual request body type is {@link ByteBuf}.
		 */
		RAW,
		/**
		 * The actual request body type is a super type of {@link WebPart}.
		 */
		MULTIPART,
		/**
		 * The actual request body type is {@link Parameter}.
		 */
		URLENCODED,
		/**
		 * The actual request body type is none of the above.
		 */
		ENCODED;
	}
	
	/**
	 * <p>
	 * Indicates the reactive kind of a request body.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	public static enum RequestBodyReactiveKind {
		/**
		 * The body parameter is not of a reactive type.
		 */
		NONE,
		/**
		 * The body parameter is of type {@link Publisher Publisher&lt;T&gt;} where
		 * {@code T} represents the actual request body type.
		 */
		PUBLISHER,
		/**
		 * The body parameter is of type {@link Mono Mono&lt;T&gt;} where {@code T}
		 * represents the actual request body type.
		 */
		ONE,
		/**
		 * The body parameter is of type {@link Flux Flux&lt;T&gt;} where {@code T}
		 * represents the actual request body type.
		 */
		MANY;
	}
	
	/**
	 * <p>
	 * Returns the request body kind.
	 * </p>
	 * 
	 * @return the request body kind
	 */
	RequestBodyKind getBodyKind();
	
	/**
	 * <p>
	 * Returns the request body reactive kind.
	 * </p>
	 * 
	 * @return the request body reactive kind
	 */
	RequestBodyReactiveKind getBodyReactiveKind();
	
	/**
	 * <p>
	 * Returns the actual type of the request body.
	 * </p>
	 * 
	 * <p>
	 * When the request body is reactive, this corresponds to the type argument of
	 * the reactive type.
	 * </p>
	 * 
	 * @return the actual body type
	 */
	@Override
	TypeMirror getType();
}
