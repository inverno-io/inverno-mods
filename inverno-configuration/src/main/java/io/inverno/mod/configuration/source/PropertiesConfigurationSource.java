/*
 * Copyright 2023 Jeremy KUHN
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
package io.inverno.mod.configuration.source;

import io.inverno.mod.base.converter.SplittablePrimitiveDecoder;
import io.inverno.mod.configuration.AbstractHashConfigurationSource;
import io.inverno.mod.configuration.ConfigurationKey;
import io.inverno.mod.configuration.ConfigurationProperty;
import io.inverno.mod.configuration.DefaultingStrategy;
import io.inverno.mod.configuration.internal.GenericConfigurationProperty;
import io.inverno.mod.configuration.internal.JavaStringConverter;
import io.inverno.mod.configuration.internal.parser.option.ConfigurationOptionParser;
import io.inverno.mod.configuration.internal.parser.option.ParseException;
import io.inverno.mod.configuration.internal.parser.option.StringProvider;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A configuration source backed by a {@link Properties} object.
 * </p>
 *
 * <p>
 * This source supports parameterized configuration properties defined as follows:
 * </p>
 * 
 * <pre>{@code
 * Properties properties = new Properties();
 * 
 * properties.setProperty("web.server_port", "8080");
 * properties.setProperty("web.server_port[profile=\"ssl\"]", "8443");
 * properties.setProperty("db.url[env=\"dev\"]", "jdbc:oracle:thin:@dev.db.server:1521:sid");
 * properties.setProperty("db.url[env=\"prod\",zone=\"eu\"]", "jdbc:oracle:thin:@prod_eu.db.server:1521:sid");
 * properties.setProperty("db.url[env=\"prod\",zone=\"us\"]", "jdbc:oracle:thin:@prod_us.db.server:1521:sid");
 * }</pre>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @see AbstractHashConfigurationSource
 */
public class PropertiesConfigurationSource extends AbstractHashConfigurationSource<String, PropertiesConfigurationSource> {

	private static final Logger LOGGER = LogManager.getLogger(PropertiesConfigurationSource.class);
	
	private final Properties properties;
	
	private Mono<List<ConfigurationProperty>> propertiesPublisher;

	/**
	 * <p>
	 * Creates a properties configuration source with the specified {@link Properties}.
	 * </p>
	 *
	 * @param properties the properties
	 */
	public PropertiesConfigurationSource(Properties properties) {
		super(new JavaStringConverter());
		this.properties = properties;
	}

	/**
	 * <p>
	 * Creates a properties configuration source with the specified {@link Properties} and the specified string value decoder.
	 * </p>
	 *
	 * @param properties the properties
	 * @param decoder    a string decoder
	 */
	public PropertiesConfigurationSource(Properties properties, SplittablePrimitiveDecoder<String> decoder) {
		super(decoder);
		this.properties = properties;
	}

	/**
	 * <p>
	 * Creates a properties configuration source from the specified initial source and using the specified defaulting strategy.
	 * </p>
	 *
	 * @param initial            the initial configuration source.
	 * @param defaultingStrategy a defaulting strategy
	 */
	private PropertiesConfigurationSource(PropertiesConfigurationSource initial, DefaultingStrategy defaultingStrategy) {
		super(initial, defaultingStrategy);
		this.properties = initial.properties;
	}
	
	@Override
	public PropertiesConfigurationSource withDefaultingStrategy(DefaultingStrategy defaultingStrategy) {
		return new PropertiesConfigurationSource(this.initial != null ? this.initial : this, defaultingStrategy);
	}
	
	@Override
	protected Mono<List<ConfigurationProperty>> load() {
		if(this.propertiesPublisher == null) {
			this.propertiesPublisher = Mono.fromSupplier(() -> this.properties.entrySet().stream()
				.map(entry -> {
					try {
						ConfigurationOptionParser<PropertiesConfigurationSource> parser = new ConfigurationOptionParser<>(new StringProvider(entry.getKey().toString()));
						return new GenericConfigurationProperty<ConfigurationKey, PropertiesConfigurationSource, String>( parser.StartKey(), entry.getValue().toString(), this);
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
		return this.propertiesPublisher;
	}
}
