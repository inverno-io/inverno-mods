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
package io.inverno.mod.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.function.Consumer;

/**
 * <p>
 * Used on an interface to indicate a configuration.
 * </p>
 * 
 * <p>
 * A configuration should be created when there's a need to provide
 * configuration data in a module. Configuration properties are declared as
 * non-void no-argument methods in an interface. Default values can be specified
 * in default methods.
 * </p>
 * 
 * <blockquote><pre>
 * {@literal @Configuration}
 * public interface SomeConfig {
 *     
 *     String property1();
 *     
 *     default int property2() {
 *         return 0;
 *     }
 * }
 * </pre></blockquote>
 * 
 * <p>
 * For a given configuration, a module bean named after the configuration
 * interface [ConfigurationInterface]Bean will be generated to provide a
 * concrete configuration inside the enclosing module. This bean will load
 * configuration data from a {@link ConfigurationSource} and a list of
 * parameters when provided within the module, when no configuration source is
 * specified, the default implementation is loaded. A configurer can also be
 * provided to override all or part of the loaded data.
 * </p>
 * 
 * <p>
 * The generated bean also provides a configurator to programmatically create a
 * concrete configuration in an efficient way. A {@link Consumer} of such
 * configurator is called a configurer:
 * </p>
 * 
 * <blockquote><pre>
 * Config config = SomeConfigBean.ConfigConfigurator
 *     .create(configConfigurator -> configConfigurator.property1("someValue").property2(42));
 * </pre></blockquote>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Configuration {

	/**
	 * <p>
	 * Indicates a name identifying the configuration bean in the module, defaults
	 * to the name of the class.
	 * </p>
	 * 
	 * @return A name
	 */
	String name() default "";
	
	/**
	 * <p>
	 * Indicates whether a bean should be generated in addition to the configuration
	 * loader.
	 * </p>
	 * 
	 * @return true to generate a bean, false otherwise
	 */
	boolean generateBean() default true;
	
	/**
	 * <p>
	 * Indicates whether the generated bean should be overridable.
	 * </p>
	 * 
	 * @return true to generate a bean, false otherwise
	 */
	boolean overridable() default true;
}
