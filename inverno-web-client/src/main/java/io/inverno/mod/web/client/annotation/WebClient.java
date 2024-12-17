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

import io.inverno.core.annotation.Bean;
import io.inverno.mod.discovery.ServiceID;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * <p>
 * The {@code @WebClient} annotation indicates Web client stub interface.
 * </p>
 *
 * <p>
 * It must be set on an interface which specifies Web route stub pointing to resources exposed by an HTTP server. The {@link #uri()} attribute identifies the destination to which HTTP requests will be
 * sent, it must be a valid service URI. The Web compiler generates a bean implementing the Web client stub, the Web client stub interface can then be injected in a module to consume HTTP resources.
 * </p>
 *
 * <p>
 * Web routes are declared as methods annotated with {@link WebRoute @Webroute}.
 * </p>
 *
 * <p>
 * For instance, the following example will result in the generation of a {@code BookClient} bean which allows to list or get book resources exposed at {@code http://host:8080/book}.
 * </p>
 *
 * <pre>{@code
 * @Webclient( uri = "http://host:8080/book" )
 * public interface BookClient {
 *
 *     @WebRoute
 *     List<Book> getList();
 *
 *     @WebRoute( path = "/{id}" )
 *     Book get(@PathParam String id)
 * }
 * }</pre>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
@Retention(SOURCE)
@Target(TYPE)
public @interface WebClient {

	/**
	 * <p>
	 * Indicates a name identifying the Web client bean in the module, defaults to the name of the class.
	 * </p>
	 *
	 * @return A name
	 */
	String name() default "";

	/**
	 * <p>
	 * The destination URI.
	 * </p>
	 *
	 * <p>
	 * The destination URI must be a valid service URI as defined by {@link ServiceID}: it must be absolute (i.e. have a scheme), if it is not opaque it must have an authority
	 * component and the path component must be absolute.
	 * </p>
	 *
	 * @return the destination URI
	 */
	String uri();

	/**
	 * <p>
	 * Indicates the visibility of the Web client bean in the module.
	 * </p>
	 *
	 * @return The bean's visibility
	 */
	Bean.Visibility visibility() default Bean.Visibility.PUBLIC;
}
