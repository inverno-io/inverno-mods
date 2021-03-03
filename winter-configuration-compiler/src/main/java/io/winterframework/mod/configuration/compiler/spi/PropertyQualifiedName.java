/*
 * Copyright 2020 Jeremy KUHN
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
package io.winterframework.mod.configuration.compiler.spi;

import io.winterframework.core.compiler.spi.BeanQualifiedName;
import io.winterframework.core.compiler.spi.ModuleQualifiedName;
import io.winterframework.core.compiler.spi.QualifiedNameFormatException;

/**
 * @author jkuhn
 *
 */
public class PropertyQualifiedName extends BeanQualifiedName {

	private BeanQualifiedName beanQName;
	
	/**
	 * <p>
	 * The name of the property.
	 * </p>
	 */
	private String name;

	public PropertyQualifiedName(BeanQualifiedName beanQName, String name) throws QualifiedNameFormatException {
		super(beanQName.getModuleQName(), beanQName.getBeanName() + "." + name);

		this.beanQName = beanQName;
		this.name = name;

		this.validateQualifiedNamePart(this.name);
	}

	/**
	 * <p>
	 * Returns the name of the bean defining the property.
	 * </p>
	 * 
	 * @return a bean qualified name
	 */
	public BeanQualifiedName getBeanQName() {
		return this.beanQName;
	}

	/**
	 * <p>
	 * Returns the name of the property.
	 * </p>
	 * 
	 * @return the socket name
	 */
	public String getPropertyName() {
		return this.name;
	}

	public static PropertyQualifiedName valueOf(String qname) throws QualifiedNameFormatException {
		int lastSeparatorIndex = qname.lastIndexOf(".");
		if (lastSeparatorIndex == -1) {
			throw new QualifiedNameFormatException("Invalid qname " + qname + ", was expecting: BeanQualifiedName().<propertyName>");
		}
		return new PropertyQualifiedName(BeanQualifiedName.valueOf(qname.substring(0, lastSeparatorIndex)), qname.substring(lastSeparatorIndex + 1));
	}

	public static PropertyQualifiedName valueOf(ModuleQualifiedName moduleQName, String qname)
			throws QualifiedNameFormatException {
		String[] qnameParts = qname.split(".");
		if (qnameParts.length < 2) {
			throw new QualifiedNameFormatException(
					"Invalid qname " + qname + ", was expecting: <beanName>.<propertyName>");
		}
		return new PropertyQualifiedName(new BeanQualifiedName(moduleQName, qnameParts[0]), qnameParts[1]);
	}

}
