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

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.winterframework.core.annotation.Bean;
import io.winterframework.mod.web.WebRouterConfigurer;

/**
 * <p>
 * The WebRoutes annotation is used in conjunction with the {@link Bean @Bean}
 * annotation to indicate a web router configurer bean.
 * </p>
 * 
 * <p>
 * Web router configurer beans must implements {@link WebRouterConfigurer}, they
 * are used to programmatically specifies web routes in a module. They are
 * injected in a generated {@link WebRouterConfigurer} bean that can be used to
 * configure a web router.
 * </p>
 * 
 * <p>
 * The web routes configured in the bean can be declared in the annotation,
 * these information are then used at compile time to generate documentation.
 * </p>
 * 
 * <blockquote>
 * 
 * <pre>
 * &#64;WebRoutes({
 *     &#64;WebRoute(path = { "/book" })
 *     &#64;WebRoute(path = { "/book/{id}" })
 * })
 * &#64;Bean
 * public class CustomWebRouterConfigurer implements WebRouterConfigurer{@literal <WebExchange>} {
 *     ...
 * }
 * </pre>
 * 
 * </blockquote>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see WebRouterConfigurer
 * @see WebController
 * @see WebRoute
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ TYPE })
public @interface WebRoutes {

	/**
	 * <p>
	 * Returns the web routes configured by the web router configurer.
	 * </p>
	 * 
	 * <p>
	 * This is mostly for documentation purposes, if you want to define undocumented
	 * routes, you can simply omit to declare them.
	 * </p>
	 * 
	 * @return an array of web route
	 */
	WebRoute[] value() default {}; 
}
