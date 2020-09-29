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
package io.winterframework.mod.configuration.internal;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import io.winterframework.mod.configuration.AbstractConfigurationSource;
import io.winterframework.mod.configuration.ConfigurationEntry;
import io.winterframework.mod.configuration.ConfigurationKey;
import io.winterframework.mod.configuration.ConfigurationQuery;
import io.winterframework.mod.configuration.ExecutableConfigurationQuery;
import io.winterframework.mod.configuration.ValueConverter;
import reactor.core.publisher.Flux;

/**
 * @author jkuhn
 *
 */
public abstract class AbstractPropertiesConfigurationSource<A, B extends AbstractPropertiesConfigurationSource<A,B>> extends AbstractConfigurationSource<AbstractPropertiesConfigurationSource.PropertyConfigurationQuery<A, B>, AbstractPropertiesConfigurationSource.PropertyExecutableConfigurationQuery<A, B>, AbstractPropertiesConfigurationSource.PropertyConfigurationQueryResult<A, B>, A> {

	protected Function<String, A> propertyAccessor;
	
	public AbstractPropertiesConfigurationSource(ValueConverter<A> converter, Function<String, A> propertyAccessor) {
		super(converter);
		this.propertyAccessor = propertyAccessor;
	}
	
	@Override
	public PropertyExecutableConfigurationQuery<A, B> get(String... names) throws IllegalArgumentException {
		return new PropertyExecutableConfigurationQuery<>(this).and().get(names);
	}
	
	public static class PropertyConfigurationQuery<A, B extends AbstractPropertiesConfigurationSource<A,B>> implements ConfigurationQuery<PropertyConfigurationQuery<A, B>, PropertyExecutableConfigurationQuery<A, B>, PropertyConfigurationQueryResult<A, B>> {
		
		private List<String> names;
		
		private LinkedHashMap<String, Object> parameters;
		
		private PropertyExecutableConfigurationQuery<A, B> executableQuery;
		
		private PropertyConfigurationQuery(PropertyExecutableConfigurationQuery<A, B> executableQuery) {
			this.executableQuery = executableQuery;
			this.names = new LinkedList<>();
			this.parameters = new LinkedHashMap<>();
		}
		
		@Override
		public PropertyExecutableConfigurationQuery<A, B> get(String... names) throws IllegalArgumentException {
			if(names == null || names.length == 0) {
				throw new IllegalArgumentException("You can't query an empty list of configuration entries");
			}
			this.names.addAll(Arrays.asList(names));
			return this.executableQuery;
		}
	}
	
	public static class PropertyExecutableConfigurationQuery<A, B extends AbstractPropertiesConfigurationSource<A,B>> implements ExecutableConfigurationQuery<PropertyConfigurationQuery<A, B>, PropertyExecutableConfigurationQuery<A, B>, PropertyConfigurationQueryResult<A, B>> {

		private B source;
		
		private LinkedList<PropertyConfigurationQuery<A, B>> queries;
		
		@SuppressWarnings("unchecked")
		private PropertyExecutableConfigurationQuery(AbstractPropertiesConfigurationSource<A, B> source) {
			this.source = (B)source;
			this.queries = new LinkedList<>();
		}
		
		@Override
		public ExecutableConfigurationQuery<PropertyConfigurationQuery<A, B>, PropertyExecutableConfigurationQuery<A, B>, PropertyConfigurationQueryResult<A, B>> withParameters(Parameter... parameters) throws IllegalArgumentException {
			if(parameters != null && parameters.length > 0) {
				PropertyConfigurationQuery<A, B> currentQuery = this.queries.peekLast();
				currentQuery.parameters.clear();
				String duplicateParameters = "";
				for(Parameter parameter : parameters) {
					if(currentQuery.parameters.put(parameter.getName(), parameter.getValue()) != null) {
						duplicateParameters += parameter.getName();
					}
				}
				if(duplicateParameters != null && duplicateParameters.length() > 0) {
					throw new IllegalArgumentException("The following parameters were specified more than once: " + duplicateParameters);
				}
			}
			return this;
		}
		
		@Override
		public PropertyConfigurationQuery<A, B> and() {
			this.queries.add(new PropertyConfigurationQuery<>(this));
			return this.queries.peekLast();
		}

		@Override
		public Flux<PropertyConfigurationQueryResult<A, B>> execute() {
			return Flux.fromStream(this.queries.stream().flatMap(query -> query.names.stream().map(name -> new GenericConfigurationKey(name, query.parameters)))
				.map(key -> new PropertyConfigurationQueryResult<>(key, 
						Optional.ofNullable(this.source.propertyAccessor.apply(key.getName()))
						.map(value -> new GenericConfigurationEntry<ConfigurationKey, B, A>(key, value, this.source))
						.orElse(null)
					)
				)
			);
		}
	}
	
	public static class PropertyConfigurationQueryResult<A, B extends AbstractPropertiesConfigurationSource<A,B>> extends GenericConfigurationQueryResult<ConfigurationKey, ConfigurationEntry<ConfigurationKey, B>> {

		private PropertyConfigurationQueryResult(ConfigurationKey queryKey, ConfigurationEntry<ConfigurationKey, B> queryResult) {
			super(queryKey, queryResult);
		}
	}
}
