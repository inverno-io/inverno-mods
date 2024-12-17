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
package io.inverno.mod.web.compiler.spi.server;

import io.inverno.core.compiler.spi.BeanQualifiedName;
import io.inverno.core.compiler.spi.Info;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 * <p>
 * Describes a Web controller.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public interface WebServerControllerInfo extends Info {

	@Override
	BeanQualifiedName getQualifiedName();
	
	/**
	 * <p>
	 * Returns the type element defining the Web controller.
	 * </p>
	 * 
	 * @return a type element
	 */
	TypeElement getElement();
	
	/**
	 * <p>
	 * Returns the type of the Web controller.
	 * </p>
	 * 
	 * @return a type
	 */
	DeclaredType getType();
	
	/**
	 * <p>
	 * Returns the Web controller root path.
	 * </p>
	 * 
	 * @return an absolute normalized path
	 */
	String getRootPath();
	
	/**
	 * <p>
	 * Returns the Web routes defined in the Web controller.
	 * </p>
	 * 
	 * @return an array of Web route info
	 */
	WebServerRouteInfo[] getRoutes();

	/**
	 * <p>
	 * Returns the types registry referencing all unique bodies and parameters types specified in the Web controller.
	 * </p>
	 *
	 * @return an array of types
	 */
	TypeMirror[] getTypesRegistry();
}
