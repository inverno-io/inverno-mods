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
package io.inverno.mod.web.client.annotation;

import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.client.InterceptedWebExchange;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * <p>
 * Specifies a Web route in a Web client stub.
 * </p>
 *
 * <p>
 * A Web route is an annotated method in a Web client stub interface which basically defines the resource endpoints exposed on an HTTP server that needs to be consumed in an application. The method
 * parameters can be bound to the request parameters or the request body using annotations like {@link io.inverno.mod.web.base.annotation.CookieParam @CookieParam},
 * {@link io.inverno.mod.web.base.annotation.FormParam @FormParam}, {@link io.inverno.mod.web.base.annotation.HeaderParam @HeaderParam},
 * {@link io.inverno.mod.web.base.annotation.PathParam @PathParam}, {@link io.inverno.mod.web.base.annotation.QueryParam @QueryParam},
 * {@link PartParam @PartParam} and {@link io.inverno.mod.web.base.annotation.Body @Body}.
 * </p>
 *
 * <p>
 * Some specific method parameters can be specified when needed like a {@link io.inverno.mod.web.client.WebExchange.Configurer WebExchange.Configurer&lt;? extends T&gt;} where {@code T} is the type of the
 * exchange context to give access to the actual Web exchange.
 * </p>
 *
 * <p>
 * The method's return type should eventually expose the response from the server and it must be reactive, the HTTP request being only sent when the response {@code Mono} is subscribed. A Web route
 * method can then return any of:
 * </p>
 *
 * <ul>
 * <li>{@code Mono<Void>}, {@code Flux<Void>} or {@code Publisher<Void>} when no response body is expected in which case the returned publisher normally fails when an error status (i.e. {@code 4xx} or
 * {@code 5xx}) is received from the server. This behaviour can be controlled with {@link io.inverno.mod.web.client.WebExchange#failOnErrorStatus(boolean)} and
 * {@link InterceptedWebExchange#failOnErrorStatus(java.util.function.Function)}.</li>
 * <li>{@code Mono<WebExchange<? extends T>>} where {@code T} is the type of the exchange context when there is a need to access the Web exchange before actually sending the request which is only sent
 * when the {@link io.inverno.mod.web.client.WebExchange#response()} {@code Mono} is subscribed. Note that this can also be achieved with a {@link io.inverno.mod.web.client.WebExchange.Configurer} in
 * the method parameters.</li>
 * <li>{@code Mono<Response>} when there is a need to access response status, headers...</li>
 * <li>{@code Mono<ByteBuf>}, {@code Flux<ByteBuf>} or {@code Publisher<ByteBuf>} in order to consume payload as raw data.</li>
 * <li>{@code Mono<String>}, {@code Flux<String>} or {@code Publisher<String>} in order to consume payload as string data.</li>
 * <li>{@code Mono<T>}, {@code Flux<T>} or {@code Publisher<T>} where {@code T} is none of the above in order to consume payload as encoded data.</li>
 * </ul>
 *
 * <p>
 * An encoded response payload is decoded using one of the {@link io.inverno.mod.base.converter.MediaTypeConverter} injected in the Web client module and corresponding to the content type of the
 * response. If no matching converter could be found a {@link io.inverno.mod.web.base.MissingConverterException} stating that no media was specified will be thrown.
 * </p>
 *
 * <p>
 * A simple Web route can be defined as follows in a Web client stub interface:
 * </p>
 *
 * <pre>{@code
 * @WebClient( uri = "http://host:8080/book" )
 * public interface BookClient {
 *
 *     @WebRoute( path = "{id}", method = Method.PUT, produces = "application/json", consumes = "application/json" )
 *     Book update(@PathParam String id, @Body Book book);
 * }
 * }</pre>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
@Documented
@Retention(SOURCE)
@Target({ METHOD })
@Inherited
public @interface WebRoute {

	/**
	 * <p>
	 * The HTTP method to use to consume the destination resource.
	 * </p>
	 *
	 * @return an HTTP method
	 */
	Method method() default Method.GET;

	/**
	 * <p>
	 * The path to the destination resource relative to the destination URI.
	 * </p>
	 *
	 * <p>
	 * A path can be a parameterized path as defined by {@link io.inverno.mod.base.net.URIBuilder}. Path parameter can then be passed to the Web route method by defining
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
	 * The list of media ranges that the request accepts from the server in the response as defined by <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section 5.3.2</a>.
	 * </p>
	 *
	 * @return an array of media ranges
	 */
	String[] consumes() default {};

	/**
	 * <p>
	 * The media types of the content produced by the request as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7231#section-3.1.1.5">RFC 7231 Section 3.1.1.5</a>.
	 * </p>
	 *
	 * <p>
	 * The request body content type basically specifies which {@link io.inverno.mod.base.converter.MediaTypeConverter} is be used to encode the request payload.
	 * </p>
	 *
	 * @return a media type
	 */
	String produces() default "";

	/**
	 * <p>
	 * The list of language tags that the request accepts from the server in the response as defined by
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7231#section-5.3.5">RFC 7231 Section 5.3.5</a>.
	 * </p>
	 *
	 * @return an array of language tags
	 */
	String[] language() default {};
}
