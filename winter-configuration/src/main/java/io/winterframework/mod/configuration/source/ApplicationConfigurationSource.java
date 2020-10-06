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
 * @author jkuhn
 *
 */
public class ApplicationConfigurationSource extends CompositeConfigurationSource {

	public ApplicationConfigurationSource(Module module, String... args) throws IOException {
		super(Stream.of(new CommandLineConfigurationSource(args),
				new SystemPropertiesConfigurationSource(),
				new SystemEnvironmentConfigurationSource(),
				Optional.of(Paths.get("config", "configuration.cprops")).filter(Files::exists).map(ConfigurationPropertyFileConfigurationSource::new).orElse(null),
				Optional.of(module.getResourceAsStream("configuration.cprops")).filter(Objects::nonNull).map(ConfigurationPropertyFileConfigurationSource::new).orElse(null)
		).filter(Objects::nonNull).collect(Collectors.toList()));
	}
}
