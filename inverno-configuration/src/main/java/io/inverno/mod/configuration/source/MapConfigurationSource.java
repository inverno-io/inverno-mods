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
package io.inverno.mod.configuration.source;

import io.inverno.mod.base.converter.SplittablePrimitiveDecoder;
import io.inverno.mod.configuration.AbstractHashConfigurationSource;
import io.inverno.mod.configuration.AbstractPropertiesConfigurationSource;
import io.inverno.mod.configuration.ConfigurationKey;
import io.inverno.mod.configuration.ConfigurationProperty;
import io.inverno.mod.configuration.internal.GenericConfigurationProperty;
import io.inverno.mod.base.converter.ObjectDecoder;
import io.inverno.mod.configuration.DefaultingStrategy;
import io.inverno.mod.configuration.internal.parser.option.ConfigurationOptionParser;
import io.inverno.mod.configuration.internal.parser.option.ParseException;
import io.inverno.mod.configuration.internal.parser.option.StringProvider;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A configuration source that looks up properties from a map.
 * </p>
 *
 * <p>
 * This source supports parameterized configuration properties defined as follows:
 * </p>
 * 
 * <pre>{@code
 * Map<String, Object> map = Map.of(
 *     "web.server_port", 8080,
 *     "db.url[env=\"dev\"]", "jdbc:oracle:thin:@dev.db.server:1521:sid",
 *     "db.url[env=\"prod\",zone=\"eu\"]", "jdbc:oracle:thin:@prod_eu.db.server:1521:sid",
 *     "db.url[env=\"prod\",zone=\"eu\"]", "jdbc:oracle:thin:@prod_eu.db.server:1521:sid",
 *     "db.url[env=\"prod\",zone=\"us\"]", "jdbc:oracle:thin:@prod_us.db.server:1521:sid"
 * );
 * }</pre>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see AbstractPropertiesConfigurationSource
 */
public class MapConfigurationSource extends AbstractHashConfigurationSource<Object, MapConfigurationSource> {

	private static final Logger LOGGER = LogManager.getLogger(MapConfigurationSource.class);
	
	/**
	 * The map holding configuration data
	 */
	protected final Map<String, Object> map;
	
	/**
	 * <p>
	 * Creates a map configuration source with the specified map.
	 * </p>
	 * 
	 * @param properties a map of properties
	 */
	public MapConfigurationSource(Map<String, Object> properties) {
		this(properties, new ObjectDecoder());
	}
	
	/**
	 * <p>
	 * Creates a map configuration source with the specified map and string value decoder.
	 * </p>
	 *
	 * @param map a map defining the properties
	 * @param decoder    a string decoder
	 */
	public MapConfigurationSource(Map<String, Object> map, SplittablePrimitiveDecoder<Object> decoder) {
		super(decoder);
		this.map = map;
	}
	
	/**
	 * <p>
	 * Creates a map configuration source from the specified initial source and using the specified defaulting strategy.
	 * </p>
	 *
	 * @param initial            the initial configuration source.
	 * @param defaultingStrategy a defaulting strategy
	 */
	private MapConfigurationSource(MapConfigurationSource initial, DefaultingStrategy defaultingStrategy) {
		super(initial, defaultingStrategy);
		this.map = initial.map;
	}

	@Override
	public MapConfigurationSource withDefaultingStrategy(DefaultingStrategy defaultingStrategy) {
		return new MapConfigurationSource(this.initial != null ? this.initial : this, defaultingStrategy);
	}

	@Override
	protected Mono<List<ConfigurationProperty>> load() {
		return Mono.fromSupplier(() -> {
			List<ConfigurationProperty> properties = new LinkedList<>();
			for(Map.Entry<String, Object> entry : this.map.entrySet()) {
				try {
					ConfigurationOptionParser<?> parser = new ConfigurationOptionParser<>(new StringProvider(entry.getKey()));
					ConfigurationKey configurationKey = parser.StartKey();
					ConfigurationProperty configurationProperty = new GenericConfigurationProperty<>(configurationKey, entry.getValue(), this);
					properties.add(configurationProperty);
				} 
				catch (ParseException e) {
					LOGGER.warn(() -> "Ignoring property " + entry.getKey() + " after parsing error: " + e.getMessage());
				}
			}
			return properties;
		});
	}
}
