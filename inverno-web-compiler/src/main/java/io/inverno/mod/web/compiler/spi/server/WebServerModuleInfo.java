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
package io.inverno.mod.web.compiler.spi.server;

import io.inverno.core.compiler.spi.Info;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.type.TypeMirror;

/**
 * <p>
 * Describes the module's Web server to generate.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public interface WebServerModuleInfo extends Info {

	@Override
	WebServerModuleQualifiedName getQualifiedName();

	/**
	 * <p>
	 * Returns the module element for which a Web sever is generated.
	 * </p>
	 *
	 * @return a module element
	 */
	ModuleElement getElement();

	/**
	 * <p>
	 * Returns the Web route interceptor configurers provided in the module.
	 * </p>
	 *
	 * <p>
	 * These can be defined as beans in the module or in component modules.
	 * </p>
	 *
	 * @return an array of interceptor configurers
	 */
	WebServerRouteInterceptorConfigurerInfo[] getInterceptorConfigurers();

	/**
	 * <p>
	 * Returns the Web router configurers provided in the module.
	 * </p>
	 *
	 * <p>
	 * These can be defined as beans in the module or in component modules.
	 * </p>
	 *
	 * @return an array of router configurers
	 */
	WebServerRouterConfigurerInfo[] getRouterConfigurers();

	/**
	 * <p>
	 * Returns the error Web route interceptor configurers provided in the module.
	 * </p>
	 *
	 * <p>
	 * These can be defined as beans in the module or in component modules.
	 * </p>
	 *
	 * @return an array of error interceptor configurers
	 */
	ErrorWebServerRouteInterceptorConfigurerInfo[] getErrorInterceptorConfigurers();

	/**
	 * <p>
	 * Returns the error Web routes configurers provided in the module.
	 * </p>
	 *
	 * <p>
	 * These can be defined as beans in the module or in component modules.
	 * </p>
	 *
	 * @return an array of error router configurers
	 */
	ErrorWebServerRouterConfigurerInfo[] getErrorRouterConfigurers();

	/**
	 * <p>
	 * Returns the Web server configurers provided in the module.
	 * </p>
	 *
	 * <p>
	 * These can be defined as beans in the module or in component modules.
	 * </p>
	 *
	 * @return an array of provided server configurer
	 */
	WebServerConfigurerInfo[] getServerConfigurers();

	/**
	 * <p>
	 * Returns the Web controllers defined in the module.
	 * </p>
	 *
	 * @return an array of Web controllers
	 */
	WebServerControllerInfo[] getControllers();

	/**
	 * <p>
	 * Returns the list of exchange context types required by the routes defined by the Web server.
	 * </p>
	 *
	 * @return an array of context types
	 */
	TypeMirror[] getContextTypes();

	/**
	 * <p>
	 * Returns the types registry referencing all unique bodies and parameters types specified in the Web controllers.
	 * </p>
	 *
	 * @return an array of types
	 */
	TypeMirror[] getTypesRegistry();

	/**
	 * <p>
	 * Accepts the specified Web server info visitor.
	 * </p>
	 *
	 * @param <R>     the type of the visitor result
	 * @param <P>     the type of the visitor parameter
	 * @param visitor the visitor to invoke
	 * @param p       the parameter
	 *
	 * @return the visitor result
	 */
	<R, P> R accept(WebServerModuleInfoVisitor<R, P> visitor, P p);
}
