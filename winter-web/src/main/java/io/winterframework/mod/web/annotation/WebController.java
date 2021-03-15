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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.winterframework.core.annotation.Bean;
import io.winterframework.mod.web.WebRouterConfigurer;

/**
 * <p>
 * The WebController annotation is used in conjunction with the
 * {@link Bean @Bean} annotation to indicate a web controller bean.
 * </p>
 * 
 * <p>
 * Web controller beans are used to specify web routes exposed in a module.
 * These routes are aggregated in a generated {@link WebRouterConfigurer} bean
 * that can be used to configure a web router.
 * </p>
 * 
 * <p>
 * Web routes are declared as methods annotated with {@link WebRoute @Webroute}
 * and implementing the exchange handler.
 * </p>
 * 
 * <p>
 * A web controller defines a base path, set to '/' by default, for all the
 * routes defined in the controller.
 * </p>
 * 
 * <p>
 * For instance, in the following example a request to {@code /book} is handled
 * by the {@code getList()} method and a request to {@code /book/1} by the
 * {@code get()} method.
 * </p>
 * 
 * <blockquote>
 * 
 * <pre>
 * &#64;WebController( path = "/book" )
 * &#64;Bean
 * public class BookResource {
 * 
 *     &#64;WebRoute
 *     public List{@literal <Book>} getList() {
 *         ...
 *     }
 *     
 *     &#64;WebRoute( path = "{id}" )
 *     public Book get() {
 *         ...
 *     }
 * }
 * </pre>
 * 
 * </blockquote>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Bean
 * @see WebRoute
 */
@Retention(SOURCE)
@Target(TYPE)
public @interface WebController {

	/**
	 * <p>
	 * The base path of all the routes defined in the web controller.
	 * </p>
	 * 
	 * @return the base path
	 */
	String path() default "/";
}
