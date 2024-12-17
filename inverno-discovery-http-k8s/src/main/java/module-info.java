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
 * The Inverno framework HTTP Kubernetes discovery module provides services to resolve HTTP services in a Kubernetes cluster.
 * </p>
 *
 * <p>
 * The module defines the following sockets:
 * </p>
 *
 * <dl>
 * <dt><b>httpClient (required)</b></dt>
 * <dd>the HTTP client</dd>
 * <dt><b>configuration</b></dt>
 * <dd>the HTTP discovery meta module configuration</dd>
 * </dl>
 *
 * <p>
 * It exposes the following beans:
 * </p>
 *
 * <dl>
 * <dt><b>configuration</b></dt>
 * <dd>the HTTP discovery meta module configuration</dd>
 * <dt><b>k8sEnvHttpDiscoveryService</b></dt>
 * <dd>the Kubernetes environment HTTP discovery service</dd>
 * </dl>
 *
 * <p>
 * The Kubernetes environment HTTP discovery service resolves services from service environment variables, a Kubernetes environment HTTP service URI being of the form
 * {@code k8s-env://<service_name>}.
 * </p>
 *
 * <pre>{@code
 * HttpClient httpClient = ...
 *
 * K8s k8sDiscoveryModule = new K8s.Builder(httpClient).build();
 * try {
 *     k8sDiscoveryModule.start();
 *
 *     httpClient.exchange("/")
 *         .flatMap(exchange -> k8sDiscoveryModule.k8sEnvHttpDiscoveryService().resolve(ServiceId.of("k8s-env://sampleService")) // resolve the meta service
 *              .flatMap(service -> service.getInstance(exchange))                                                               // get an instance
 *              .map(serviceInstance -> serviceInstance.bind(exchange))                                                          // bind the exchange to the instance
 *          )
 *          .flatMap(Exchange::response)                                                                                         // send the request
 *          ...
 * }
 * finally {
 *     k8sDiscoveryModule.stop();
 * }
 * }</pre>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
@io.inverno.core.annotation.Module( excludes = {"io.inverno.mod.http.client", "io.inverno.mod.discovery.http"} )
module io.inverno.mod.discovery.http.k8s {
	requires io.inverno.core;
	requires static io.inverno.core.annotation; // for javadoc...

	requires transitive io.inverno.mod.base;
	requires io.inverno.mod.configuration;
	requires transitive io.inverno.mod.discovery;
	requires transitive io.inverno.mod.discovery.http;
	requires transitive io.inverno.mod.http.client;

	requires org.apache.commons.lang3;
	requires org.reactivestreams;
	requires reactor.core;

	exports io.inverno.mod.discovery.http.k8s;
}