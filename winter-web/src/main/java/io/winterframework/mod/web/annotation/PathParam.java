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
import java.util.Optional;

import io.winterframework.mod.base.converter.ObjectConverter;
import io.winterframework.mod.base.net.URIBuilder;
import io.winterframework.mod.web.MissingRequiredParameterException;

/**
 * <p>
 * Binds the value of a URI path parameter as defined by {@link URIBuilder} to a
 * web route method parameter whose name indicates the name of the parameter.
 * </p>
 * 
 * <p>
 * The annotated parameter can be of any type as long as it can be decoded by
 * the parameter converter injected in the web module.
 * </p>
 * 
 * <p>
 * A path parameter can be defined as optional when the method parameter is an
 * {@link Optional}, it is otherwise considered as required and a
 * {@link MissingRequiredParameterException} will be thrown if the route is
 * invoked with the parameter missing.
 * </p>
 * 
 * <p>
 * The parameter converter may split a value around a value separator (eg. value
 * {@code 1,2,3,4} can be bound to a list of integers).
 * </p>
 * 
 * <blockquote>
 * 
 * <pre>
 * &#64;WebRoute( path = "/{requiredParameter}" )
 * public void handler(@PathParam int requiredParameter) {
 *     ...
 * }
 * 
 * &#64;WebRoute( path = "/{optionalParameter}" )
 * public void handler(@QueryParam Optional{@literal <Integer>} optionalParameter) {
 *     ...
 * }
 * 
 * &#64;WebRoute( path = "/{multiValueParameter}" )
 * public void handler(@QueryParam List{@literal <Integer>} multiValueParameter) {
 *     ...
 * }
 * </pre>
 * 
 * </blockquote>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see WebRoute
 * @see ObjectConverter
 */
@Documented
@Retention(SOURCE)
@Target({ PARAMETER })
public @interface PathParam {

}
