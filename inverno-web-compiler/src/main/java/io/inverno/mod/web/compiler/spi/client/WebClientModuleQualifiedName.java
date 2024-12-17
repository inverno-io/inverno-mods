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

/**
 * <p>
 * A qualified name identifying a Web client.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class WebClientModuleQualifiedName extends BeanQualifiedName {

	private static final String WEB_CLIENT_NAME = "webClient";

	private static final String WEB_CLIENT_CLASSNAME = Character.toUpperCase(WEB_CLIENT_NAME.charAt(0)) + WEB_CLIENT_NAME.substring(1);

	private final String className;

	/**
	 * <p>
	 * Creates the Web client qualified name of the specified module qualified name.
	 * </p>
	 *
	 * @param moduleQName the module qualified name
	 */
	public WebClientModuleQualifiedName(ModuleQualifiedName moduleQName) {
		super(moduleQName, WEB_CLIENT_NAME);
		this.className = this.getModuleQName().getSourcePackageName() + "." + Character.toUpperCase(moduleQName.getModuleName().charAt(0)) + moduleQName.getModuleName().substring(1) + "_" + WEB_CLIENT_CLASSNAME;
	}

	/**
	 * <p>
	 * Returns the client class name.
	 * </p>
	 *
	 * @return a canonical class name
	 */
	public String getClassName() {
		return this.className;
	}
}