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

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;
import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.web.server.WebPart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Binds the payload of a request to a web route method parameter.
 * </p>
 *
 * <p>
 * The type of the annotated parameter can either be:
 * </p>
 *
 * <ul>
 * <li>{@link ByteBuf}, {@code Mono<ByteBuf>}, {@code Flux<ByteBuf>} or {@code Publisher<ByteBuf>} to consume raw data</li>
 * <li><code>Mono&lt;{@link Parameter}&gt;</code>, {@code Flux<Parameter>} or {@code Publisher<Parameter>} to consume URL encoded form data</li>
 * <li>{@code Mono<T>}, {@code Flux<T>} or {@code Publisher<T>} where type {@code T} is a super type of {@link WebPart} to consume multipart form data</li>
 * <li>{@code T}, {@code Mono<T>}, {@code Flux<T>} or {@code Publisher<T>} where type {@code T} is neither a {@link ByteBuf}, nor a {@link Parameter}, nor a super type of {@link WebPart} to consume
 * decoded data based on the content type of the request.</li>
 * </ul>
 *
 * <p>
 * An encoded request payload is decoded using one of the {@link MediaTypeConverter} injected in the web server module and corresponding to the content type of the request.
 * </p>
 *
 * <pre>{@code
 * @WebRoute( method = Method.POST, consumes = MediaTypes.APPLICATION_JSON )
 * public void create(Book book) {
 *     ...
 * }
 *
 * @WebRoute( method = Method.POST, consumes = MediaTypes.APPLICATION_X_NDJSON )
 * public void createReactive(Flux<Book> book) {
 *     ...
 * }
 * }</pre>
 *
 * <p>
 * This annotation can't be used in conjunction with {@link FormParam @FormParam}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see WebRoute
 * @see Mono
 * @see Flux
 * @see Publisher
 * @see WebPart
 * @see MediaTypeConverter
 */
@Documented
@Retention(SOURCE)
@Target({ PARAMETER })
public @interface Body {

}
