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
package io.winterframework.mod.web.annotation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.netty.buffer.ByteBuf;
import io.winterframework.mod.base.converter.MediaTypeConverter;
import io.winterframework.mod.base.resource.MediaTypes;
import io.winterframework.mod.http.server.ResponseBody;
import io.winterframework.mod.http.server.ResponseBody.Sse.EventFactory;

/**
 * <p>
 * Binds a server-sent events {@link EventFactory factory} to a web route method
 * parameter.
 * </p>
 * 
 * <p>
 * The type of the annotated parameter can either:
 * </p>
 * 
 * <ul>
 * <li>be a {@link ResponseBody.Sse.EventFactory
 * ResponseBody.Sse.EventFactory&lt;ByteBuf,
 * ResponseBody.Sse.Event&lt;ByteBuf&gt;&gt;} to produce server-sent event with
 * raw payload</li>
 * <li>considering type {@code T} which is not a {@link ByteBuf}, be a
 * {@link ResponseBody.Sse.EventFactory ResponseBody.Sse.EventFactory&lt;T,
 * ResponseBody.Sse.Event&lt;T&gt;&gt;} to produce server-sent event with
 * encoded data</li>
 * </ul>
 * 
 * <p>
 * The server-sent events factory is used in the handler to create server-sent
 * events that are usually emitted in a {@code Mono}, {@code Flux} or
 * {@code Publisher} returned by the route handler.
 * </p>
 * 
 * <p>
 * The event data is encoded using one of the {@link MediaTypeConverter}
 * injected in the web module and corresponding to the media type specified in
 * the annotation.
 * </p>
 * 
 * <blockquote>
 * 
 * <pre>
 * &#64;WebRoute(path = "/events/raw", method = Method.GET)
 * public Publisher{@literal<ResponseBody.Sse.Event<ByteBuf>>} get_raw_events(@SseEventFactory ResponseBody.Sse.EventFactory{@literal <ByteBuf, ResponseBody.Sse.Event<ByteBuf>>} events) {
 *     return Flux.interval(Duration.ofSeconds(1))
 *         .map(seq -> events.create(
 *             event -> event
 *                 .id(Long.toString(seq))
 *                 .event("event_raw")
 *                 .value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Event " + seq, Charsets.DEFAULT)))
 *             )
 *         );
 * }
 * 
 * &#64;WebRoute(path = "/events/encoded", method = Method.GET)
 * public Publisher{@literal <ResponseBody.Sse.Event<ByteBuf>>} get_encoded_events(@SseEventFactory(MediaTypes.APPLICATION_JSON) ResponseBody.Sse.EventFactory{@literal <Book, ResponseBody.Sse.Event<Book>>} events) {
 *     return Flux.interval(Duration.ofSeconds(1))
 *         .map(seq -> events.create(
 *             event -> event
 *                 .id(Long.toString(seq))
 *                 .event("event_encoded")
 *                 .value(new Book(...))
 *             )
 *         );
 * }
 * </pre>
 * 
 * </blockquote>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 *
 */
@Documented
@Retention(SOURCE)
@Target({ PARAMETER })
public @interface SseEventFactory {

	/**
	 * <p>
	 * Returns the media type of the server-sent event data.
	 * </p>
	 * 
	 * <p>
	 * This media type is used to determine the {@link MediaTypeConverter} to use to
	 * encode the data of the event.
	 * </p>
	 * 
	 * @return a media type
	 */
	String value() default MediaTypes.TEXT_PLAIN;
}
