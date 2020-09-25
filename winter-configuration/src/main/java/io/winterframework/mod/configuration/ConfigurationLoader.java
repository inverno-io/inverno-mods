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

import java.util.function.Consumer;
import java.util.function.Function;

import io.winterframework.mod.configuration.ExecutableConfigurationQuery.Parameter;
import io.winterframework.mod.configuration.internal.ConfigurationTypeConfigurationLoader;
import io.winterframework.mod.configuration.internal.ConfiguratorTypeConfigurationLoader;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
public interface ConfigurationLoader<A, B extends ConfigurationLoader<A, B>> {

	static <E> ConfigurationLoader<E, ?> withConfiguration(Class<E> configurationType) {
		return new ConfigurationTypeConfigurationLoader<>(configurationType);
	}
	
	static <E,F> ConfigurationLoader<E, ?> withConfigurator(Class<F> configuratorType, Function<Consumer<F>, E> configurationCreator) {
		return new ConfiguratorTypeConfigurationLoader<>(configuratorType, configurationCreator);
	}
	
	static Parameter parameter(String name, Object value) {
		return ExecutableConfigurationQuery.parameter(name, value);
	}
	
	default B withParameters(String k1, Object v1) throws IllegalArgumentException {
		return this.withParameters(parameter(k1, v1));
	}
	
	default B withParameters(String k1, Object v1, String k2, Object v2) throws IllegalArgumentException {
		return this.withParameters(parameter(k1, v1), parameter(k2, v2));
	}
	
	default B withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3) throws IllegalArgumentException {
		return this.withParameters(parameter(k1, v1), parameter(k2, v2), parameter(k3, v3));
	}
	
	default B withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) throws IllegalArgumentException {
		return this.withParameters(parameter(k1, v1), parameter(k2, v2), parameter(k3, v3), parameter(k4, v4));
	}
	
	default B withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5) throws IllegalArgumentException {
		return this.withParameters(parameter(k1, v1), parameter(k2, v2), parameter(k3, v3), parameter(k4, v4), parameter(k5, v5));
	}
	
	default B withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6) throws IllegalArgumentException {
		return this.withParameters(parameter(k1, v1), parameter(k2, v2), parameter(k3, v3), parameter(k4, v4), parameter(k5, v5), parameter(k6, v6));
	}
	
	default B withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7) throws IllegalArgumentException {
		return this.withParameters(parameter(k1, v1), parameter(k2, v2), parameter(k3, v3), parameter(k4, v4), parameter(k5, v5), parameter(k6, v6), parameter(k7, v7));
	}
	
	default B withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8) throws IllegalArgumentException {
		return this.withParameters(parameter(k1, v1), parameter(k2, v2), parameter(k3, v3), parameter(k4, v4), parameter(k5, v5), parameter(k6, v6), parameter(k7, v7), parameter(k8, v8));
	}
	
	default B withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9) throws IllegalArgumentException {
		return this.withParameters(parameter(k1, v1), parameter(k2, v2), parameter(k3, v3), parameter(k4, v4), parameter(k5, v5), parameter(k6, v6), parameter(k7, v7), parameter(k8, v8), parameter(k9, v9));
	}
	
	default B withParameters(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, String k7, Object v7, String k8, Object v8, String k9, Object v9, String k10, Object v10) throws IllegalArgumentException {
		return this.withParameters(parameter(k1, v1), parameter(k2, v2), parameter(k3, v3), parameter(k4, v4), parameter(k5, v5), parameter(k6, v6), parameter(k7, v7), parameter(k8, v8), parameter(k9, v9), parameter(k10, v10));
	}
	
	B withParameters(Parameter... parameters) throws IllegalArgumentException;
	
	B withSource(ConfigurationSource<?, ?, ?> source);
	
	Mono<A> load();
}
