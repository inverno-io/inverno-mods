/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.discovery.http.meta.internal;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Overridable;
import io.inverno.core.annotation.Wrapper;
import io.inverno.mod.boot.json.InvernoBaseModule;
import io.inverno.mod.configuration.ConfigurationSource;
import io.inverno.mod.discovery.http.HttpDiscoveryService;
import java.util.List;
import java.util.function.Supplier;

/**
 * <p>
 * Defines the sockets bean exposed by the module.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public final class ModuleSockets {

	private ModuleSockets() {}

	/**
	 * <p>
	 * The service configuration source socket bean.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	@Bean(name = "serviceConfigurationSource")
	public interface ServiceConfigurationSourceSocket extends Supplier<ConfigurationSource> {}

	/**
	 * <p>
	 * The object mapper socket bean.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	@Overridable @Wrapper @Bean( name = "objectMapper", visibility = Bean.Visibility.PRIVATE )
	public static class ObjectMapperSocket implements Supplier<ObjectMapper> {

		private ObjectMapper instance;

		@Override
		public ObjectMapper get() {
			if(this.instance == null) {
				this.instance = JsonMapper.builder()
					.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
					.addModules(new Jdk8Module(), new InvernoBaseModule())
					.build();
			}
			return this.instance;
		}
	}

	/**
	 * <p>
	 * The list of discovery services used to resolve HTTP meta service destinations.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	@Overridable @Wrapper @Bean(name = "discoveryServices", visibility = Bean.Visibility.PRIVATE)
	public static class DiscoveryServicesSocket implements Supplier<List<? extends HttpDiscoveryService>> {

		@Override
		public List<? extends HttpDiscoveryService> get() {
			return List.of();
		}
	}
}
