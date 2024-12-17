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
package io.inverno.mod.http.client;

import io.inverno.mod.http.base.OutboundData;
import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import org.reactivestreams.Publisher;

/**
 *<p>
 * An intercepted response body allows to transform the response payload received from the endpoint and/or provide a response payload in case the request sent is cancelled.
 * </p>
 * 
 * <p>
 * It is exposed in the {@link InterceptedRequest}, once the request has been sent to the endpoint and an actual response received it is no longer possible to provide a response payload and
 * {@link IllegalStateException} shall be raised.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @see InterceptedRequest
 */
public interface InterceptedResponseBody {

	/**
	 * <p>
	 * Transforms the response payload publisher.
	 * </p>
	 *
	 * <p>
	 * This can be used in an exchange interceptor in order to decorate the response data publisher.
	 * </p>
	 *
	 * @param transformer a payload publisher transformer
	 *
	 * @return the response body
	 * 
	 * @throws IllegalStateException if the response payload publisher has already been subscribed
	 */
	InterceptedResponseBody transform(Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer) throws IllegalStateException;
	
	/**
	 * <p>
	 * Produces an empty payload.
	 * </p>
	 *
	 * <p>
	 * If a payload has already been provided this method does nothing.
	 * </p>
	 *
	 * <p>
	 * A typical usage is:
	 * </p>
	 *
	 * <pre>{@code
	 * exchange.response().body().empty();
	 * }</pre>
	 */
	void empty();
	
	/**
	 * <p>
	 * Returns a raw payload producer.
	 * </p>
	 * 
	 * <p>
	 * A typical usage is:
	 * </p>
	 * 
	 * <pre>{@code
	 * exchange.response().body().raw().stream(
	 *     Flux.just(
	 *         Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hello ", Charsets.DEFAULT)), 
	 *         Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("World!", Charsets.DEFAULT))
	 *     )
	 * );
	 * }</pre>
	 * 
	 * @return a raw payload producer
	 */
	OutboundData<ByteBuf> raw();
	
	/**
	 * <p>
	 * Returns a string payload producer.
	 * </p>
	 * 
	 * <p>
	 * A typical usage is:
	 * </p>
	 * 
	 * <pre>{@code
	 * exchange.response().body().string().stream(
	 *     Flux.just(
	 *         Unpooled.unreleasableBuffer("Hello "), 
	 *         Unpooled.unreleasableBuffer("World!")
	 *     )
	 * );
	 * }</pre>
	 * 
	 * @param <T> the type of char sequence
	 * 
	 * @return a string payload producer
	 */
	<T extends CharSequence> OutboundData<T> string();
	
	/**
	 * <p>
	 * Returns a resource payload producer.
	 * </p>
	 * 
	 * <p>
	 * A typical usage is:
	 * </p>
	 * 
	 * <pre>{@code
	 * ResourceService resourceService = ... 
	 * exchange.response().body().resource().value(resourceService.get("file:/path/to/resource");
	 * }</pre>
	 * 
	 * @return a resource payload producer
	 */
	InterceptedResponseBody.ResourceData resource();
	
	/**
	 * <p>
	 * A resource payload producer.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	interface ResourceData {
		
		/**
		 * <p>
		 * Sets the specified resource in the response payload.
		 * </p>
		 *
		 * <p>
		 * This method tries to determine the content type of the specified resource in which case the content type header is set in the response.
		 * </p>
		 *
		 * @param resource a resource
		 *
		 * @throws IllegalStateException if the request has already been sent to the endpoint and a response received
		 */
		void value(io.inverno.mod.base.resource.Resource resource) throws IllegalStateException;
	}
}
