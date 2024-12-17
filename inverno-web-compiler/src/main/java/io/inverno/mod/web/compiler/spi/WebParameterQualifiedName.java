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

import io.inverno.core.compiler.spi.QualifiedName;
import io.inverno.core.compiler.spi.QualifiedNameFormatException;

/**
 * <p>
 * A qualified name identifying a parameter in a Web route.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class WebParameterQualifiedName extends QualifiedName {

	private final WebRouteQualifiedName routeQName;
	
	private final String name;
	
	/**
	 * <p>
	 * Creates a Web parameter qualified name with the specified route qualified name and parameter name.
	 * </p>
	 *
	 * @param routeQName the route qualified name
	 * @param name       the parameter name
	 *
	 * @throws QualifiedNameFormatException if the specified parameter name is invalid
	 */
	public WebParameterQualifiedName(WebRouteQualifiedName routeQName, String name) throws QualifiedNameFormatException {
		super(routeQName.getValue() + "." + name);
		
		this.routeQName = routeQName;
		this.name = name;
	}

	@Override
	public String getSimpleValue() {
		return this.getParameterName();
	}
	
	/**
	 * <p>
	 * Returns the route qualified name.
	 * </p>
	 * 
	 * @return the route qualified name
	 */
	public WebRouteQualifiedName getRouteQName() {
		return this.routeQName;
	}
	
	/**
	 * <p>
	 * Returns the parameter name.
	 * </p>
	 * 
	 * @return the parameter name
	 */
	public String getParameterName() {
		return this.name;
	}
}
