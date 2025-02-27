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

import io.inverno.mod.http.base.InboundData;
import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Represents the response payload received by a client from a server in a client exchange.
 * </p>
 *
 * <p>
 * The response body basically provides multiple ways to consume the payload depending on the response content type.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @see Response
 */
public interface ResponseBody {
	
	/**
	 * <p>
	 * Transforms the payload publisher.
	 * </p>
	 *
	 * <p>
	 * This can be used to decorate the response data publisher before it has been subscribed to.
	 * </p>
	 * 
	 * @param transformer a payload publisher transformer
	 *
	 * @return the response body
	 * 
	 * @throws IllegalStateException if the response payload publisher has already been subscribed
	 */
	ResponseBody transform(Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer) throws IllegalStateException;

	/**
	 * <p>
	 * Transforms the payload publisher to subscribe to the specified publisher before subscribing to the payload publisher.
	 * </p>
	 *
	 * <p>
	 * This basically allows to perform actions before the response body is actually sent.
	 * </p>
	 *
	 * @param before the publisher to subscribe before the response body publisher
	 *
	 * @return the response body
	 */
	default ResponseBody before(Mono<Void> before) {
		return this.transform(before::thenMany);
	}

	/**
	 * <p>
	 * Transforms the payload publisher to subscribe to the specified publisher after payload publisher completion.
	 * </p>
	 *
	 * <p>
	 * This basically allows to perform actions after the response body has been sent.
	 * </p>
	 *
	 * @param after the publisher to subscribe before the response body publisher
	 *
	 * @return the response body
	 */
	default ResponseBody after(Mono<Void> after) {
		return this.transform(body -> Flux.from(body).concatWith(after.cast(ByteBuf.class)));
	}

	/**
	 * <p>
	 * Returns a raw payload consumer.
	 * </p>
	 *
	 * @return the raw data
	 */
	InboundData<ByteBuf> raw();
	
	/**
	 * <p>
	 * Returns a string payload consumer.
	 * </p>
	 *
	 * @return the string data
	 */
	InboundData<CharSequence> string() throws IllegalStateException;
	
	// TODO SSE
}
