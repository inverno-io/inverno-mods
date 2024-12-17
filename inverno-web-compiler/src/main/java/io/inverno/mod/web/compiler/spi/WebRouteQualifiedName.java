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
package io.inverno.mod.web.compiler.spi;

import io.inverno.core.compiler.spi.BeanQualifiedName;
import io.inverno.core.compiler.spi.QualifiedName;
import io.inverno.core.compiler.spi.QualifiedNameFormatException;

/**
 * <p>
 * A qualified name identifying a Web route.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public abstract class WebRouteQualifiedName extends QualifiedName {

	protected final BeanQualifiedName beanQName;

	protected final String name;

	/**
	 * <p>
	 * Creates a Web route qualified name.
	 * </p>
	 *
	 * @param name the route name
	 *
	 * @throws QualifiedNameFormatException if the specified name is invalid
	 */
	public WebRouteQualifiedName(String name) throws QualifiedNameFormatException {
		super(SEPARATOR + name);

		this.beanQName = null;
		this.name = name;

		this.validateQualifiedNamePart(this.name);
	}

	/**
	 * <p>
	 * Creates a Web server route qualified name with the specified bean qualified name and route name.
	 * </p>
	 *
	 * @param beanQName the qualified name of the bean defining the route
	 * @param name      the route name
	 *
	 * @throws QualifiedNameFormatException if the specified name is invalid
	 */
	public WebRouteQualifiedName(BeanQualifiedName beanQName, String name) throws QualifiedNameFormatException {
		super(beanQName.getValue() + SEPARATOR + name);

		this.beanQName = beanQName;
		this.name = name;

		this.validateQualifiedNamePart(this.name);
	}

	@Override
	public String getSimpleValue() {
		return this.getName();
	}

	/**
	 * <p>
	 * Returns the route name.
	 * </p>
	 *
	 * @return the route name
	 */
	public String getName() {
		return this.name;
	}
}
