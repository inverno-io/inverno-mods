/*
 * Copyright 2020 Jeremy KUHN
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
package io.inverno.mod.http.server;

import io.inverno.mod.http.base.OutboundData;
import io.netty.buffer.ByteBuf;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Represents the payload body of a server response in a server exchange.
 * </p>
 *
 * <p>
 * The response body basically provides multiple ways to produce the response payload.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
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
	 * This can be used in an exchange interceptor in order to decorate response data publisher.
	 * </p>
	 *
	 * @param transformer a request payload publisher transformer
	 *
	 * @return the response body
	 * 
	 * @throws IllegalArgumentException if data were already sent to the client
	 */
	ResponseBody transform(Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer) throws IllegalArgumentException;

	/**
	 * <p>
	 * Transforms the payload publisher to subscribe to the specified publisher before subscribing to the payload publisher.
	 * </p>
	 *
	 * <p>
	 * This basically allows to perform actions before the response is actually sent.
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
	ResponseBody.Resource resource();
	
	/**
	 * <p>
	 * Returns a server-sent events payload producer as defined by <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events</a>.
	 * </p>
	 * 
	 * <p>
	 * A typical usage is:
	 * </p>
	 * 
	 * <pre>{@code
	 * exchange.response().body().sse().from(
	 *     (events, data) -> Flux.interval(Duration.ofSeconds(1))
	 *         .map(seq -> events.create(event -> event
	 *                 .id(Long.toString(seq))
	 *                 .event("seq")
	 *                 .value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Event #" + seq, Charsets.DEFAULT)))
	 *             )
	 *         )
	 * );
	 * }</pre>
	 * 
	 * @return a server-sent events payload producer
	 */
	ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> sse();
	
	/**
	 * <p>
	 * Returns a server-sent events payload producer as defined by <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events</a>.
	 * </p>
	 * 
	 * <p>
	 * A typical usage is:
	 * </p>
	 * 
	 * <pre>{@code
	 * exchange.response().body().sseString().from(
	 *     (events, data) -> Flux.interval(Duration.ofSeconds(1))
	 *         .map(seq -> events.create(event -> event
	 *                 .id(Long.toString(seq))
	 *                 .event("seq")
	 *                 .value("Event #" + seq, Charsets.DEFAULT))
	 *             )
	 *         )
	 * );
	 * }</pre>
	 * 
	 * @param <T> The type of char sequence
	 * 
	 * @return a server-sent events payload producer
	 */
	<T extends CharSequence> ResponseBody.Sse<T, ResponseBody.Sse.Event<T>, ResponseBody.Sse.EventFactory<T, ResponseBody.Sse.Event<T>>> sseString();
	
	/**
	 * <p>
	 * A resource payload producer.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	interface Resource {
		
		/**
		 * <p>
		 * Sets the specified resource in the response payload.
		 * </p>
		 * 
		 * <p>
		 * This method tries to determine the content type of the specified resource in
		 * which case the content type header is set in the response.
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
	 * A server-sent events payload producer as defined by
	 * <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events</a>.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 *
	 * @param <A> the type of data sent in the event
	 * @param <B> the server-sent event type
	 * @param <C> the server-sent event factory
	 */
	interface Sse<A, B extends ResponseBody.Sse.Event<A>, C extends ResponseBody.Sse.EventFactory<A, B>> {
		
		/**
		 * <p>
		 * Sets the server-sent events stream in the specified consumer using the server-sent event factory to create events and the server-sent
		 * events producer to sets the stream of events.
		 * </p>
		 *
		 * @param data a function in which server-sent events stream must be set
		 */
		void from(BiConsumer<C, OutboundData<B>> data);
		
		/**
		 * <p>
		 * Represents a server-sent event.
		 * </p>
		 * 
		 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
		 * @since 1.0
		 *
		 * @param <A> the type of data sent in the event
		 */
		interface Event<A> extends OutboundData<A> {
			
			/**
			 * <p>
			 * Sets the event id.
			 * </p>
			 * 
			 * @param id an id
			 * 
			 * @return the event
			 */
			ResponseBody.Sse.Event<A> id(String id);
			
			/**
			 * <p>
			 * Sets the event comment.
			 * </p>
			 * 
			 * @param comment a comment
			 * 
			 * @return the event
			 */
			ResponseBody.Sse.Event<A> comment(String comment);
			
			/**
			 * <p>
			 * Sets the type of event.
			 * </p>
			 * 
			 * @param event an event type
			 * 
			 * @return the event
			 */
			ResponseBody.Sse.Event<A> event(String event);
		}

		/**
		 * <p>
		 * A server-sent event factory is used to create server-sent events.
		 * </p>
		 * 
		 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
		 * @since 1.0
		 *
		 * @param <A> the type of data sent in the event data
		 * @param <B> the server-sent event type
		 */
		@FunctionalInterface
		interface EventFactory<A, B extends ResponseBody.Sse.Event<A>> {
			
			/**
			 * <p>
			 * Creates a server-sent event with the specified configurer.
			 * </p>
			 * 
			 * @param configurer a server-sent event configurer
			 * 
			 * @return a new server-sent event
			 */
			B create(Consumer<B> configurer);
			
			/**
			 * <p>
			 * Creates an empty server-sent event.
			 * </p>
			 * 
			 * @return a new server-sent event
			 */
			default B create() {
				return this.create(event -> {});
			}
		}
	}
}
