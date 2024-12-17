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

import io.inverno.core.compiler.spi.BeanQualifiedName;
import io.inverno.core.compiler.spi.ModuleQualifiedName;
import io.inverno.core.compiler.spi.QualifiedNameFormatException;
import io.inverno.mod.web.compiler.spi.WebRouteQualifiedName;

/**
 * <p>
 * A qualified name identifying a Web client route in a Web client.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class WebClientRouteQualifiedName extends WebRouteQualifiedName {

	/**
	 * <p>
	 * Creates a Web client route qualified name with the specified name.
	 * </p>
	 *
	 * @param name the route name
	 *
	 * @throws QualifiedNameFormatException if the specified name is invalid
	 */
	public WebClientRouteQualifiedName(String name) throws QualifiedNameFormatException {
		super(name);
	}

	/**
	 * <p>
	 * Creates a Web client route qualified name with the specified controller qualified name and route name.
	 * </p>
	 *
	 * @param clientQName the client qualified name
	 * @param name        the route name
	 *
	 * @throws QualifiedNameFormatException if the specified route name is invalid
	 */
	public WebClientRouteQualifiedName(BeanQualifiedName clientQName, String name) throws QualifiedNameFormatException {
		super(clientQName, name);
	}

	/**
	 * <p>
	 * Returns the client qualified name.
	 * </p>
	 *
	 * @return the bean qualified name of the controller
	 */
	public BeanQualifiedName getClientQName() {
		return this.beanQName;
	}

	/**
	 * <p>
	 * Creates a Web client route qualified name from the specified raw value of the form {@code WebClientQualifiedName():<routeName>} where {@code <routeName>} is a valid Java name.
	 * </p>
	 *
	 * @param qname a raw qualified name
	 *
	 * @return a Web client route qualified name
	 *
	 * @throws QualifiedNameFormatException if the specified value is not a  Web route qualified name
	 */
	public static WebClientRouteQualifiedName valueOf(String qname) throws QualifiedNameFormatException {
		int lastSeparatorIndex = qname.lastIndexOf(SEPARATOR);
		if (lastSeparatorIndex == -1) {
			throw new QualifiedNameFormatException(
				"Invalid qname " + qname + ", was expecting: WebClientQualifiedName():<routeName>");
		}
		return new WebClientRouteQualifiedName(BeanQualifiedName.valueOf(qname.substring(0, lastSeparatorIndex)),
			qname.substring(lastSeparatorIndex + 1));
	}

	/**
	 * <p>
	 * Creates a Web client route qualified name from the specified module qualified name and the specified raw value of the form {@code <clientName>:<routeName>} where {@code <clientName>} and
	 * {@code <routeName>} are valid Java names.
	 * </p>
	 *
	 * @param moduleQName a module qualified name
	 * @param qname       a raw qualified name
	 *
	 * @return a Web client route qualified name
	 *
	 * @throws QualifiedNameFormatException if the specified value is not a Web parameter qualified name
	 */
	public static WebClientRouteQualifiedName valueOf(ModuleQualifiedName moduleQName, String qname)
		throws QualifiedNameFormatException {
		String[] qnameParts = qname.split(SEPARATOR);
		if (qnameParts.length != 2) {
			throw new QualifiedNameFormatException(
				"Invalid qname " + qname + ", was expecting: <clientName>:<routeName>");
		}
		return new WebClientRouteQualifiedName(new BeanQualifiedName(moduleQName, qnameParts[0]), qnameParts[1]);
	}
}
