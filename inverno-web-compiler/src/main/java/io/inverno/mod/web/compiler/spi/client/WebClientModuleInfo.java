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
package io.inverno.mod.web.compiler.spi.client;

import io.inverno.core.compiler.spi.Info;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.type.TypeMirror;

/**
 * <p>
 * Describes the module's Web client to generate.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public interface WebClientModuleInfo extends Info {

	@Override
	WebClientModuleQualifiedName getQualifiedName();

	/**
	 * <p>
	 * Returns the module element for which a Web client is generated.
	 * </p>
	 *
	 * @return a module element
	 */
	ModuleElement getElement();

	/**
	 * <p>
	 * Returns the Web client sockets defined in the module.
	 * </p>
	 *
	 * <p>
	 * These can be defined as module bean sockets in module's beans or socket beans in component modules.
	 * </p>
	 *
	 * @return an array of Web client sockets
	 */
	WebClientSocketInfo[] getWebClientSockets();

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
	WebClientRouteInterceptorConfigurerInfo[] getInterceptorConfigurers();

	/**
	 * <p>
	 * Returns the Web client stubs defined in the module.
	 * </p>
	 *
	 * @return an array of Web client stubs
	 */
	WebClientStubInfo[] getClientStubs();

	/**
	 * <p>
	 * Returns the list of exchange context types required by the routes defined by the Web client.
	 * </p>
	 *
	 * @return an array of context types
	 */
	TypeMirror[] getContextTypes();

	/**
	 * <p>
	 * Accepts the specified Web client info visitor.
	 * </p>
	 *
	 * @param <R>     the type of the visitor result
	 * @param <P>     the type of the visitor parameter
	 * @param visitor the visitor to invoke
	 * @param p       the parameter
	 *
	 * @return the visitor result
	 */
	<R, P> R accept(WebClientModuleInfoVisitor<R, P> visitor, P p);
}
