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
package io.inverno.mod.configuration.compiler.spi;

import io.inverno.core.compiler.spi.BeanQualifiedName;
import io.inverno.core.compiler.spi.ModuleQualifiedName;
import io.inverno.core.compiler.spi.QualifiedNameFormatException;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
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

	/**
	 * <p>
	 * Creates a property qualified name with the specified bean qualified name and
	 * property name.
	 * </p>
	 * 
	 * @param beanQName a bean qualified name
	 * @param name      the property name
	 * 
	 * @throws QualifiedNameFormatException if the name is invalid
	 */
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

	/**
	 * <p>
	 * Creates a property qualified name from the specified raw value of the form
	 * {@code BeanQualifiedName():<propertyName>} where
	 * {@code <propertyName>} is a valid Java name.
	 * </p>
	 * 
	 * @param qname a raw qualified name
	 * 
	 * @return a property qualified name
	 * @throws QualifiedNameFormatException if the specified value is not a property
	 *                                      qualified name
	 */
	public static PropertyQualifiedName valueOf(String qname) throws QualifiedNameFormatException {
		int lastSeparatorIndex = qname.lastIndexOf(".");
		if (lastSeparatorIndex == -1) {
			throw new QualifiedNameFormatException("Invalid qname " + qname + ", was expecting: BeanQualifiedName().<propertyName>");
		}
		return new PropertyQualifiedName(BeanQualifiedName.valueOf(qname.substring(0, lastSeparatorIndex)), qname.substring(lastSeparatorIndex + 1));
	}

	/**
	 * <p>
	 * Creates a property qualified name from the specified module qualified name
	 * and the specified raw value of the form
	 * {@code <beanName>:<propertyName>} where
	 * {@code <beanName>} and {@code <propertyName>} are valid
	 * Java names.
	 * </p>
	 * 
	 * @param moduleQName a module qualified name
	 * @param qname       a raw qualified name
	 * 
	 * @return a property qualified name
	 * @throws QualifiedNameFormatException if the specified value is not a property
	 *                                      qualified name
	 */
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
