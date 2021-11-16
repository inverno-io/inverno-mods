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

/**
 * <p>
 * A qualified name identifying a web routes, interceptors or provided router configurer.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class WebConfigurerQualifiedName extends BeanQualifiedName {

	private final String className;
	
	/**
	 * <p>
	 * Creates a web router configurer qualified name with the specified bean
	 * qualified name and class name.
	 * </p>
	 * 
	 * @param beanQName the bean qualified name of the bean defining the web router
	 *                  configurer
	 * @param className the canonical class name of the class defining the web
	 *                  router configurer
	 */
	public WebConfigurerQualifiedName(BeanQualifiedName beanQName, String className) {
		super(beanQName.getModuleQName(), beanQName.getBeanName());
		this.className = className;
	}
	
	/**
	 * <p>
	 * Returns the name of the router.
	 * </p>
	 * 
	 * @return the router name
	 */
	public String getRouterName() {
		return this.getBeanName();
	}
	
	/**
	 * <p>
	 * Returns the router class name.
	 * </p>
	 * 
	 * @return a canonical class name
	 */
	public String getClassName() {
		return this.className;
	}
}
