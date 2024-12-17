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
package io.inverno.mod.web.base.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * <p>
 * Binds a request body to a Web route method parameter.
 * </p>
 *
 * <p>
 * The type of the annotated parameter can be any of:
 * </p>
 *
 * <ul>
 * <li>{@link io.netty.buffer.ByteBuf}, {@code Mono<ByteBuf>}, {@code Flux<ByteBuf>} or {@code Publisher<ByteBuf>} to produce or consume raw data.</li>
 * <li>{@link String}, {@code Mono<String>}, {@code Flux<String>} or {@code Publisher<String>} to produce or consume string data.</li>
 * <li>{@code T}, {@code Mono<T>}, {@code Flux<T>} or {@code Publisher<T>} where type {@code T} is the type of application resource to produce or consume data encoded or decoded based on the request
 * content type.</li>
 * </ul>
 *
 * <p>
 * Server Web routes also accept:
 * </p>
 *
 * <ul>
 * <li><code>Mono&lt;{@link io.inverno.mod.http.base.Parameter}&gt;</code>, {@code Flux<Parameter>} or {@code Publisher<Parameter>} to consume URL encoded form data.</li>
 * <li>{@code Mono<T>}, {@code Flux<T>} or {@code Publisher<T>} where type {@code T} is a super type of {@code WebPart} to consume multipart form data</li>
 * </ul>
 *
 * <p>
 * Client Web route also accept:
 * </p>
 *
 * <ul>
 * <li>{@link io.inverno.mod.base.resource.Resource} to send resources as request body in which case the request content type can be automatically deduced from the resource.</li>
 * </ul>
 *
 * <p>
 * An encoded payload is encoded or decoded using a {@link io.inverno.mod.base.converter.MediaTypeConverter} corresponding to the content type specified in the request.
 * </p>
 *
 * <p>
 * This annotation can't be used in conjunction with other parameters that specifies the request body format such as {@link FormParam @FormParam}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see io.inverno.mod.base.converter.MediaTypeConverter
 */
@Documented
@Retention(SOURCE)
@Target({ PARAMETER })
public @interface Body {

}
