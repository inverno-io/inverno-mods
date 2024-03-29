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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.inverno.mod.configuration.Configuration;
import io.inverno.mod.configuration.ConfigurationLoader;
import io.inverno.mod.configuration.ConfigurationLoaderException;
import io.inverno.mod.configuration.ConfigurationQuery;
import io.inverno.mod.configuration.ConfigurationQueryResult;
import io.inverno.mod.configuration.ExecutableConfigurationQuery;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A {@link ConfigurationLoader} implementation that determines the configuration properties to load by analyzing a configuration interface as defined by {@link Configuration @Configuration} through
 * reflection and eventually returns a Java proxy for the configuration interface providing the retrieved values.
 * </p>
 *
 * <p>
 * Configuration properties must be declared as non-void no-argument methods in the interface. Default values can be specified in default methods.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see Configuration
 * @see ConfigurationLoader
 *
 * @param <A> the configuration type
 */
public class ConfigurationTypeConfigurationLoader<A> extends AbstractReflectiveConfigurationLoader<A, ConfigurationTypeConfigurationLoader<A>> {

	private Class<A> configurationType;
	
	/**
	 * <p>
	 * Creates a configuration loader for the specified type.
	 * </p>
	 *
	 * @param configurationType the configuration type
	 *
	 * @throws IllegalArgumentException if the specified type is not an interface
	 */
	public ConfigurationTypeConfigurationLoader(Class<A> configurationType) {
		if(!configurationType.isInterface()) {
			throw new IllegalArgumentException(configurationType.getCanonicalName() +" is not an interface");
		}
		this.configurationType = configurationType;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Mono<A> load() {
		List<ConfigurationProxyQuery> proxyQueries = new LinkedList<>();
		ExecutableConfigurationQuery<?,?> configurationQuery = this.visitConfigurationType(this.configurationType, "", null, proxyQueries);

		if(configurationQuery == null) {
			return Mono.just(null);
		}
		
		return configurationQuery.execute()
			.collectList()
			.map(results -> {
				Map<String, Object> properties = new HashMap<>();
				Iterator<? extends ConfigurationQueryResult> resultsIterator = results.iterator();
				for(ConfigurationProxyQuery proxyQuery : proxyQueries) {
					if(proxyQuery.nested) {
						properties.put(proxyQuery.name, Proxy.newProxyInstance(proxyQuery.type.getClassLoader(), new Class[] {proxyQuery.type}, new ConfigurationProxyInvocationHandler(properties, proxyQuery.name))); 
					}
					else {
						resultsIterator.next().getResult().ifPresent(result -> {
							if(proxyQuery.array) {
								properties.put(proxyQuery.name, result.asArrayOf(proxyQuery.componentType).orElse(null));
							}
							else if(proxyQuery.collection || proxyQuery.list) {
								properties.put(proxyQuery.name, result.asListOf(proxyQuery.componentType).orElse(null));
							}
							else if(proxyQuery.set) {
								properties.put(proxyQuery.name, result.asSetOf(proxyQuery.componentType).orElse(null));
							}
							else {
								properties.put(proxyQuery.name, result.as(proxyQuery.type).orElse(null));
							}
						});
					}
				}
				return (A)Proxy.newProxyInstance(configurationType.getClassLoader(), new Class<?>[] { this.configurationType }, new ConfigurationProxyInvocationHandler(properties, ""));
			});
	}
	
	private ExecutableConfigurationQuery<?,?> visitConfigurationType(Class<?> configurationType, String prefix, ConfigurationQuery<?,?> configurationQuery, List<ConfigurationProxyQuery> accumulator) throws ConfigurationLoaderException, IllegalArgumentException {
		ExecutableConfigurationQuery<?,?> exectuableConfigurationQuery = null;
		for(Method method : configurationType.getMethods()) {
			if(method.getParameters().length == 0) {
				ConfigurationProxyQuery query;
				try {
					query = new ConfigurationProxyQuery(prefix, method);
				} 
				catch (ClassNotFoundException e) {
					throw new ConfigurationLoaderException("Error while loading configuration " + configurationType.getCanonicalName() + ": " + e.getMessage());
				}
				accumulator.add(query);
				if(query.nested) {
					exectuableConfigurationQuery = this.visitConfigurationType(query.type, query.name, exectuableConfigurationQuery != null ? exectuableConfigurationQuery.and() : configurationQuery, accumulator);
				}
				else {
					exectuableConfigurationQuery = (exectuableConfigurationQuery != null ? exectuableConfigurationQuery.and().get(query.name) : configurationQuery != null ? configurationQuery.get(query.name) : this.source.get(query.name)).withParameters(ConfigurationTypeConfigurationLoader.this.parameters);
				}
			}
		}
		return exectuableConfigurationQuery;
	}
	
	private class ConfigurationProxyQuery {
		
		private final Method method;
		
		private String name;
		
		private Class<?> type;
		
		private Class<?> componentType;
		
		private boolean array;
		
		private boolean collection;
		
		private boolean list;
		
		private boolean set;
		
		private boolean nested;
		
		private ConfigurationProxyQuery(String prefix, Method method) throws ClassNotFoundException {
			this.method = method;
			this.name = (StringUtils.isNotEmpty(prefix) ? prefix + "." : "") + method.getName();
			
			this.type = this.method.getReturnType();
			
			this.array = this.type.isArray();
			this.collection = Collection.class.equals(this.type);
			this.list = List.class.equals(this.type);
			this.set = Set.class.equals(this.type);
			
			this.nested = this.type.isInterface() && !(this.collection || this.list || this.set);
			
			if(this.array) {
				this.componentType = this.type.getComponentType();
			}
			else if(this.collection || this.list || this.set) {
				this.componentType = Class.forName(((ParameterizedType)this.method.getGenericReturnType()).getActualTypeArguments()[0].getTypeName());
			}
		}
	}
	
	private static class ConfigurationProxyInvocationHandler implements InvocationHandler {

		private final Map<String, Object> properties;
		
		private final String prefix;
		
		public ConfigurationProxyInvocationHandler(Map<String, Object> properties, String prefix) {
			this.properties = properties;
			this.prefix = prefix;
		}
		
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			String propertyName = method.getName();
			if(StringUtils.isNotEmpty(prefix)) {
				propertyName = this.prefix + "." + propertyName;
			}
			
			if(this.properties.containsKey(propertyName)) {
				return this.properties.get(propertyName);
			}
			else if(method.isDefault()) {
				return MethodHandles.lookup()
					.findSpecial( 
						method.getDeclaringClass(), 
						method.getName(),  
						MethodType.methodType(method.getReturnType(), new Class<?>[0]),  
						method.getDeclaringClass())
					.bindTo(proxy)
					.invokeWithArguments(args);
			}
			else {
				return ConfigurationTypeConfigurationLoader.getDefaultValue(method.getReturnType());
			}
		}
	}
}
