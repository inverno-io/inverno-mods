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
package io.inverno.mod.discovery.http.k8s;

import io.inverno.mod.configuration.Configuration;

/**
 * <p>
 * HTTP Kubernetes service discovery module configuration
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
@Configuration( name = "configuration" )
public interface K8sHttpDiscoveryConfiguration {

	/**
	 * <p>
	 * Indicates whether HTTPS should be preferred when both HTTP and HTTPS ports are defined in environment variables.
	 * </p>
	 *
	 * @return true to prefer HTTPS, false to prefer HTTP
	 */
	default boolean prefer_https() {
		return true;
	}
}
