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
import java.nio.file.Path;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.winterframework.mod.configuration.ConfigurationEntry;
import io.winterframework.mod.configuration.ConfigurationKey;
import io.winterframework.mod.configuration.converter.StringValueConverter;
import io.winterframework.mod.configuration.internal.AbstractHashConfigurationSource;
import io.winterframework.mod.configuration.internal.parser.properties.ConfigurationPropertiesParser;
import io.winterframework.mod.configuration.internal.parser.properties.ParseException;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
public class ConfigurationPropertyFileConfigurationSource extends AbstractHashConfigurationSource<String, ConfigurationPropertyFileConfigurationSource> {

	private static final Logger LOGGER = LogManager.getLogger(ConfigurationPropertyFileConfigurationSource.class);
	
	private Path propertyFile;
	
	/**
	 * @param converter
	 */
	public ConfigurationPropertyFileConfigurationSource(Path propertyFile) {
		super(new StringValueConverter());
		this.propertyFile = propertyFile;
	}

	@Override
	protected Mono<List<ConfigurationEntry<ConfigurationKey, ConfigurationPropertyFileConfigurationSource>>> load() {
		return Mono.defer(() -> {
			try {
				ConfigurationPropertiesParser<ConfigurationPropertyFileConfigurationSource> parser = new ConfigurationPropertiesParser<>(Files.newInputStream(this.propertyFile));
				parser.setConfigurationSource(this);
				parser.setValueConverter(this.converter);
				return Mono.just(parser.StartConfigurationProperties());
			} 
			catch (IOException e) {
				LOGGER.warn(() -> "Ignoring configuration property file: " + this.propertyFile.getFileName() + " after I/O error: " + e.getMessage());
			} 
			catch (ParseException e) {
				LOGGER.warn(() -> "Ignoring configuration property file " + this.propertyFile.getFileName() + " after parsing error: " + e.getMessage());
			}
			return Mono.empty();
		});
	}
}
