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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.web.server.MissingRequiredParameterException;

/**
 * <p>
 * Binds the value of a HTTP header to a web route method parameter whose name indicates the name of the header.
 * </p>
 *
 * <p>
 * The annotated parameter can be of any type as long as it can be decoded by the parameter converter injected in the web server module.
 * </p>
 *
 * <p>
 * A header parameter can be defined as optional when the method parameter is an {@link Optional}, it is otherwise considered as required and a {@link MissingRequiredParameterException} will be thrown
 * if the route is invoked with the parameter missing.
 * </p>
 *
 * <p>
 * If the request contains multiple headers with the same name, the first one is bound unless the method parameter is an array, a {@link Collection}, a {@link List} or a {@link Set} in which case all
 * values are bound. The parameter converter may also split raw values around a value separator (eg. value {@code 1,2,3,4} can be bound to a list of integers).
 * </p>
 *
 * <pre>{@code
 * @WebRoute( ... )
 * public void handler(@HeaderParam int requiredHeader) {
 *     ...
 * }
 *
 * @WebRoute( ... )
 * public void handler(@HeaderParam Optional<Integer> optionalHeader) {
 *     ...
 * }
 *
 * @WebRoute( ... )
 * public void handler(@HeaderParam List<Integer> multiValueHeader) {
 *     ...
 * }
 * }</pre>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see WebRoute
 * @see ObjectConverter
 */
@Documented
@Retention(SOURCE)
@Target({ PARAMETER })
public @interface HeaderParam {

}
