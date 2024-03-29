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

/**
 * <p>
 * A qualified name identifying a web router configurer.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class WebServerControllerConfigurerQualifiedName extends BeanQualifiedName {

	private static final String WEB_CONTROLLER_CONFIGURER_NAME = "webServerContollerConfigurer";
	
	private static final String WEB_CONTROLLER_CONFIGURER_CLASSNAME = Character.toUpperCase(WEB_CONTROLLER_CONFIGURER_NAME.charAt(0)) + WEB_CONTROLLER_CONFIGURER_NAME.substring(1);
	
	private final String className;
	
	/**
	 * <p>
	 * Creates the web router configurer qualified name of the specified module qualifed name.
	 * </p>
	 *
	 * @param moduleQName the module qualified name
	 */
	public WebServerControllerConfigurerQualifiedName(ModuleQualifiedName moduleQName) {
		super(moduleQName, WEB_CONTROLLER_CONFIGURER_NAME);
		this.className = this.getModuleQName().getSourcePackageName() + "." + Character.toUpperCase(moduleQName.getModuleName().charAt(0))
			+ moduleQName.getModuleName().substring(1) + "_" + WEB_CONTROLLER_CONFIGURER_CLASSNAME;
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
