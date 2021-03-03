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
import io.winterframework.core.compiler.spi.QualifiedName;
import io.winterframework.core.compiler.spi.QualifiedNameFormatException;

/**
 * @author jkuhn
 *
 */
public class WebParameterQualifiedName extends QualifiedName {

	private final WebRouteQualifiedName routeQName;
	
	private final String name;
	
	public WebParameterQualifiedName(WebRouteQualifiedName routeQName, String name) throws QualifiedNameFormatException {
		super(routeQName.getValue() + "." + name);
		
		this.routeQName = routeQName;
		this.name = name;
	}

	@Override
	public String getSimpleValue() {
		return this.getParameterName();
	}
	
	public WebRouteQualifiedName getRouteQName() {
		return this.routeQName;
	}
	
	public String getParameterName() {
		return this.name;
	}
	
	public static WebParameterQualifiedName valueOf(String qname) throws QualifiedNameFormatException {
		int lastSeparatorIndex = qname.lastIndexOf(".");
		if (lastSeparatorIndex == -1) {
			throw new QualifiedNameFormatException("Invalid qname " + qname + ", was expecting: WebParameterQualifiedName().<parameterName>");
		}
		return new WebParameterQualifiedName(WebRouteQualifiedName.valueOf(qname.substring(0, lastSeparatorIndex)), qname.substring(lastSeparatorIndex + 1));
	}

	public static WebParameterQualifiedName valueOf(BeanQualifiedName controllerQName, String qname)
			throws QualifiedNameFormatException {
		String[] qnameParts = qname.split(".");
		if (qnameParts.length < 2) {
			throw new QualifiedNameFormatException(
					"Invalid qname " + qname + ", was expecting: <routeName>.<parameterName>");
		}
		return new WebParameterQualifiedName(new WebRouteQualifiedName(controllerQName, qnameParts[0]), qnameParts[1]);
	}

}
