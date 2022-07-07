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
package io.inverno.mod.web.annotation;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.web.WebRouterConfigurer;
import io.inverno.mod.web.WebRoutesConfigurer;
import io.inverno.mod.web.WebServerControllerConfigurer;
import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * The WebRoutes annotation is used in conjunction with the {@link Bean @Bean} annotation to indicate a web routes configurer bean.
 * </p>
 *
 * <p>
 * Web router configurer beans must implements {@link WebRoutesConfigurer} or {@link WebRouterConfigurer}, they are used to programmatically specifies web routes and/or WebSocket routes in a module.
 * They are injected in a generated {@link WebServerControllerConfigurer} bean that can be used to configure a web server.
 * </p>
 *
 * <p>
 * The routes configured in the bean can be declared in the annotation, these information are then used at compile time to detect conflicts and generate documentation.
 * </p>
 *
 * <pre>{@code
 * @WebRoutes(
 *     value = {
 *         @WebRoute(path = { "/book" })
 *         @WebRoute(path = { "/book/{id}" })
 *     },
 *     webSockets = {
 *         @WebSocketRoute(path = { "/ws" })
 *     }
 * )
 * @Bean
 * public class CustomWebRouterConfigurer implements WebRouterConfigurer<WebExchange<EchangeContext>> {
 *     ...
 * }
 * }</pre>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see WebRouterConfigurer
 * @see WebController
 * @see WebRoute
 * @see WebSocketRoute
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ TYPE })
public @interface WebRoutes {

	/**
	 * <p>
	 * Returns the web routes configured in the web router configurer.
	 * </p>
	 * 
	 * <p>
	 * Using these information, the compiler will be able to report route definition clash: different route defined using the same parameters that are likely to conflict at runtime. It also uses them
	 * to generate documentation (OpenAPI specification).
	 * </p>
	 * 
	 * <p>
	 * If you want to define undocumented routes and you know a route will not conflict with another one, you can omit to declare them.
	 * </p>
	 * 
	 * @return an array of web route
	 */
	WebRoute[] value() default {}; 
	
	/**
	 * <p>
	 * Returns the WebSocket routes configured in the web router configurer.
	 * </p>
	 * 
	 * <p>
	 * Using these information, the compiler will be able to report route definition clash: different route defined using the same parameters that are likely to conflict at runtime. It also uses them
	 * to generate documentation (OpenAPI specification).
	 * </p>
	 * 
	 * <p>
	 * If you want to define undocumented routes and you know a route will not conflict with another one, you can omit to declare them.
	 * </p>
	 * 
	 * @return an array of WebSocket route
	 */
	WebSocketRoute[] webSockets() default {};
}
