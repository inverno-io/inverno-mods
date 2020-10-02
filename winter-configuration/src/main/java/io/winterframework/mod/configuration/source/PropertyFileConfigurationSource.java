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
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.winterframework.mod.configuration.ConfigurationProperty;
import io.winterframework.mod.configuration.ValueDecoder;
import io.winterframework.mod.configuration.codec.StringValueDecoder;
import io.winterframework.mod.configuration.ConfigurationKey;
import io.winterframework.mod.configuration.internal.AbstractHashConfigurationSource;
import io.winterframework.mod.configuration.internal.GenericConfigurationProperty;
import io.winterframework.mod.configuration.internal.parser.option.ConfigurationOptionParser;
import io.winterframework.mod.configuration.internal.parser.option.ParseException;
import io.winterframework.mod.configuration.internal.parser.option.StringProvider;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
public class PropertyFileConfigurationSource extends AbstractHashConfigurationSource<String, PropertyFileConfigurationSource> {

	private static final Logger LOGGER = LogManager.getLogger(PropertyFileConfigurationSource.class);
	
	private Path propertyFile;
	
	public PropertyFileConfigurationSource(Path propertyFile) {
		this(propertyFile, new StringValueDecoder());
	}
	
	public PropertyFileConfigurationSource(Path propertyFile, ValueDecoder<String> decoder) {
		super(decoder);
		this.propertyFile = propertyFile;
	}

	@Override
	protected Mono<List<ConfigurationProperty<ConfigurationKey, PropertyFileConfigurationSource>>> load() {
		return Mono.defer(() -> {
			try {
				Properties properties = new Properties();
				properties.load(Files.newInputStream(this.propertyFile));
				
				return Mono.just(properties.entrySet().stream().map(entry -> {
						try {
							ConfigurationOptionParser<PropertyFileConfigurationSource> parser = new ConfigurationOptionParser<>(new StringProvider(entry.getKey().toString()));
							return new GenericConfigurationProperty<ConfigurationKey, PropertyFileConfigurationSource, String>( parser.StartKey(), entry.getValue().toString(), this);
						} 
						catch (ParseException e) {
							LOGGER.warn(() -> "Ignoring property " + entry.getKey() + " after parsing error: " + e.getMessage());
						}
						return null;
					})
					.filter(Objects::nonNull)
					.collect(Collectors.toList())
				);
			}
			catch(IOException e) {
				LOGGER.warn(() -> "Invalid property file " + this.propertyFile.getFileName().toString() + " after I/O error: " + e.getMessage());
				return Mono.error(e);
			}
		});
	}
}