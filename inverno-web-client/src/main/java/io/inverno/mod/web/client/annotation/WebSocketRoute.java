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
package io.inverno.mod.web.client.annotation;

import io.inverno.mod.http.base.ws.WebSocketMessage;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Specifies a WebSocket route in a Web client stub.
 * </p>
 *
 * <p>
 * A WebSocket route is an annotated method in a Web client stub interface which basically defines the WebSocket endpoints exposed on an HTTP server to which an application needs to connect. As for a
 * {@link WebRoute}, method parameters can be bound the upgrading request parameter using annotations like {@link io.inverno.mod.web.base.annotation.CookieParam @CookieParam},
 * {@link io.inverno.mod.web.base.annotation.FormParam @FormParam}, {@link io.inverno.mod.web.base.annotation.HeaderParam @HeaderParam},
 * {@link io.inverno.mod.web.base.annotation.PathParam @PathParam} and {@link io.inverno.mod.web.base.annotation.QueryParam @QueryParam}.
 * </p>
 *
 * <p>
 * Some specific method parameters can be specified when needed like a {@link io.inverno.mod.web.client.ws.Web2SocketExchange.Configurer Web2SocketExchange.Configurer&lt;? extends T&gt;} where {@code T} is
 * the type of the exchange context to give access to the actual WebSocket exchange.
 * </p>
 *
 * <p>
 * WebSocket outbound data are also specified in the method parameters as:
 * </p>
 *
 * <ul>
 * <li>{@code Consumer<BaseWeb2SocketExchange.Outbound>} in order to explicitly configure the WebSocket outbound.</li>
 * <li>{@code Mono<ByteBuf>}, {@code Flux<ByteBuf>} or {@code Publisher<ByteBuf>} in order to produce message payload as raw data.</li>
 * <li>{@code Mono<String>}, {@code Flux<String>} or {@code Publisher<String>} in order to produce message payload as string data.</li>
 * <li>{@code Mono<T>}, {@code Flux<T>} or {@code Publisher<T>} in order to produce message payload as encoded data in which case the negotiated subprotocol is used to determine the
 * {@link io.inverno.mod.base.converter.MediaTypeConverter} to use to encode message content.</li>
 * </ul>
 *
 * <p>
 * The method's return type should eventually expose the WebSocket inbound data received from the server and it must be reactive, the WebSocket upgrade request being only sent when the WebSocket
 * {@code Mono} is subscribed. A WebSocket route method can then return any of:
 * </p>
 *
 * <ul>
 * <li>{@code Mono<Web2SocketExchange<? extends T>>} where {@code T} is the type of the exchange context when there is a need to access the WebSocket exchange before actually sending the upgrade
 * request which is only sent when the {@link io.inverno.mod.web.client.WebExchange#webSocket()} {@code Mono} is subscribed. Note that this can also be achieved with a
 * {@link io.inverno.mod.web.client.ws.Web2SocketExchange.Configurer} in the method parameters.</li>
 * <li>{@code Mono<Inbound>} when there is a need to explicitly access the WebSocket inbound</li>
 * <li>{@code Mono<ByteBuf>}, {@code Flux<ByteBuf>} or {@code Publisher<ByteBuf>} in order to consume message payload as raw data.</li>
 * <li>{@code Mono<String>}, {@code Flux<String>} or {@code Publisher<String>} in order to consume message payload as string data.</li>
 * <li>{@code Mono<T>}, {@code Flux<T>} or {@code Publisher<T>} in order to consume message payload as encoded data in which case the negotiated subprotocol is used to determine the
 * {@link io.inverno.mod.base.converter.MediaTypeConverter} to use to decode message content.</li>
 * </ul>
 *
 * <ul>
 * <li>{@code Web2SocketExchange} which is the WebSocket exchange to handle</li>
 * <li>{@link io.inverno.mod.web.base.ws.BaseWeb2SocketExchange.Inbound} which is the WebSocket inbound</li>
 * <li>{@link io.inverno.mod.web.base.ws.BaseWeb2SocketExchange.Outbound} which is the WebSocket outbound</li>
 * <li>Any publisher (i.e. {@link java.util.concurrent.Flow.Publisher}, {@link reactor.core.publisher.Flux} or {@link reactor.core.publisher.Mono}) of {@link io.netty.buffer.ByteBuf}, {@code String}
 * or {@code T} as WebSocket inbound.</li>
 * </ul>
 *
 *
 * <p>
 * In above inbound and outbound data definition, each published element is interpreted as a single message content, in order to send or receive messages with fragmented payloads, publishers of
 * publishers can be specified as for instance {@code Flux<Flux<String>>}.
 * </p>
 *
 * <p>
 * A simple WebSocket endpoint can be defined as follows in a Web client stub:
 * </p>
 * 
 * <pre>{@code
 * @WebClient( uri = "http://host:8080/chat" )
 * public interface ChatClient {
 * 
 *     @WebRoute( subprotocol = json" )
 *     Flux<Message> joinChat(Flux<Message> outbound);
 * }
 * }</pre>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.METHOD })
@Inherited
public @interface WebSocketRoute {
	
	/**
	 * <p>
	 * The path to the WebSocket endpoint relative to the destination URI.
	 * </p>
	 *
	 * <p>
	 * A path can be a parameterized path as defined by {@link io.inverno.mod.base.net.URIBuilder}. Path parameter can then be passed to the WebSocket route method by defining
	 * {@link io.inverno.mod.web.base.annotation.PathParam path parameters}.
	 * </p>
	 *
	 * <p>
	 * Note that this path will be normalized in the resulting request.
	 * </p>
	 *
	 * @return a path
	 */
	String path() default "";

	/**
	 * <p>
	 * The list of language tags that the request accepts from the server in the response as defined by
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7231#section-5.3.5">RFC 7231 Section 5.3.5</a>.
	 * </p>
	 *
	 * @return an array of language tags
	 */
	String[] language() default {};
	
	/**
	 * <p>
	 * The subprotocol specifying the message format and used to resolve the {@link io.inverno.mod.base.converter.MediaTypeConverter} used to encode and decode inbound and outbound messages.
	 * </p>
	 * 
	 * @return a subprotocol
	 */
	String subprotocol() default "";
	
	/**
	 * <p>
	 * The type of WebSocket messages consumed and produced by the WebSocket.
	 * </p>
	 *
	 * @return a WebSocket message type
	 */
	WebSocketMessage.Kind messageType() default WebSocketMessage.Kind.TEXT;

	/**
	 * <p>
	 * Specifies whether the WebSocket exchange should be closed when the outbound frames publisher completes successfully.
	 * </p>
	 * 
	 * @return true to close the WebSocket exchange when the outbound frames publisher completes, false otherwise
	 * 
	 * @see io.inverno.mod.http.base.ws.BaseWebSocketExchange.Outbound#closeOnComplete(boolean)
	 */
	boolean closeOnComplete() default true;
}
