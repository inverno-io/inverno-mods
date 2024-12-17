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
 * Binds the value of an HTTP query parameter to a Web route method parameter whose name indicates the name of the parameter.
 * </p>
 *
 * <p>
 * The annotated parameter can be of any type as long as it can be decoded by a parameter converter.
 * </p>
 *
 * <p>
 * When specifying a Web server route, a query parameter can be defined as optional when the method parameter is an {@link java.util.Optional}, it is otherwise considered as required and a
 * {@link io.inverno.mod.web.base.MissingRequiredParameterException} will be thrown if the route is invoked with the parameter missing.
 * </p>
 *
 * <p>
 * If a request is received containing multiple query parameters with the same name, the first one is bound unless the method parameter is an array, a {@link java.util.Collection}, a
 * {@link java.util.List} or a {@link java.util.Set} in which case all values are bound. A parameter converter may also split raw values around a value separator (eg. value {@code 1,2,3,4} can be
 * bound to a list of integers).
 * </p>
 *
 * <pre>{@code
 * @WebRoute( ... )
 * void handler(@QueryParam int requiredParameter)
 *
 * @WebRoute( ... )
 * void handler(@QueryParam Optional<Integer> optionalParameter)
 *
 * @WebRoute( ... )
 * void handler(@QueryParam List<Integer> multiValueParameter)
 * }</pre>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see io.inverno.mod.base.converter.ObjectConverter
 */
@Documented
@Retention(SOURCE)
@Target({ PARAMETER })
public @interface QueryParam {

	/**
	 * <p>
	 * The query parameter name.
	 * </p>
	 *
	 * <p>
	 * This allows to override the name which is otherwise the Web route method parameter name, typically when the actual parameter name is not a valid Java identifier.
	 * </p>
	 *
	 * @return a parameter name
	 */
	String name() default "";
}
