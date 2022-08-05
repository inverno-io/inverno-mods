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

import io.inverno.core.compiler.spi.BeanQualifiedName;
import io.inverno.core.compiler.spi.ModuleQualifiedName;
import io.inverno.core.compiler.spi.QualifiedName;
import io.inverno.core.compiler.spi.QualifiedNameFormatException;

/**
 * <p>
 * A qualified name identifying a web route in a web controller or a web router
 * configurer.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class WebRouteQualifiedName extends QualifiedName {

	private final BeanQualifiedName controllerQName;

	private final String name;

	/**
	 * <p>
	 * Creates a web route qualified name with the specified name.
	 * </p>
	 * 
	 * @param name the route name
	 * 
	 * @throws QualifiedNameFormatException if the specified name is invalid
	 */
	public WebRouteQualifiedName(String name) throws QualifiedNameFormatException {
		super(SEPARATOR + name);

		this.controllerQName = null;
		this.name = name;

		this.validateQualifiedNamePart(this.name);
	}
	
	/**
	 * <p>
	 * Creates a web route qualified name with the specified controller qualified
	 * name and route name.
	 * </p>
	 * 
	 * @param controllerQName the controller qualified name
	 * @param name            the route name
	 * 
	 * @throws QualifiedNameFormatException if the specified route name is invalid
	 */
	public WebRouteQualifiedName(BeanQualifiedName controllerQName, String name) throws QualifiedNameFormatException {
		super(controllerQName.getValue() + SEPARATOR + name);

		this.controllerQName = controllerQName;
		this.name = name;

		this.validateQualifiedNamePart(this.name);
	}

	@Override
	public String getSimpleValue() {
		return this.getName();
	}

	/**
	 * <p>
	 * Returns the controller qualified name.
	 * </p>
	 * 
	 * @return the bean qualified name of the controller
	 */
	public BeanQualifiedName getControllerQName() {
		return this.controllerQName;
	}

	/**
	 * <p>Returns the route name.</p>
	 * 
	 * @return the route name
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * <p>
	 * Creates a web route qualified name from the specified raw value of the form
	 * {@code WebControllerQualifiedName():<routeName>} where
	 * {@code <routeName>} is a valid Java name.
	 * </p>
	 * 
	 * @param qname a raw qualified name
	 * 
	 * @return a web route qualified name
	 * @throws QualifiedNameFormatException if the specified value is not a web route
	 *                                      qualified name
	 */
	public static WebRouteQualifiedName valueOf(String qname) throws QualifiedNameFormatException {
		int lastSeparatorIndex = qname.lastIndexOf(SEPARATOR);
		if (lastSeparatorIndex == -1) {
			throw new QualifiedNameFormatException(
					"Invalid qname " + qname + ", was expecting: WebControllerQualifiedName():<routeName>");
		}
		return new WebRouteQualifiedName(BeanQualifiedName.valueOf(qname.substring(0, lastSeparatorIndex)),
				qname.substring(lastSeparatorIndex + 1));
	}
	
	/**
	 * <p>
	 * Creates a bean socket qualified name from the specified module qualified name
	 * and the specified raw value of the form
	 * {@code <controllerName>:<routeName>} where
	 * {@code <controllerName>} and {@code <routeName>} are
	 * valid Java names.
	 * </p>
	 * 
	 * @param moduleQName a module qualified name
	 * @param qname       a raw qualified name
	 * 
	 * @return a web route qualified name
	 * @throws QualifiedNameFormatException if the specified value is not a web
	 *                                      parameter qualified name
	 */
	public static WebRouteQualifiedName valueOf(ModuleQualifiedName moduleQName, String qname)
			throws QualifiedNameFormatException {
		String[] qnameParts = qname.split(SEPARATOR);
		if (qnameParts.length != 2) {
			throw new QualifiedNameFormatException(
					"Invalid qname " + qname + ", was expecting: <controllerName>:<routeName>");
		}
		return new WebRouteQualifiedName(new BeanQualifiedName(moduleQName, qnameParts[0]), qnameParts[1]);
	}

}
