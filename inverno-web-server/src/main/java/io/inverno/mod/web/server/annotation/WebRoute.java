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
package io.inverno.mod.web.server.annotation;

import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.http.base.InternalServerErrorException;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.web.server.WebExchange;
import io.inverno.mod.web.server.WebResponseBody;
import io.netty.buffer.ByteBuf;
import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.METHOD;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.SOURCE;
import java.lang.annotation.Target;

/**
 * <p>
 * Specifies a web route in a web controller.
 * </p>
 *
 * <p>
 * A web route is an annotated method in a web controller which basically implements the exchange handler logic. The method parameters can be bound to the request parameters or the request body using
 * annotations like  {@link CookieParam @CookieParam}, {@link FormParam @FormParam},
 * {@link HeaderParam @HeaderParam}, {@link PathParam @PathParam},
 * {@link QueryParam @QueryParam} and {@link Body @Body}.
 * </p>
 *
 * <p>
 * Some specific method parameter can also be specified when needed like the exchange being processed declared as {@link Exchange} or {@link WebExchange} and a server-sent event factory as described
 * by {@link SseEventFactory}.
 * </p>
 *
 * <p>
 * The method's return type describes the payload of the response, it can either be:
 * </p>
 *
 * <ul>
 * <li>{@code void} to produce an empty response</li>
 * <li>{@link ByteBuf}, {@code Mono<ByteBuf>}, {@code Flux<ByteBuf>} or {@code Publisher<ByteBuf>} to produce raw data</li>
 * <li>{@code Mono<T>}, {@code Flux<T>} or {@code Publisher<T>} where type {@code T} is a super type of {@link WebResponseBody.SseEncoder.Event
 * WebResponseBody.SseEncoder.Event&lt;ByteBuf&gt;} to produce server-sent events with raw data</li>
 * <li>{@code Mono<T>}, {@code Flux<T>} or {@code Publisher<T>} where type {@code T} is a super type of {@link WebResponseBody.SseEncoder.Event
 * WebResponseBody.SseEncoder.Event&lt;U&gt;} and U not a {@link ByteBuf} to produce server-sent events with encoded data based on the media type specified by the
 * {@link SseEventFactory @SseEventFactory} annotated method parameter</li>
 * <li>{@link Resource} to produce a static resource</li>
 * <li>{@code T}, {@code Mono<T>}, {@code Flux<T>} or {@code Publisher<T>} where {@code T} is none of the above to produce encoded data based on the content type of the response</li>
 * </ul>
 *
 * <p>
 * A response payload is encoded using one of the {@link MediaTypeConverter} injected in the web server module and corresponding to the content type of the response which is automatically set in case of
 * successful content negotiation, basically when there's a match between the media type produced by the route and what the client accepts, otherwise the response content type must be set explicitly
 * or an {@link InternalServerErrorException} stating that no media was specified will be thrown.
 * </p>
 *
 * <p>
 * A simple web route can be defined as follows in a web controller bean:
 * </p>
 *
 * <pre>{@code
 * @WebController( path = "/book" )
 * @Bean
 * public class BookController {
 *
 *     @WebRoute( path = "{id}", method = Method.PUT, consumes = "application/json", produces = "application/json" )
 *     public Book update(@Body Book book) {
 *         ...
 *     }
 * }
 * }</pre>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see WebController
 */
@Documented
@Retention(SOURCE)
@Target({ METHOD })
@Inherited
public @interface WebRoute {

	/**
	 * <p>
	 * The list of HTTP methods to which the method of a request must belong to be served by the route.
	 * </p>
	 *
	 * @return an array of HTTP methods
	 */
	Method[] method() default {};

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
	 * The list of media ranges that the content type of a request must match to be served by the route as defined by
	 * <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section 5.3.2</a>.
	 * </p>
	 *
	 * @return an array of media ranges
	 */
	String[] consumes() default {};

	/**
	 * <p>
	 * The list of media types that the request must match (i.e. accept at least one of them) to be served by the route.
	 * </p>
	 *
	 * @return an array of media types
	 */
	String[] produces() default {};

	/**
	 * <p>
	 * The list of languages that a request must match (i.e. accept at least one of them) to be served by the route.
	 * </p>
	 *
	 * @return an array of language tags
	 */
	String[] language() default {};
}
