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

/**
 * <p>
 * The Inverno framework HTTP meta service discovery module defines the meta HTTP service API and provides a configuration based HTTP meta service discovery service.
 * </p>
 *
 * <p>
 * An HTTP meta service composes multiple HTTP services defined as destinations in one or more routes defining the criteria (path, authority, headers...) matched by a given HTTP request to be
 * routed to a particular set of destinations as well as load balancing strategy, request/response transformation, HTTP client configuration...
 * </p>
 *
 * <p>
 * The module defines the following sockets:
 * </p>
 *
 * <dl>
 * <dt><b>serviceConfigurationSource (required)</b></dt>
 * <dd>a configuration source exposing HTTP meta service descriptors</dd>
 * <dt><b>configuration</b></dt>
 * <dd>the HTTP discovery meta module configuration</dd>
 * <dt><b>discoveryServices</b></dt>
 * <dd>the external discovery services used to resolve HTTP meta service destinations</dd>
 * <dt><b>objectMapper</b></dt>
 * <dd>an object mapper to read JSON HTTP meta service descriptors</dd>
 * </dl>
 *
 * <p>
 * It exposes the following beans:
 * </p>
 *
 * <dl>
 * <dt><b>configuration</b></dt>
 * <dd>the HTTP discovery meta module configuration</dd>
 * <dt><b>configurationHttpMetaDiscoveryService</b></dt>
 * <dd>the configuration HTTP meta service discovery service</dd>
 * </dl>
 *
 * <p>
 * The configuration HTTP meta service discovery service resolves services from service descriptors defined in a configuration source, a configuration HTTP meta service URI being of the form
 * {@code conf://<service_name>}.
 * </p>
 *
 * <pre>{@code
 * ConfigurationSource serviceConfigurationSource = ...
 * HttpDiscoveryService dnsHttpDiscoveryService = ...
 *
 * Meta discoveryHttpMetaModule = new Meta.Builder(serviceConfigurationSource)
 *     .setDiscoveryServices(List.of(dnsHttpDiscoveryService))
 *     .build();
 * try {
 *     discoveryHttpMetaModule.start();
 *
 *     httpClient.exchange("/")
 *         .flatMap(exchange -> discoveryHttpMetaModule.configurationHttpMetaDiscoveryService().resolve(ServiceId.of("conf://sampleService")) // resolve the meta service
 *              .flatMap(service -> service.getInstance(exchange))                                                                            // get an instance
 *              .map(serviceInstance -> serviceInstance.bind(exchange))                                                                       // bind the exchange to the instance
 *          )
 *          .flatMap(Exchange::response)                                                                                                      // send the request
 *          ...
 * }
 * finally {
 *     discoveryHttpMetaModule.stop();
 * }
 * }</pre>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
@io.inverno.core.annotation.Module( excludes = {"io.inverno.mod.boot", "io.inverno.mod.discovery.http"} )
module io.inverno.mod.discovery.http.meta {
	requires io.inverno.core;
	requires static io.inverno.core.annotation; // for javadoc...

	requires transitive io.inverno.mod.base;
	requires io.inverno.mod.boot;
	requires io.inverno.mod.configuration;
	requires transitive io.inverno.mod.discovery;
	requires transitive io.inverno.mod.discovery.http;

	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.datatype.jdk8;
	requires org.apache.commons.lang3;
	requires org.reactivestreams;
	requires reactor.core;

	exports io.inverno.mod.discovery.http.meta;

	opens io.inverno.mod.discovery.http.meta to com.fasterxml.jackson.databind;
}
