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
package io.inverno.mod.discovery;

import io.inverno.mod.configuration.ConfigurationSource;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Base configuration based discovery service implementation.
 * </p>
 *
 * <p>
 * Service descriptors specifying the service instances and traffic policy are resolved from a {@link ConfigurationSource} and unmarshalled using {@link #readServiceDescriptor(String)}. They are then used
 * to actually create the service using {@link #createService(ServiceID, Mono)}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the type of service instance
 * @param <B> the type of service request
 * @param <C> the type of traffic policy
 * @param <D> the service descriptor type
 */
public abstract class AbstractConfigurationDiscoveryService<A extends ServiceInstance, B, C extends TrafficPolicy<A, B>, D> extends AbstractDiscoveryService<A, B, C> {

	private final String serviceKeyPrefix;
	private final ConfigurationSource configurationSource;

	/**
	 * <p>
	 * Creates a configuration discovery service.
	 * </p>
	 *
	 * @param supportedSchemes    the set of supported schemes
	 * @param serviceKeyPrefix    the prefix to prepend to the service name when retrieving descriptors from configuration
	 * @param configurationSource the configuration source
	 */
	public AbstractConfigurationDiscoveryService(
			Set<String> supportedSchemes,
			String serviceKeyPrefix,
			ConfigurationSource configurationSource
		) {
		super(supportedSchemes);

		if(StringUtils.isNotBlank(serviceKeyPrefix)) {
			this.serviceKeyPrefix = serviceKeyPrefix.endsWith(".") ? serviceKeyPrefix : serviceKeyPrefix + ".";
		}
		else {
			this.serviceKeyPrefix = "";
		}
		this.configurationSource = configurationSource;
	}

	@Override
	protected final Mono<? extends Service<A, B, C>> doResolve(ServiceID serviceId, C trafficPolicy) throws MalformedServiceDescriptorException {
		Mono<D> serviceDescriptor = this.configurationSource.get(this.serviceKeyPrefix + serviceId.getURI().getHost())
			.execute()
			.single()
			.mapNotNull(result -> result.asString(null))
			.map(content -> {
				try {
					return this.readServiceDescriptor(content);
				}
				catch(Exception e) {
					throw new MalformedServiceDescriptorException(serviceId, e);
				}
			});
		return this.createService(serviceId, serviceDescriptor).refresh(trafficPolicy);
	}

	/**
	 * <p>
	 * Deserializes the service descriptor retrieved from configuration.
	 * </p>
	 *
	 * @param content the descriptor content
	 *
	 * @return service descriptor
	 *
	 * @throws Exception if there was an error reading the descriptor content
	 */
	protected abstract D readServiceDescriptor(String content) throws Exception;

	/**
	 * <p>
	 * Creates a service from service descriptor.
	 * </p>
	 *
	 * @param serviceId         the service ID
	 * @param serviceDescriptor the service descriptor
	 *
	 * @return a service
	 */
	protected abstract Service<A, B, C> createService(ServiceID serviceId, Mono<D> serviceDescriptor);
}
