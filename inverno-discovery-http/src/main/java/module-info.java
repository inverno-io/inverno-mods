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
 * The Inverno framework HTTP service discovery module defines the API for HTTP service discovery and provides the DNS based HTTP discovery service.
 * </p>
 *
 * <p>
 * It defines the following sockets:
 * </p>
 *
 * <dl>
 * <dt><b>httpClient (required)</b></dt>
 * <dd>the HTTP client used to create endpoints</dd>
 * <dt><b>netService (required)</b></dt>
 * <dd>the Net service used for DNS lookups</dd>
 * </dl>
 *
 * <p>
 * It exposes the following beans:
 * </p>
 *
 * <dl>
 * <dt><b>dnsHttpDiscoveryService</b></dt>
 * <dd>the DNS based HTTP discovery service</dd>
 * </dl>
 *
 * <p>
 * The DNS based HTTP discovery service is typically used to resolve standard {@code http://}, {@code https://}, {@code ws://} or {@code wss://} service URIs.
 * </p>
 *
 * <pre>{@code
 * HttpClient httpClient = ...
 * NetService netService = ...
 *
 * Http discoveryHttpModule = new Http.Builder(httpClient, netService).build();
 * try {
 *     discoveryHttpModule.start();
 *
 *     httpClient.exchange("/")
 *         .flatMap(exchange -> discoveryHttpModule.dnsHttpDiscoveryService().resolve(ServiceId.of("http://example.org")) // resolve the service
 *             .flatMap(service -> service.getInstance(exchange))                                                         // get an instance
 *             .map(serviceInstance -> serviceInstance.bind(exchange))                                                    // bind the exchange to the instance
 *         )
 *         .flatMap(Exchange::response)                                                                                   // send the request
 *         ...
 * }
 * finally {
 *     discoveryHttpModule.stop();
 * }
 * }</pre>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
@io.inverno.core.annotation.Module( excludes = {"io.inverno.mod.http.client"} )
module io.inverno.mod.discovery.http {
	requires io.inverno.core;
	requires static io.inverno.core.annotation; // for javadoc...

	requires transitive io.inverno.mod.base;
	requires transitive io.inverno.mod.discovery;
	requires transitive io.inverno.mod.http.client;

	requires org.reactivestreams;
	requires reactor.core;

	exports io.inverno.mod.discovery.http;
}