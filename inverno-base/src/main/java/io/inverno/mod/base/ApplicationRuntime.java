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
package io.inverno.mod.base;

import java.nio.file.spi.FileSystemProvider;

/**
 * <p>
 * The runtime where the application is running.
 * </p>
 *
 * <p>
 * An application can be run in the JVM with or without the module system, depending on whether it was started with a class path or a module path. It can also be a native image generated with GraalVM.
 * This can be important at runtime to be able to handle different runtime behaviours, for instance resources are resolved differently whether the application is running with a class path, a module
 * path or a native image: we can't resolve a module resource with {@link Module#getResourceAsStream(java.lang.String) } if the application was started with a class path and we can't list resources 
 * with {@link ClassLoader#resources(java.lang.String) } when running a native image.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.7
 */
public enum ApplicationRuntime {

	/**
	 * The JVM has been started with a class path.
	 */
	JVM_CLASSPATH,
	/**
	 * The JVM has been started with a module path.
	 */
	JVM_MODULE,
	/**
	 * A native image has been started.
	 */
	IMAGE_NATIVE;
	
	private static final ApplicationRuntime SINGLETON;
	
	static {
		if(FileSystemProvider.installedProviders().stream().anyMatch(fsp -> fsp.getScheme().equals("resource"))) {
			// We found the resource:/ file system provider which is available in a native image
			SINGLETON = IMAGE_NATIVE;
		}
		else if(ApplicationRuntime.class.getModule().isNamed()) {
			// RuntimeEnvironment is defined in a named module, if its runtime module is unnamed we are not running the module system.
			SINGLETON = JVM_MODULE;
		}
		else {
			// Use classpath by default
			SINGLETON = JVM_CLASSPATH;
		}
	}
	
	/**
	 * <p>
	 * Returns the current runtime.
	 * </p>
	 * 
	 * @return the application runtime
	 */
	public static ApplicationRuntime getApplicationRuntime() {
		return SINGLETON;
	}
}
