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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.configuration.ConfigurationSource;
import io.inverno.mod.discovery.AbstractConfigurationDiscoveryService;
import io.inverno.mod.discovery.CompositeDiscoveryService;
import io.inverno.mod.discovery.DiscoveryService;
import io.inverno.mod.discovery.Service;
import io.inverno.mod.discovery.ServiceID;
import io.inverno.mod.discovery.http.HttpDiscoveryService;
import io.inverno.mod.discovery.http.HttpServiceInstance;
import io.inverno.mod.discovery.http.HttpTrafficPolicy;
import io.inverno.mod.discovery.http.meta.HttpMetaDiscoveryConfiguration;
import io.inverno.mod.discovery.http.meta.HttpMetaServiceDescriptor;
import io.inverno.mod.http.client.UnboundExchange;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An HTTP meta service discovery service bean that resolves JSON {@link HttpMetaServiceDescriptor service descriptors} from a {@link ConfigurationSource}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
@Bean
public class ConfigurationHttpMetaDiscoveryService extends AbstractConfigurationDiscoveryService<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy, HttpMetaServiceDescriptor> implements @Provide HttpDiscoveryService {

	private final HttpMetaDiscoveryConfiguration configuration;
	private final ObjectMapper objectMapper;
	private final DiscoveryService<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> destinationDiscoveryService;

	/**
	 * <p>
	 * Creates an HTTP meta service discovery service.
	 * </p>
	 *
	 * @param configuration       the HTTP meta service discovery module configuration
	 * @param configurationSource the configuration source
	 * @param objectMapper        an object mapper
	 * @param discoveryServices   a list of discovery services for resolving destination URIs
	 */
	public ConfigurationHttpMetaDiscoveryService(
		HttpMetaDiscoveryConfiguration configuration,
		ConfigurationSource configurationSource,
		ObjectMapper objectMapper,
		List<? extends HttpDiscoveryService> discoveryServices
	) {
		super(Set.of("conf"), configuration.meta_service_configuration_key_prefix(), configurationSource);

		this.configuration = configuration;
		this.objectMapper = objectMapper;

		List<DiscoveryService<? extends HttpServiceInstance, ? super UnboundExchange<?>, ? super HttpTrafficPolicy>> discoveryServicesAndSelf = new ArrayList<>(discoveryServices);
		discoveryServicesAndSelf.add(this);
		this.destinationDiscoveryService = new CompositeDiscoveryService<>(discoveryServicesAndSelf);
	}

	@Override
	protected HttpMetaServiceDescriptor readServiceDescriptor(String content) throws Exception {
		JsonNode json = null;
		try {
			json = this.objectMapper.readTree(content);
		}
		catch (JsonProcessingException e) {
			try {
				return createServiceDescriptor(URI.create(content));
			}
			catch(IllegalArgumentException e1) {
				e1.addSuppressed(e);
				throw e1;
			}
		}

		if(json.isTextual()) {
			return createServiceDescriptor(URI.create(json.asText()));
		}
		else if(json.isArray()) {
			ArrayNode jsonArray = (ArrayNode)json;
			if(!jsonArray.isEmpty()) {
				if(jsonArray.get(0).isTextual()) {
					// list of URIs
					return createServiceDescriptor(this.objectMapper.treeToValue(jsonArray, URI[].class));
				}
				else if(jsonArray.get(0).isObject()) {
					// list of destinations
					return createServiceDescriptor(this.objectMapper.treeToValue(jsonArray, new TypeReference<List<HttpMetaServiceDescriptor.DestinationDescriptor>>() {}));
				}
				else {
					throw new IllegalArgumentException("Must be a URI, an array of URIs, an array of destinations or a valid HTTP service configuration descriptor");
				}
			}
			else {
				throw new IllegalArgumentException("Empty array");
			}
		}
		else if(json.isObject()) {
			// HttpMetaServiceDescriptor
			return this.objectMapper.treeToValue(json, HttpMetaServiceDescriptor.class);
		}
		else {
			throw new IllegalArgumentException("Must be a URI, an array of URIs, an array of destinations or a valid HTTP service configuration descriptor");
		}
	}

	@Override
	protected Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> createService(ServiceID serviceId, Mono<HttpMetaServiceDescriptor> serviceDescriptor) {
		return new HttpMetaService(serviceId, serviceDescriptor, this.destinationDiscoveryService);
	}

	/**
	 * <p>
	 * Creates an HTTP meta service descriptor from a single URI.
	 * </p>
	 *
	 * <p>
	 * This is used when the configuration value is a single URI resulting in a service descriptor with one route with one destination.
	 * </p>
	 *
	 * @param uri a destination URI
	 *
	 * @return an HTTP meta service descriptor
	 */
	private static HttpMetaServiceDescriptor createServiceDescriptor(URI uri) {
		List<HttpMetaServiceDescriptor.DestinationDescriptor> destinations = List.of(new HttpMetaServiceDescriptor.DestinationDescriptor(
			null,
			null,
			uri,
			null,
			null,
			null
		));

		List<HttpMetaServiceDescriptor.RouteDescriptor> routes = List.of(new HttpMetaServiceDescriptor.RouteDescriptor(
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			destinations
		));

		return new HttpMetaServiceDescriptor(
			null,
			null,
			routes
		);
	}

	/**
	 * <p>
	 * Creates an HTTP meta service descriptor from a list of URIs.
	 * </p>
	 *
	 * <p>
	 * This is used when the configuration value is an array of URIs resulting in a service descriptor with one route with multiple destinations.
	 * </p>
	 *
	 * @param uris an array of URIs
	 *
	 * @return an HTTP meta service descriptor
	 */
	private static HttpMetaServiceDescriptor createServiceDescriptor(URI[] uris) {
		List<HttpMetaServiceDescriptor.DestinationDescriptor> destinations = Arrays.stream(uris)
			.map(uri -> new HttpMetaServiceDescriptor.DestinationDescriptor(
				null,
				null,
				uri,
				null,
				null,
				null
			))
			.collect(Collectors.toList());

		List<HttpMetaServiceDescriptor.RouteDescriptor> routes = List.of(new HttpMetaServiceDescriptor.RouteDescriptor(
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			destinations
		));

		return new HttpMetaServiceDescriptor(
			null,
			null,
			routes
		);
	}

	/**
	 * <p>
	 * Creates an HTTP meta service descriptor from multiple destination descriptors.
	 * </p>
	 *
	 * <p>
	 * This is used when the configuration value is an array of destination descriptors resulting in a service descriptor with one route with multiple destinations.
	 * </p>
	 *
	 * @param destinations an array of destination descriptor
	 *
	 * @return an HTTP meta service descriptor
	 */
	private static HttpMetaServiceDescriptor createServiceDescriptor(List<HttpMetaServiceDescriptor.DestinationDescriptor> destinations) {
		List<HttpMetaServiceDescriptor.RouteDescriptor> routes = List.of(new HttpMetaServiceDescriptor.RouteDescriptor(
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			destinations
		));

		return new HttpMetaServiceDescriptor(
			null,
			null,
			routes
		);
	}
}
