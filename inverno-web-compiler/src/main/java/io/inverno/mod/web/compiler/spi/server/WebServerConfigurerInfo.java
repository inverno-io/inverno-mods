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
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 * <p>
 * Describes a Web server configurer.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public interface WebServerConfigurerInfo extends Info {

	/**
	 * <p>
	 * Returns the element of the Web server configurer.
	 * </p>
	 *
	 * <p>
	 * The returned element can be a {@link TypeElement} when the configurer is a bean defined in the module or an {@link javax.lang.model.element.ExecutableElement} if the configurer is built from a
	 * {@link io.inverno.mod.web.server.WebServer} socket exposed by a composed module.
	 * </p>
	 *
	 * @return a type element
	 */
	Element getElement();

	/**
	 * <p>
	 * Returns the type of the Web server configurer.
	 * </p>
	 *
	 * @return a type
	 */
	DeclaredType getType();

	/**
	 * <p>
	 * Returns the Web routes defined in the Web server configurer.
	 * </p>
	 *
	 * @return an array of Web route info
	 */
	WebServerRouteInfo[] getRoutes();

	/**
	 * <p>
	 * Returns the exchange context type required by the routes defined by the Web server configurer.
	 * </p>
	 *
	 * @return a context type
	 */
	TypeMirror getContextType();
}
