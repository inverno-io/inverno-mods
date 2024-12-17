/*
 * Copyright 2024 Jeremy KUHN
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
 * The Inverno framework Web client module provides a Web enabled HTTP1.x and HTTP/2 client.
 * </p>
 *
 * <p>
 * It combines Inverno HTTP discovery module and HTTP client module behind an API that abstract service resolution and service request logic. The Web client API also extends the HTTP client API with
 * global interceptor definitions and automatic content encoding and decoding.
 * </p>
 *
 * <p>
 * It defines the following sockets:
 * </p>
 *
 * <dl>
 * <dt><b>discoveryServices (required)</b></dt>
 * <dd>The list of HTTP discovery services used to resolve service URIs.</dd>
 * <dt><b>httpClient (required)</b></dt>
 * <dd>The HTTP client used to create unbound exchanges.</dd>
 * <dt><b>mediaTypeConverters (required)</b></dt>
 * <dd>The media type converters used to encode and decode payloads.</dd>
 * <dt><b>reactor (required)</b></dt>
 * <dd>The reactor providing the event loop used to refresh cached services.</dd>
 * <dt><b>configuration</b></dt>
 * <dd>The Web client module configuration.</dd>
 * <dt><b>parameterConverter</b></dt>
 * <dd>A parameter converter used to convert parameter values.</dd>
 * </dl>
 *
 * <p>
 * It exposes the following beans:
 * </p>
 *
 * <dl>
 * <dt><b>configuration</b></dt>
 * <dd>The Web client module configuration</dd>
 * <dt><b>webClientBoot</b></dt>
 * <dd>The Web client boot used to initialize the root Web client</dd>
 * </dl>
 *
 * <p>
 * A Web client can be creates and used as follows:
 * </p>
 *
 * <pre>{@code
 * List<? extends HttpDiscoveryService> discoveryServices = ... // dnsHttpDiscoveryService, configurationHttpMetaDiscoveryService...
 * HttpClient httpClient = ...
 * List<MediaTypeConverter<ByteBuf>> mediaTypeConverters = ...
 * Reactor reactor = ...
 *
 * Client module = new Client.Builder(discoveryServices, httpClient, mediaTypeConverters, reactor).build();
 * try {
 *     WebClient<AppContext> rootWebClient = module.webClientBoot().webClient(AppContext::new);
 *     String responseBody = rootWebClient
 *         .exchange(URI.create("http://example.org"))
 *         .flatMap(WebExchange::response)
 *         .flatMapMany(response -> response.body().string().stream())
 *         .collect(Collectors.joining())
 *         .block();
 * }
 * finally {
 *     module.stop();
 * }
 * }</pre>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
@io.inverno.core.annotation.Module( excludes = {"io.inverno.mod.discovery.http", "io.inverno.mod.http.client"})
module io.inverno.mod.web.client {
	requires io.inverno.core;
	requires static io.inverno.core.annotation; // for javadoc...
	
	requires transitive io.inverno.mod.base;
	requires io.inverno.mod.configuration;
	requires transitive io.inverno.mod.discovery;
	requires transitive io.inverno.mod.discovery.http;
	requires transitive io.inverno.mod.http.client;
	requires transitive io.inverno.mod.web.base;

	requires org.apache.logging.log4j;
	requires org.reactivestreams;
	requires reactor.core;
	
	exports io.inverno.mod.web.client;
	exports io.inverno.mod.web.client.annotation;
	exports io.inverno.mod.web.client.ws;
}
