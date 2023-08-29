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
package io.inverno.mod.web.annotation;

import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.http.base.ws.WebSocketMessage;
import io.inverno.mod.web.Web2SocketExchange;
import io.netty.buffer.ByteBuf;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Specifies a WebSocket route in a web controller.
 * </p>
 * 
 * <p>
 * A WebSocket route is an annotated method in a web controller which basically implement the WebSocket exchange handler logic. It accepts any combination of the following parameters:
 * </p>
 * 
 * <ul>
 * <li>{@link Web2SocketExchange} which is the WebSocket exchange to handle</li>
 * <li>{@link Web2SocketExchange.Inbound} which is the WebSocket inbound</li>
 * <li>{@link Web2SocketExchange.Outbound} which is the WebSocket outbound</li>
 * <li>Any publisher (i.e. {@link Publisher}, {@link Flux} or {@link Mono}) of {@link ByteBuf}, {@code String} or {@code T} as WebSocket inbound.</li>
 * </ul>
 * 
 * <p>
 * The method's return type can also be used to specify the WebSocket outbound as a publisher of {@link ByteBuf}, {@code String} or {@code T}, in which case it is not possible to specify it as
 * a method parameter.
 * </p>
 * 
 * <p>
 * When an inbound or outbound publisher is specified with {@code T} other than {@link ByteBuf} or {@code String}, WebSocket messages are automcatically decoded or encoded using the
 * {@link MediaTypeConverter} corresponding to the negotiated subprotocol as defined by {@link Web2SocketExchange}.
 * </p>
 * 
 * <p>
 * A simple Websocket route can be defined as follows in a web controller bean:
 * </p>
 * 
 * <pre>{@code
 * @WebController( path = "/chat" )
 * @Bean
 * public class ChatController {
 * 
 *     @WebRoute( path = "ws", subprotocol = json" )
 *     public Flux<Message> chatWebSocket(Flux<Message> inbound) {
 *         ...
 *     }
 * }
 * }</pre>
 * 
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.METHOD })
@Inherited
public @interface WebSocketRoute {
	
	/**
	 * <p>
	 * The path that the absolute path of a request must match to be served by the route.
	 * </p>
	 *
	 * <p>
	 * A path can be a parameterized path parameters as defined by {@link URIBuilder}.
	 * </p>
	 *
	 * <p>
	 * Note that this path will be normalized in the resulting route.
	 * </p>
	 *
	 * @return an array of path
	 */
	String[] path() default {};

	/**
	 * <p>
	 * Indicates whether trailing slash in the request path should be matched by the route.
	 * </p>
	 *
	 * <p>
	 * When activated, the route matches a path regardless of whether it ends with a slash as long as the path matches one of the paths specified in the route with or without trailing slash.
	 * </p>
	 *
	 * @return true to match path with or without trailing slash, false otherwise
	 */
	boolean matchTrailingSlash() default false;

	/**
	 * <p>
	 * The list of languages that a request must match (i.e. accept at least one of them) to be served by the route.
	 * </p>
	 *
	 * @return an array of language tags
	 */
	String[] language() default {};
	
	/**
	 * <p>
	 * The list of WebSocket subprotocols supported by the WebSocket route.
	 * </p>
	 * 
	 * @return an array of subprotocols
	 */
	String[] subprotocol() default {};
	
	/**
	 * <p>
	 * The type of WebSocket messages consumed and produced by the WebSocket route.
	 * </p>
	 * 
	 * <p>
	 * This is only relevant when the route is defined with inbound and outbound publishers otherwise the message type shall be determined in the WebSocket route handler.
	 * </p>
	 * 
	 * @return a WebSocket message type
	 */
	WebSocketMessage.Kind messageType() default WebSocketMessage.Kind.TEXT;
}
