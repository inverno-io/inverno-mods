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

import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.http.base.InboundRequestHeaders;
import io.inverno.mod.http.base.OutboundData;
import io.inverno.mod.http.base.OutboundRequestHeaders;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;

/**
 * <p>
 * Represents a part in a multipart/form-data request body as defined by <a href="https://tools.ietf.org/html/rfc7578">RFC 7578</a>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 *
 * @see RequestBody.Multipart
 * 
 * @param <A> the type of data sent in the part
 */
public interface Part<A> extends OutboundData<A> {

	/**
	 * <p>
	 * Specifies the part's name.
	 * </p>
	 * 
	 * @param name a name
	 * 
	 * @return this part
	 */
	Part<A> name(String name);
	
	/**
	 * <p>
	 * Specifies the part's file name.
	 * </p>
	 * 
	 * @param filename a file name
	 * 
	 * @return this part
	 */
	Part<A> filename(String filename);
	
	/**
	 * <p>
	 * Returns the part's headers.
	 * </p>
	 * 
	 * <p>
	 * The returned headers are read-only, use {@link #headers(java.util.function.Consumer) } for setting part's headers.
	 * </p>
	 * 
	 * @return read-only part's headers
	 */
	InboundRequestHeaders headers();
	
	/**
	 * <p>
	 * Specifies the part's headers.
	 * </p>
	 * 
	 * @param headersConfigurer a headers configurer
	 * 
	 * @return this part
	 */
	Part<A> headers(Consumer<OutboundRequestHeaders> headersConfigurer);
	
	/**
	 * <p>
	 * A factory for creating {@link Part}.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	interface Factory {
		
		/**
		 * <p>
		 * Creates a part with raw data.
		 * </p>
		 * 
		 * @param partConfigurer a part configurer
		 * 
		 * @return a new part
		 */
		Part<ByteBuf> raw(Consumer<Part<ByteBuf>> partConfigurer);
		
		/**
		 * <p>
		 * Creates a part with string data.
		 * </p>
		 * 
		 * @param <T> the type of char sequence
		 * @param partConfigurer a part configurer
		 * 
		 * @return a new part
		 */
		<T extends CharSequence> Part<T> string(Consumer<Part<T>> partConfigurer);
		
		/**
		 * <p>
		 * Creates a part with a resource as data.
		 * </p>
		 * 
		 * <p>
		 * The name of the resource shall be used as file name if no explicit file name is specified on the part. In a same way, {@code content-type} and {@code content-length} headers shall also be
		 * automatically set in the part if not explicitly specified.
		 * </p>
		 * 
		 * @param partConfigurer a pat configurer
		 * 
		 * @return a new part
		 */
		Part<Resource> resource(Consumer<Part<Resource>> partConfigurer);
	}
}
