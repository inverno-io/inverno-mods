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
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.winterframework.mod.base.converter.SplittablePrimitiveDecoder;
import io.winterframework.mod.configuration.AbstractHashConfigurationSource;
import io.winterframework.mod.configuration.ConfigurationKey;
import io.winterframework.mod.configuration.ConfigurationProperty;
import io.winterframework.mod.configuration.internal.GenericConfigurationProperty;
import io.winterframework.mod.configuration.internal.JavaStringConverter;
import io.winterframework.mod.configuration.internal.parser.option.ConfigurationOptionParser;
import io.winterframework.mod.configuration.internal.parser.option.ParseException;
import io.winterframework.mod.configuration.internal.parser.option.StringProvider;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A configuration source that looks up properties in a regular property file.
 * </p>
 * 
 * <p>
 * This source supports parameterized configuration properties defined in a
 * configuration file as follows:
 * </p>
 * 
 * <blockquote><pre>
 * web.server_port=8080
 * web.server_port[profile="ssl"]=8443
 * db.url[env="dev"]=jdbc:oracle:thin:@dev.db.server:1521:sid
 * db.url[env="prod",zone="eu"]=jdbc:oracle:thin:@prod_eu.db.server:1521:sid
 * db.url[env="prod",zone="us"]=jdbc:oracle:thin:@prod_us.db.server:1521:sid
 * </pre></blockquote>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AbstractHashConfigurationSource
 */
public class PropertyFileConfigurationSource extends AbstractHashConfigurationSource<String, PropertyFileConfigurationSource> {

	private static final Logger LOGGER = LogManager.getLogger(PropertyFileConfigurationSource.class);
	
	private Path propertyFile;
	
	private InputStream propertyInput;
	
	/**
	 * <p>
	 * Creates a property file configuration source with the {@code .properties}
	 * file at the specified path.
	 * </p>
	 * 
	 * @param propertyFile the path to the {@code .properties} file
	 */
	public PropertyFileConfigurationSource(Path propertyFile) {
		this(propertyFile, new JavaStringConverter());
	}
	
	/**
	 * <p>
	 * Creates a property file configuration source with the {@code .properties}
	 * file at the specified path and the specified string value decoder.
	 * </p>
	 * 
	 * @param propertyFile the path to the {@code .properties} file
	 * @param decoder      a string decoder
	 */
	public PropertyFileConfigurationSource(Path propertyFile, SplittablePrimitiveDecoder<String> decoder) {
		super(decoder);
		this.propertyFile = propertyFile;
	}
	
	/**
	 * <p>
	 * Creates a property file configuration source with the specified input
	 * stream.
	 * </p>
	 * 
	 * @param propertyInput the {@code .properties} input
	 */
	public PropertyFileConfigurationSource(InputStream propertyInput) {
		this(propertyInput, new JavaStringConverter());
	}
	
	/**
	 * <p>
	 * Creates a property file configuration source with the specified input stream
	 * and string value decoder.
	 * </p>
	 * 
	 * @param propertyInput the {@code .properties} input
	 * @param decoder       a string decoder
	 */
	public PropertyFileConfigurationSource(InputStream propertyInput, SplittablePrimitiveDecoder<String> decoder) {
		super(decoder);
		this.propertyInput = propertyInput;
	}

	@Override
	protected Mono<List<ConfigurationProperty<ConfigurationKey, PropertyFileConfigurationSource>>> load() {
		return Mono.defer(() -> {
			try(InputStream input = this.propertyFile != null ? Files.newInputStream(this.propertyFile) : this.propertyInput) {
				Properties properties = new Properties();
				properties.load(input);
				
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