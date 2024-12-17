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
package io.inverno.mod.discovery.http.meta;

import io.inverno.mod.configuration.Configuration;

/**
 * <p>
 * HTTP meta service discovery module configuration.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
@Configuration( name = "configuration" )
public interface HttpMetaDiscoveryConfiguration {

	/**
	 * <p>
	 * The prefix to prepend to the service name when retrieving {@link HttpMetaServiceDescriptor} from a configuration source.
	 * </p>
	 *
	 * <p>
	 * Defaults to {@code io.inverno.mod.discovery.http.meta.service}.
	 * </p>
	 *
	 * @return the meta service key prefix
	 */
	default String meta_service_configuration_key_prefix() {
		return this.getClass().getPackageName() + ".service";
	}
}
