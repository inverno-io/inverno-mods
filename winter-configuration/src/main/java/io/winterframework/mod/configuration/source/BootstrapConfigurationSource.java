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
package io.winterframework.mod.configuration.source;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * A configuration source that considers multiple local sources to load the
 * bootstrap configuration of an application.
 * </p>
 * 
 * <p>
 * It resolves configuration properties from the following sources in that
 * order:
 * </p>
 * 
 * <ul>
 * <li>command line arguments</li>
 * <li>system properties</li>
 * <li>system environment variables</li>
 * <li>the {@code configuration.cprops} file in {@code ./conf/} or
 * <code>${winter.config.path}/</code> directories if one exists (if the first one exists the second
 * one is ignored)</li>
 * <li>the {@code configuration.cprops} file in <code>${java.home}/conf/</code> directory if it exists</li>
 * <li>the {@code configuration.cprops} file in the application module if it
 * exists</li>
 * </ul>
 * 
 * <p>
 * This source is typically created in a {@code main} method to load the
 * application configuration at startup.
 * </p>
 * 
 * <blockquote>
 * 
 * <pre>
 * public class Application {
 * 
 *     public static void main(String[] args) {
 *         ApplicationConfigurationSource source = new ApplicationConfigurationSource(App.class.getModule(), args);
 *         
 *         // Load configuration
 *         ApplicationConfiguration configuration = ConfigurationLoader
 *             .withConfiguration(ApplicationConfiguration.class)
 *             .withSource(source)
 *             .load()
 *             .block();
 * 
 *         // Start the application with the configuration
 *         ...
 *     }
 * }
 * </pre>
 * 
 * </blockquote>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see CompositeConfigurationSource
 * @see CommandLineConfigurationSource
 * @see SystemPropertiesConfigurationSource
 * @see SystemEnvironmentConfigurationSource
 * @see CPropsFileConfigurationSource
 */
public class BootstrapConfigurationSource extends CompositeConfigurationSource {

	private static final String WINTER_CONFIG_PATH = "winter.config.path";
	
	private static final String JAVA_HOME = "java.home";
	
	/**
	 * <p>
	 * Creates a bootstrap configuration source for the specified application
	 * module and with the specified command line arguments.
	 * </p>
	 * 
	 * @param module the application module
	 * @param args   the command line arguments
	 * @throws IOException if something goes wrong creating the configuration source
	 */
	// TODO 
	// - add Paths.get(System.getProperty("app.home"), "conf", "configuration.cprops");
	// - add Paths.get(System.getProperty("java.home"), "conf", "configuration.cprops");
	public BootstrapConfigurationSource(Module module, String... args) throws IOException {
		super(Stream.of(new CommandLineConfigurationSource(args),
				new SystemPropertiesConfigurationSource(),
				new SystemEnvironmentConfigurationSource(),
				Optional.of(Paths.get("conf", "configuration.cprops")).filter(Files::exists).or(() -> Optional.ofNullable(System.getProperty(WINTER_CONFIG_PATH)).map(config_path -> Paths.get(config_path, "configuration.cprops")).filter(Files::exists)).map(CPropsFileConfigurationSource::new).orElse(null),
				Optional.of(System.getProperty(JAVA_HOME)).map(app_home -> Paths.get(app_home, "conf", "configuration.cprops")).filter(Files::exists).map(CPropsFileConfigurationSource::new).orElse(null),
				Optional.of(module.getResourceAsStream("configuration.cprops")).filter(Objects::nonNull).map(CPropsFileConfigurationSource::new).orElse(null)
		).filter(Objects::nonNull).collect(Collectors.toList()));
	}
}
