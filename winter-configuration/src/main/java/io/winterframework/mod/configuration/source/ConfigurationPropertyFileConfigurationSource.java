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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.winterframework.mod.base.converter.SplittablePrimitiveDecoder;
import io.winterframework.mod.configuration.AbstractHashConfigurationSource;
import io.winterframework.mod.configuration.ConfigurationKey;
import io.winterframework.mod.configuration.ConfigurationProperty;
import io.winterframework.mod.configuration.internal.JavaStringConverter;
import io.winterframework.mod.configuration.internal.parser.properties.ConfigurationPropertiesParser;
import io.winterframework.mod.configuration.internal.parser.properties.ParseException;
import io.winterframework.mod.configuration.internal.parser.properties.StreamProvider;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
public class ConfigurationPropertyFileConfigurationSource extends AbstractHashConfigurationSource<String, ConfigurationPropertyFileConfigurationSource> {

	private static final Logger LOGGER = LogManager.getLogger(ConfigurationPropertyFileConfigurationSource.class);
	
	private Path propertyFile;
	
	private InputStream propertyInput;
	
	public ConfigurationPropertyFileConfigurationSource(Path propertyFile) {
		this(propertyFile, new JavaStringConverter());
	}
	
	public ConfigurationPropertyFileConfigurationSource(Path propertyFile, SplittablePrimitiveDecoder<String> decoder) {
		super(decoder);
		this.propertyFile = propertyFile;
	}

	public ConfigurationPropertyFileConfigurationSource(InputStream propertyInput) {
		this(propertyInput, new JavaStringConverter());
	}
	
	public ConfigurationPropertyFileConfigurationSource(InputStream propertyInput, SplittablePrimitiveDecoder<String> decoder) {
		super(decoder);
		this.propertyInput = propertyInput;
	}
	
	@Override
	protected Mono<List<ConfigurationProperty<ConfigurationKey, ConfigurationPropertyFileConfigurationSource>>> load() {
		return Mono.defer(() -> {
			try(InputStream input = this.propertyFile != null ? Files.newInputStream(this.propertyFile) : this.propertyInput) {
				ConfigurationPropertiesParser<ConfigurationPropertyFileConfigurationSource> parser = new ConfigurationPropertiesParser<>(new StreamProvider(input));
				parser.setConfigurationSource(this);
				return Mono.just(parser.StartConfigurationProperties());
			} 
			catch (IOException e) {
				LOGGER.warn(() -> "Invalid configuration property file " + this.propertyFile.getFileName() + " after I/O error: " + e.getMessage());
				return Mono.error(e);
			} 
			catch (ParseException e) {
				LOGGER.warn(() -> "Invalid configuration property file " + this.propertyFile.getFileName() + " after parsing error: " + e.getMessage());
				return Mono.error(e);
			}
		});
	}
}
