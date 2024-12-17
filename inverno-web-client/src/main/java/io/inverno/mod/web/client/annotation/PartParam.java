/*
 * Copyright 2024 Jeremy Kuhn
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

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * <p>
 * Binds a Web route method parameter to a body part.
 * </p>
 *
 * <p>
 * The name of the body part is the method parameter name by default and it can be overridden by specifying the {@link #name()} attribute.
 * </p>
 *
 * <p>
 * The type of the annotated parameter can either be:
 * </p>
 *
 * <ul>
 * <li>{@link io.netty.buffer.ByteBuf}, {@code Mono<ByteBuf>}, {@code Flux<ByteBuf>} or {@code Publisher<ByteBuf>} to produce raw data</li>
 * <li>{@link String}, {@code Mono<String>}, {@code Flux<String>} or {@code Publisher<String>} to produce string data</li>
 * <li>{@code T}, {@code Mono<T>}, {@code Flux<T>} or {@code Publisher<T>} to produce data encoded based on the part content type specified in the {@link #contentType()} attribute.</li>
 * <li>{@link io.inverno.mod.base.resource.Resource} to produce resource data, the part filename is then set to the resource file name by default, this can be overridden by specifying the
 * {@link #filename()} attribute.</li>
 * </ul>
 *
 * <p>
 * An encoded part payload is decoded encoded using one of the {@link io.inverno.mod.base.converter.MediaTypeConverter} injected in the Web client module and corresponding to the part content type.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see WebRoute
 * @see io.inverno.mod.base.converter.MediaTypeConverter
 */
@Documented
@Retention(SOURCE)
@Target({ PARAMETER })
public @interface PartParam {

	/**
	 * <p>
	 * Specifies the name of the body part.
	 * </p>
	 *
	 * @return the part name
	 */
	String name() default "";

	/**
	 * <p>
	 * Specifies the body part file name.
	 * </p>
	 *
	 * @return the part file name
	 */
	String filename() default "";

	/**
	 * <p>
	 * Specifies the content type of the body part.
	 * </p>
	 *
	 * @return the part content type
	 */
	String contentType() default "";
}
