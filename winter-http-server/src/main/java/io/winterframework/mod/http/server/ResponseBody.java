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
package io.winterframework.mod.http.server;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;

/**
 * <p>
 * Represents the payload body of a server response in a server exchange.
 * </p>
 * 
 * <p>
 * The response body basically provides multiple ways to produce the response
 * payload.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Response
 */
public interface ResponseBody {

	/**
	 * <p>
	 * Produces an empty payload.
	 * </p>
	 * 
	 * <p>
	 * If a payload has already been provided this method does nothing.
	 * </p>
	 * 
	 * <p>A typical usage is:</p>
	 * 
	 * <blockquote><pre>
	 * exchange.response().body().empty();
	 * </pre></blockquote>
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
	 * <blockquote><pre>
	 * exchange.response().body().raw().stream(
	 *     Flux.just(
	 *         Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hello ", Charsets.DEFAULT)), 
	 *         Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("World!", Charsets.DEFAULT))
	 *     )
	 * );
	 * </pre></blockquote>
	 * 
	 * @return a raw payload producer
	 */
	ResponseData<ByteBuf> raw();
	
	/**
	 * <p>
	 * Returns a string payload producer.
	 * </p>
	 * 
	 * <p>
	 * A typical usage is:
	 * </p>
	 * 
	 * <blockquote><pre>
	 * exchange.response().body().string().stream(
	 *     Flux.just(
	 *         Unpooled.unreleasableBuffer("Hello "), 
	 *         Unpooled.unreleasableBuffer("World!")
	 *     )
	 * );
	 * </pre></blockquote>
	 * 
	 * @return a string payload producer
	 */
	<T extends CharSequence> ResponseData<T> string();
	
	/**
	 * <p>
	 * Returns a resource payload producer.
	 * </p>
	 * 
	 * <p>
	 * A typical usage is:
	 * </p>
	 * 
	 * <blockquote><pre>
	 * ResourceService resourceService = ... 
	 * exchange.response().body().resource().value(resourceService.get("file:/path/to/resource");
	 * </pre></blockquote>
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
	 * <blockquote><pre>
	 * exchange.response().body().sse().from(
	 *     (events, data) -> Flux.interval(Duration.ofSeconds(1))
	 *         .map(seq -> events.create(event -> event
	 *                 .id(Long.toString(seq))
	 *                 .event("seq")
	 *                 .value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Event #" + seq, Charsets.DEFAULT)))
	 *             )
	 *         )
	 * );
	 * </pre></blockquote>
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
	 * <blockquote><pre>
	 * exchange.response().body().sseString().from(
	 *     (events, data) -> Flux.interval(Duration.ofSeconds(1))
	 *         .map(seq -> events.create(event -> event
	 *                 .id(Long.toString(seq))
	 *                 .event("seq")
	 *                 .value("Event #" + seq, Charsets.DEFAULT))
	 *             )
	 *         )
	 * );
	 * </pre></blockquote>
	 * 
	 * @return a server-sent events payload producer
	 */
	<T extends CharSequence> ResponseBody.Sse<T, ResponseBody.Sse.Event<T>, ResponseBody.Sse.EventFactory<T, ResponseBody.Sse.Event<T>>> sseString();
	
	/**
	 * <p>
	 * A resource payload producer.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	public static interface Resource {
		
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
		 * @throws IllegalStateException if the payload has already been set
		 */
		void value(io.winterframework.mod.base.resource.Resource resource) throws IllegalStateException;
	}
	
	/**
	 * <p>
	 * A server-sent events payload producer as defined by
	 * <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events</a>.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
	 * @since 1.0
	 *
	 * @param <A> the type of data sent in the event
	 * @param <B> the server-sent event type
	 * @param <C> the server-sent event factory
	 */
	public interface Sse<A, B extends ResponseBody.Sse.Event<A>, C extends ResponseBody.Sse.EventFactory<A, B>> {
		
		/**
		 * <p>
		 * Sets the server-sent events stream in the specified consumer using the
		 * server-sent event factory to create events and the server-sent events
		 * producer to sets the stream of events.
		 * </p>
		 * 
		 * @param data a function in which server-sent events stream must be set
		 */
		void from(BiConsumer<C, ResponseData<B>> data);
		
		/**
		 * <p>
		 * Represents a server-sent event.
		 * </p>
		 * 
		 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
		 * @since 1.0
		 *
		 * @param <A> the type of data sent in the event
		 */
		public static interface Event<A> extends ResponseData<A> {
			
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
		 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
		 * @since 1.0
		 *
		 * @param <A> the type of data sent in the event data
		 * @param <B> the server-sent event type
		 */
		@FunctionalInterface
		public static interface EventFactory<A, B extends ResponseBody.Sse.Event<A>> {
			
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
