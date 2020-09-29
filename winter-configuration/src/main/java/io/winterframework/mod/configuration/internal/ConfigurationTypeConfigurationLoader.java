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

import io.winterframework.mod.configuration.ConfigurationLoaderException;
import io.winterframework.mod.configuration.ConfigurationQuery;
import io.winterframework.mod.configuration.ConfigurationQueryResult;
import io.winterframework.mod.configuration.ExecutableConfigurationQuery;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
public class ConfigurationTypeConfigurationLoader<A> extends AbstractReflectiveConfigurationLoader<A, ConfigurationTypeConfigurationLoader<A>> {

	private Class<A> configurationType;
	
	public ConfigurationTypeConfigurationLoader(Class<A> configurationType) {
		this.configurationType = configurationType;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Mono<A> load() {
		List<ConfigurationProxyQuery> proxyQueries = new LinkedList<>();
		ExecutableConfigurationQuery<?,?,?> configurationQuery = this.visitConfigurationType(this.configurationType, "", null, proxyQueries);

		if(configurationQuery == null) {
			return Mono.just(null);
		}
		
		return configurationQuery.execute()
			.collectList()
			.map(results -> {
				Map<String, Object> entries = new HashMap<>();
				Iterator<? extends ConfigurationQueryResult<?,?>> resultsIterator = results.iterator();
				for(ConfigurationProxyQuery proxyQuery : proxyQueries) {
					if(proxyQuery.nested) {
						entries.put(proxyQuery.name, Proxy.newProxyInstance(proxyQuery.type.getClassLoader(), new Class[] {proxyQuery.type}, new ConfigurationProxyInvocationHandler(entries, proxyQuery.name))); 
					}
					else {
						resultsIterator.next().getResult().ifPresent(result -> {
							if(proxyQuery.array) {
								entries.put(proxyQuery.name, result.valueAsArrayOf(proxyQuery.componentType).orElse(null));
							}
							else if(proxyQuery.collection) {
								entries.put(proxyQuery.name, result.valueAsCollectionOf(proxyQuery.componentType).orElse(null));
							}
							else if(proxyQuery.list) {
								entries.put(proxyQuery.name, result.valueAsListOf(proxyQuery.componentType).orElse(null));
							}
							else if(proxyQuery.set) {
								entries.put(proxyQuery.name, result.valueAsSetOf(proxyQuery.componentType).orElse(null));
							}
							else {
								entries.put(proxyQuery.name, result.valueAs(proxyQuery.type).orElse(null));
							}
						});
					}
				}
				return (A)Proxy.newProxyInstance(configurationType.getClassLoader(), new Class[] {configurationType}, new ConfigurationProxyInvocationHandler(entries, ""));
			});
	}
	
	private ExecutableConfigurationQuery<?, ?, ?> visitConfigurationType(Class<?> configurationType, String prefix, ConfigurationQuery<?,?,?> configurationQuery, List<ConfigurationProxyQuery> accumulator) throws ConfigurationLoaderException, IllegalArgumentException {
		ExecutableConfigurationQuery<?,?,?> exectuableConfigurationQuery = null;
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
		
		private Method method;
		
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
			this.name = (prefix != null && prefix != "" ? prefix + "." : "") + this.method.getName();
			
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

		private Map<String, Object> entries;
		
		private String prefix;
		
		public ConfigurationProxyInvocationHandler(Map<String, Object> entries, String prefix) {
			this.entries = entries;
			this.prefix = prefix;
		}
		
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			String entryName = method.getName();
			if(this.prefix != null && this.prefix != "") {
				entryName = this.prefix + "." + entryName;
			}
			
			if(this.entries.containsKey(entryName)) {
				return this.entries.get(entryName);
			}
			else if(method.isDefault()) {
				return MethodHandles.lookup()
					.findSpecial( 
						method.getDeclaringClass(), 
						method.getName(),  
						MethodType.methodType(method.getReturnType(), new Class[0]),  
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
