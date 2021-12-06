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

import java.util.Map;

import io.inverno.mod.base.converter.SplittablePrimitiveDecoder;
import io.inverno.mod.configuration.AbstractPropertiesConfigurationSource;
import io.inverno.mod.configuration.internal.ObjectDecoder;

/**
 * <p>
 * A configuration source that looks up properties from a map.
 * </p>
 *
 * <p>
 * Note that this source doesn't support parameterized queries, regardless of the parameters specified in a query, only the configuration key name is considered when resolving a value.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see AbstractPropertiesConfigurationSource
 */
public class MapConfigurationSource extends AbstractPropertiesConfigurationSource<Object, MapConfigurationSource> {

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
	 * @param properties a map of properties
	 * @param decoder    a string decoder
	 */
	public MapConfigurationSource(Map<String, Object> properties, SplittablePrimitiveDecoder<Object> decoder) {
		super(decoder, properties::get);
	}
}
