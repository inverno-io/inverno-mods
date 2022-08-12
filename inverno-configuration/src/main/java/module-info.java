import io.inverno.mod.configuration.ConfigurationSource;
import io.inverno.mod.configuration.source.BootstrapConfigurationSource;
import io.inverno.mod.configuration.source.CPropsFileConfigurationSource;
import io.inverno.mod.configuration.source.CommandLineConfigurationSource;
import io.inverno.mod.configuration.source.CompositeConfigurationSource;
import io.inverno.mod.configuration.source.MapConfigurationSource;
import io.inverno.mod.configuration.source.PropertyFileConfigurationSource;
import io.inverno.mod.configuration.source.RedisConfigurationSource;
import io.inverno.mod.configuration.source.VersionedRedisConfigurationSource;
import io.inverno.mod.configuration.source.SystemEnvironmentConfigurationSource;
import io.inverno.mod.configuration.source.SystemPropertiesConfigurationSource;

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

/**
 * <p>
 * The Inverno framework configuration module provides advanced application
 * configuration capabilities.
 * </p>
 * 
 * <p>
 * The API defines the {@link ConfigurationSource} to store or access
 * configuration properties for a particular context specified as a list of
 * parameters (key/value pairs) which opens up many possibilities to create
 * highly customizable application.
 * </p>
 * 
 * <p>
 * The module provides several configuration sources:
 * </p>
 * 
 * <ul>
 * <li>{@link BootstrapConfigurationSource} used to load the bootstrap
 * configuration of an application from various local sources: command line,
 * system properties, system environment variable and configuration files</li>
 * <li>{@link CommandLineConfigurationSource} used to load properties specified
 * in the command line</li>
 * <li>{@link CPropsFileConfigurationSource} used to load properties specified
 * in a {@code .cprops} file</li>
 * <li>{@link MapConfigurationSource} used to load properties specified in map
 * in memory.</li>
 * <li>{@link PropertyFileConfigurationSource} used to load properties specified
 * in a regular {@code .properties} file</li>
 * <li>{@link RedisConfigurationSource} used to store and load properties from a
 * Redis data store</li>
 * <li>{@link SystemEnvironmentConfigurationSource} used to load properties
 * specified as system environment variables</li>
 * <li>{@link SystemPropertiesConfigurationSource} used to load properties
 * specified as system properties</li>
 * <li>{@link CompositeConfigurationSource} used to load properties from a
 * combination of multiple configuration sources relying on particular strategy
 * to determine the best matching result for a given property</li>
 * </ul>
 * 
 * <p>
 * It also introduces the {@code .cprops} configuration file format that defines
 * a syntax that simplifies the definition of parameterized configuration
 * properties.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
module io.inverno.mod.configuration {
	requires transitive io.inverno.mod.base;
	requires static io.inverno.mod.redis;
	
	requires jdk.unsupported; // required by netty for low level API for accessing direct buffers
	requires transitive reactor.core;
	requires transitive org.reactivestreams;
	requires org.apache.logging.log4j;
	requires org.apache.commons.lang3;
	requires org.apache.commons.text;
	
	exports io.inverno.mod.configuration;
	exports io.inverno.mod.configuration.source;
}
