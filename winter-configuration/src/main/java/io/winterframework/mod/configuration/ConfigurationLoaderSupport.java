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
package io.winterframework.mod.configuration;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
public abstract class ConfigurationLoaderSupport<A,B,C extends ConfigurationLoaderSupport<A,B,C>> extends AbstractConfigurationLoader<A, C> {

	private Supplier<A> defaultConfigurationSupplier;
	private String[] properties;
	private Function<List<? extends ConfigurationQueryResult<?,?>>, Consumer<B>> resultsToConfigurer;
	private Function<Consumer<B>, A> configurationCreator;
	
	private Consumer<B> configurer;
	
	public ConfigurationLoaderSupport(String[] properties, Function<List<? extends ConfigurationQueryResult<?,?>>, Consumer<B>> resultsToConfigurer, Function<Consumer<B>, A> configurationCreator, Supplier<A> defaultConfigurationSupplier) {
		this.defaultConfigurationSupplier = defaultConfigurationSupplier;
		this.properties = properties;
		this.resultsToConfigurer = resultsToConfigurer;
		this.configurationCreator = configurationCreator;
	}
	
	@SuppressWarnings("unchecked")
	public C withConfigurer(Consumer<B> configurer) {
		this.configurer = configurer;
		return (C)this;
	}
	
	public Mono<A> load() {
		if(this.source != null) {
			return this.source
				.get(this.properties)
				.withParameters(this.parameters)
				.execute()
				.map(this.resultsToConfigurer)
				.map(configurer -> (this.configurer != null ? configurer.andThen(this.configurer) : configurer))
				.map(this.configurationCreator);
		}
		else {
			return Mono.fromSupplier(this.defaultConfigurationSupplier);
		}
	}
	
	protected static class ConfigurationBeanSupport<A, B, C extends ConfigurationLoaderSupport<A,B,C>> implements Supplier<A> {
		
		protected C loader;
		
		private Mono<A> mono;
		
		public ConfigurationBeanSupport(C loader) {
			this.loader = loader;
		}
		
		@Override
		public A get() {
			if(this.mono == null) {
				this.mono = this.loader.load();
			}
			return this.mono.block();
		}
	}
}
