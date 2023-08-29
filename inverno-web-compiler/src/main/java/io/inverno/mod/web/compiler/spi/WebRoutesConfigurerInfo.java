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
package io.inverno.mod.web.compiler.spi;

import io.inverno.core.compiler.spi.Info;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 * <p>
 * Describes a Web routes configurer.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 */
public interface WebRoutesConfigurerInfo extends Info {

	@Override
	public WebConfigurerQualifiedName getQualifiedName();

	/**
	 * <p>
	 * Returns the type element of the web routes configurer.
	 * </p>
	 * 
	 * @return a type element
	 */
	TypeElement getElement();
	
	/**
	 * <p>
	 * Returns the type of the web routes configurer.
	 * </p>
	 * 
	 * @return a type
	 */
	DeclaredType getType();
	
	/**
	 * <p>
	 * Returns the web routes defined in the web routes configurer.
	 * </p>
	 * 
	 * @return an array of web route info
	 */
	WebRouteInfo[] getRoutes();
	
	/**
	 * <p>
	 * Returns the exchange context type required by the routes defined by the web routes configurer.
	 * </p>
	 *
	 * @return a context type
	 */
	TypeMirror getContextType();
}
