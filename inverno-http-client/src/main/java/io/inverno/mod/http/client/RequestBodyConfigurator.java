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
import io.inverno.mod.http.base.Parameter;
import io.netty.buffer.ByteBuf;
import java.util.function.BiConsumer;

/**
 * <p>
 * A configurator used to configure the body of the client request.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public interface RequestBodyConfigurator {

	/**
	 * <p>
	 * Produces an empty payload.
	 * </p>
	 *
	 * <p>
	 * A typical usage is:
	 * </p>
	 *
	 * <pre>{@code
	 * httpClientRequest.body(configurator -> configurator.empty());
	 * }</pre>
	 */
	void empty();
	
	/**
	 * <p>
	 * Returns a raw outbound data producer.
	 * </p>
	 * 
	 * <p>
	 * A typical usage is:
	 * </p>
	 * 
	 * <pre>{@code
	 * httpClientRequest.body(configurator -> configurator.raw().stream(
	 *     Flux.just(
	 *         Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hello ", Charsets.DEFAULT)), 
	 *         Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("World!", Charsets.DEFAULT))
	 *     )
	 * ));
	 * }</pre>
	 * 
	 * @return a raw outbound data
	 */
	OutboundData<ByteBuf> raw();
	
	/**
	 * <p>
	 * Returns a string outbound data producer.
	 * </p>
	 * 
	 * <p>
	 * A typical usage is:
	 * </p>
	 * 
	 * <pre>{@code
	 * httpClientRequest.body(configurator -> configurator.string().stream(
	 *     Flux.just(
	 *         Unpooled.unreleasableBuffer("Hello "), 
	 *         Unpooled.unreleasableBuffer("World!")
	 *     )
	 * ));
	 * }</pre>
	 * 
	 * @param <T> the type of char sequence
	 * 
	 * @return a string outbound data
	 */
	<T extends CharSequence> OutboundData<T> string();
	
	/**
	 * <p>
	 * Returns a resource data producer.
	 * </p>
	 * 
	 * <p>
	 * A typical usage is:
	 * </p>
	 * 
	 * <pre>{@code
	 * ResourceService resourceService = ... 
	 * httpClientRequest.body(configurator -> configurator.resource().value(resourceService.get("file:/path/to/resource"));
	 * }</pre>
	 * 
	 * @return a resource payload producer
	 */
	RequestBodyConfigurator.Resource resource();
	
	/**
	 * <p>
	 * Returns a URL encoded data producer.
	 * </p>
	 * 
	 * <p>
	 * A typical usage is:
	 * </p>
	 * 
	 * <pre>{@code
	 * httpClientRequest.body(body -> body.urlEncoded().from( 
	 *     (factory, output) -> output.stream(Flux.just(
	 *         factory.create("p1", "abc"), 
	 *         factory.create("p2", List.of("a", "b", "c"))
	 * ))));
	 * }</pre>
	 * 
	 * @return a URL encoded data producer
	 */
	RequestBodyConfigurator.UrlEncoded<Parameter.Factory> urlEncoded();
	
	/**
	 * <p>
	 * Returns a Multipart form data producer.
	 * </p>
	 * 
	 * <p>
	 * A typical usage is:
	 * </p>
	 * 
	 * <pre>{@code
	 * httpClientRequest.body(body -> body.multipart().from( 
	 *     (factory, output) -> output.stream(Flux.just(
	 *         factory.string(part -> part.name("key").value("value")),
	 *         factory.resource(part -> part
	 *             .name("myfile")
	 *             .headers(headers -> headers.contentType("application/json"))
	 *             .value(new FileResource("sample.json")))
	 * ))));
	 * }</pre>
	 * 
	 * @return a Multipart form data producer
	 */
	RequestBodyConfigurator.Multipart<Part.Factory, Part<?>> multipart();
	
	/**
	 * <p>
	 * A resource data producer.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	interface Resource {
		
		/**
		 * <p>
		 * Sets the specified resource in the request payload.
		 * </p>
		 *
		 * <p>
		 * This method tries to determine the content type of the specified resource in which case the content type header is set in the request.
		 * </p>
		 *
		 * @param resource a resource
		 * 
		 * @throws IllegalStateException if data were already sent to the recipient
		 */
		void value(io.inverno.mod.base.resource.Resource resource) throws IllegalStateException;
	}
	
	/**
	 * <p>
	 * A URL encoded data producer.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 * 
	 * @param <A> The parameter factory type
	 */
	interface UrlEncoded<A extends Parameter.Factory> {

		/**
		 * <p>
		 * Sets request parameters in the specified function using the parameter factory to create parameters and the parameter outbound data to sets the parameters in the request body.
		 * </p>
		 * 
		 * @param data a function in which request parameters must be set
		 */
		void from(BiConsumer<A, OutboundData<Parameter>> data);
	}
	
	/**
	 * <p>
	 * A Multipart form data producer.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 * 
	 * @param <A> The part factory type
	 * @param <B> the part type
	 */
	interface Multipart<A extends Part.Factory, B extends Part<?>> {
		
		/**
		 * <p>
		 * Sets request parts in the specified function using the part factory to create parts and the part outbound data to sets the parts in the request body.
		 * </p>
		 * 
		 * @param data a function in which request parameters must be set
		 */
		void from(BiConsumer<A, OutboundData<B>> data);
	}
}
