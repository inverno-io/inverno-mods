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
package io.inverno.mod.configuration.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import io.inverno.mod.configuration.ConfigurationLoader;
import io.inverno.mod.configuration.ConfigurationLoaderException;
import io.inverno.mod.configuration.ConfigurationQuery;
import io.inverno.mod.configuration.ConfigurationQueryResult;
import io.inverno.mod.configuration.ExecutableConfigurationQuery;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A {@link ConfigurationLoader} implementation that determines the configuration properties to load by analyzing a configurator class through reflection and eventually returns a configuration
 * instance created by a creator function.
 * </p>
 *
 * <p>
 * Configuration properties must be declared as void single-argument methods in the configurator type.
 * </p>
 *
 * <p>
 * Unlike {@link ConfigurationTypeConfigurationLoader}, it is possible to load a configuration for any kind of object and not only interface since the creation of the configuration instance is
 * delegated to a supplied configuration creator.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see ConfigurationLoader
 *
 * @param <A> the configuration type
 * @param <B> the configurator type
 */
public class ConfiguratorTypeConfigurationLoader<A, B> extends AbstractReflectiveConfigurationLoader<A, ConfiguratorTypeConfigurationLoader<A, B>> {

	private final Class<B> configuratorType;
	
	private final Function<Consumer<B>, A> configurationCreator;
	
	/**
	 * <p>
	 * Creates a configuration loader with the specified configurator type and configuration creator.
	 * </p>
	 *
	 * @param configuratorType     the configurator type
	 * @param configurationCreator the configuration creator function
	 */
	public ConfiguratorTypeConfigurationLoader(Class<B> configuratorType, Function<Consumer<B>, A> configurationCreator) {
		this.configuratorType = configuratorType;
		this.configurationCreator = configurationCreator;
	}

	@Override
	public Mono<A> load() {
		List<ConfiguratorQuery> configuratorQueries = new LinkedList<>();
		ExecutableConfigurationQuery<?,?> configurationQuery = this.visitConfiguratorType(this.configuratorType, "", null, configuratorQueries);
		if(configurationQuery == null) {
			return Mono.just(null);
		}
		return configurationQuery.execute()
			.collectList()
			.map(results -> this.createConfigurer(this.configuratorType, configuratorQueries, results))
			.map(configurationCreator);
	}


	private <E> Consumer<E> createConfigurer(Class<E> configuratorType, List<ConfiguratorQuery> configuratorQueries, List<? extends ConfigurationQueryResult> results) {
		return configurator -> {
			for(int i = 0, j = 0; i < configuratorQueries.size() && j < results.size(); i++, j++) {
				ConfiguratorQuery configuratorQuery = configuratorQueries.get(i);
				try {
					if (configuratorQuery.nested) {
						int configuratorsFrom = ++i;
						int propertiesFrom = j;
						boolean empty = true;
						for (int k = 0; k < configuratorQuery.nestedCount; k++) {
							i++;
							empty = empty && !results.get(j).getResult().isPresent();
							if (!configuratorQueries.get(configuratorsFrom + k).nested) {
								j++;
							}
						}
						int configuratorsTo = i--;
						int propertiesTo = j--;
						if (!empty) {
							configuratorQuery.method.invoke(configurator, this.createConfigurer(configuratorQuery.type, configuratorQueries.subList(configuratorsFrom, configuratorsTo), results.subList(propertiesFrom, propertiesTo)));
						}
					} 
					else if (results.get(j).getResult().isPresent()) {
						if(configuratorQuery.array) {
							configuratorQuery.method.invoke(configurator, new Object[] {results.get(j).getResult().get().asArrayOf(configuratorQuery.componentType).orElse(null)});
						}
						else if(configuratorQuery.collection || configuratorQuery.list) {
							configuratorQuery.method.invoke(configurator, new Object[] {results.get(j).getResult().get().asListOf(configuratorQuery.componentType).orElse(null)});
						}
						else if(configuratorQuery.set) {
							configuratorQuery.method.invoke(configurator, new Object[] {results.get(j).getResult().get().asSetOf(configuratorQuery.componentType).orElse(null)});
						}
						else {
							Optional<?> arg = results.get(j).getResult().get().as(configuratorQuery.type);
							configuratorQuery.method.invoke(configurator, new Object[] {arg.isPresent() ? arg.get() : ConfiguratorTypeConfigurationLoader.getDefaultValue(configuratorQuery.type)});
						}
					}
				}
				catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new RuntimeException("Error invoking configurator for " + configuratorType.getCanonicalName() + ": " + e.getMessage(), e);
				}
			}
		};
	}
	
	private ExecutableConfigurationQuery<?,?> visitConfiguratorType(Class<?> configuratorType, String prefix, ConfigurationQuery<?,?> configurationQuery, List<ConfiguratorQuery> accumulator) throws ConfigurationLoaderException, IllegalArgumentException {
		ExecutableConfigurationQuery<?,?> exectuableConfigurationQuery = null;
		for(Method method : configuratorType.getMethods()) {
			if(method.getParameters().length == 1 && method.getReturnType().equals(configuratorType)) {
				ConfiguratorQuery configuratorQuery;
				try {
					configuratorQuery = new ConfiguratorQuery(prefix, method);
				} 
				catch (ClassNotFoundException e) {
					throw new ConfigurationLoaderException("Error while loading configuration with configurator " + configuratorType.getCanonicalName() + ": " + e.getMessage());
				}
				accumulator.add(configuratorQuery);
				if(configuratorQuery.nested) {
					int initialCount = accumulator.size();
					exectuableConfigurationQuery = this.visitConfiguratorType(configuratorQuery.type, configuratorQuery.name, exectuableConfigurationQuery != null ? exectuableConfigurationQuery.and() : configurationQuery, accumulator);
					configuratorQuery.nestedCount = accumulator.size() - initialCount;
				}
				else {
					exectuableConfigurationQuery = (exectuableConfigurationQuery != null ? exectuableConfigurationQuery.and().get(configuratorQuery.name) : configurationQuery != null ? configurationQuery.get(configuratorQuery.name) : this.source.get(configuratorQuery.name)).withParameters(ConfiguratorTypeConfigurationLoader.this.parameters);
				}
			}
		}
		return exectuableConfigurationQuery;
	}

	private class ConfiguratorQuery {
		
		private Method method;
		
		private String name;
		
		private Class<?> type;
		
		private Class<?> componentType;
		
		private boolean array;
		
		private boolean collection;
		
		private boolean list;
		
		private boolean set;
		
		private boolean nested;
		
		private int nestedCount;
		
		private ConfiguratorQuery(String prefix, Method method) throws ClassNotFoundException {
			this.method = method;
			this.name = (StringUtils.isNotEmpty(prefix) ? prefix + "." : "") + method.getName();
			
			java.lang.reflect.Parameter parameter = method.getParameters()[0];
			this.nested = parameter.getType().equals(Consumer.class);
			if(this.nested) {
				this.type = Class.forName(((ParameterizedType)parameter.getParameterizedType()).getActualTypeArguments()[0].getTypeName());
			}
			else {
				this.type = parameter.getType();
				this.array = this.type.isArray();
				this.collection = Collection.class.equals(this.type);
				this.list = List.class.equals(this.type);
				this.set = Set.class.equals(this.type);
				
				if(this.array) {
					this.componentType = this.type.getComponentType();
				}
				else if(this.collection || this.list || this.set) {
					this.componentType = Class.forName(((ParameterizedType)parameter.getParameterizedType()).getActualTypeArguments()[0].getTypeName());
				}
			}
		}
	}
}
