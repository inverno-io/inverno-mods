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
package io.winterframework.mod.web.compiler.spi;

import io.winterframework.core.compiler.spi.BeanQualifiedName;
import io.winterframework.core.compiler.spi.ModuleQualifiedName;
import io.winterframework.core.compiler.spi.QualifiedName;
import io.winterframework.core.compiler.spi.QualifiedNameFormatException;

/**
 * @author jkuhn
 *
 */
public class WebRouteQualifiedName extends QualifiedName {

	private final BeanQualifiedName controllerQName;

	private final String name;

	public WebRouteQualifiedName(String name) throws QualifiedNameFormatException {
		super(SEPARATOR + name);

		this.controllerQName = null;
		this.name = name;

		this.validateQualifiedNamePart(this.name);
	}
	
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

	public BeanQualifiedName getControllerQName() {
		return this.controllerQName;
	}

	public String getName() {
		return this.name;
	}
	
	public static WebRouteQualifiedName valueOf(String qname) throws QualifiedNameFormatException {
		int lastSeparatorIndex = qname.lastIndexOf(SEPARATOR);
		if (lastSeparatorIndex == -1) {
			throw new QualifiedNameFormatException(
					"Invalid qname " + qname + ", was expecting: WebControllerQualifiedName():<routeName>");
		}
		return new WebRouteQualifiedName(BeanQualifiedName.valueOf(qname.substring(0, lastSeparatorIndex)),
				qname.substring(lastSeparatorIndex + 1));
	}

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
