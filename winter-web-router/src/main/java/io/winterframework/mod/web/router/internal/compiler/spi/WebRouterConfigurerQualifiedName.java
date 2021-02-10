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
package io.winterframework.mod.web.router.internal.compiler.spi;

import io.winterframework.core.compiler.spi.BeanQualifiedName;
import io.winterframework.core.compiler.spi.ModuleQualifiedName;
import io.winterframework.core.compiler.spi.QualifiedNameFormatException;

/**
 * @author jkuhn
 *
 */
public class WebRouterConfigurerQualifiedName extends BeanQualifiedName {

	private static final String WEB_ROUTER_CONFIGURER_NAME = "webRouterConfigurer";
	
	private static final String WEB_ROUTER_CONFIGURER_CLASSNAME = Character.toUpperCase(WEB_ROUTER_CONFIGURER_NAME.charAt(0)) + WEB_ROUTER_CONFIGURER_NAME.substring(1);
	
	private final String className;
	
	public WebRouterConfigurerQualifiedName(ModuleQualifiedName moduleQName) throws QualifiedNameFormatException {
		super(moduleQName, WEB_ROUTER_CONFIGURER_NAME);
		this.className = this.getModuleQName().getSourcePackageName() + "." + WEB_ROUTER_CONFIGURER_CLASSNAME;
	}
	
	public WebRouterConfigurerQualifiedName(BeanQualifiedName beanQName, String className) throws QualifiedNameFormatException {
		super(beanQName.getModuleQName(), beanQName.getBeanName());
		this.className = className;
	}
	
	public String getControllerName() {
		return this.getBeanName();
	}
	
	public String getClassName() {
		return this.className;
	}
}
