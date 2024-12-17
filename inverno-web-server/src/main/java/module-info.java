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

/**
 * <p>
 * The Inverno framework Web server module provides a Web enabled HTTP1.x and HTTP/2 server.
 * </p>
 * 
 * <p>
 * It defines a complete APIs for request routing and the creation of REST APIs using a collection of annotations.
 * </p>
 * 
 * <p>
 * It defines the following sockets:
 * </p>
 *
 * <dl>
 * <dt><b>mediaTypeConverters (required)</b></dt>
 * <dd>the media type converters used to encode and decode payloads based on the content type of the request or the response</dd>
 * <dt><b>netService (required)</b></dt>
 * <dd>the net service used to create the HTTP server</dd>
 * <dt><b>reactor (required)</b></dt>
 * <dd>the reactor providing the event loops processing the requests</dd>
 * <dt><b>resourceService (required)</b></dt>
 * <dd>the resource service used to load static resources</dd>
 * <dt><b>configuration</b></dt>
 * <dd>the Web server module configuration</dd>
 * <dt><b>headerCodecs</b></dt>
 * <dd>custom header codecs used to decode specific headers</dd>
 * <dt><b>parameterConverter</b></dt>
 * <dd>a parameter converter used in {@link io.inverno.mod.http.base.Parameter} instances to convert their values</dd>
 * </dl>
 * 
 * <p>
 * It exposes the following beans:
 * </p>
 * 
 * <dl>
 * <dt><b>configuration</b></dt>
 * <dd>the Web server module configuration</dd>
 * <dt><b>webServerBoot</b></dt>
 * <dd>the Web server boot used to initialize the Web server</dd>
 * </dl>
 * 
 * <p>
 * A basic Web server exposing two routes with white labels error handlers can be started as follows:
 * </p>
 * 
 * <pre>{@code
 * List<MediaTypeConverter<ByteBuf>> mediaTypeConverters = null;
 * NetService netService = null;
 * Reactor reactor = null;
 * ResourceService resourceService = null;
 *
 * Application.run(new Server.Builder(mediaTypeConverters, netService, reactor, resourceService)
 *     .setConfiguration(WebServerConfigurationLoader.load(conf -> conf.http_server(http_conf -> http_conf.server_port(8080))))
 * )
 * .webServerBoot().webServer()                                                           // Initialize the Web server with an empty context factory
 *     .intercept().interceptor(new HttpAccessLogsInterceptor<>())                        // log 2xx HTTP requests
 *     .route()                                                                           // route /path/to/resource1 requests to "Resource 1"
 *         .path("/path/to/resource1")
 *         .method(Method.GET)
 *         .produce(MediaTypes.APPLICATION_JSON)
 *         .produce(MediaTypes.TEXT_PLAIN)
 *         .handler(exchange -> exchange.response().body().encoder().value("Resource 1"))
 *     .route()                                                                           // route /path/to/resource2 requests to "Resource 2"
 *         .path("/path/to/resource2")
 *         .method(Method.GET)
 *         .produce(MediaTypes.APPLICATION_JSON)
 *         .produce(MediaTypes.TEXT_PLAIN)
 *         .handler(exchange -> exchange.response().body().encoder().value("Resource 2"))
 *     .interceptError().interceptor(new HttpAccessLogsInterceptor<>())                   // log 4xx, 5xx HTTP requests
 *     .configureErrorRoutes(new WhiteLabelErrorRoutesConfigurer<>());                    // White label error routes
 * }</pre>
 *
 * <p>
 * Web routes can also be declared in a declarative way using the {@link io.inverno.mod.web.server.annotation.WebController @WebController} as follows:
 * </p>
 *
 * <pre>{@code
 * @WebController( path = "/book" )
 * @Bean( visibility = Bean.Visibility.PRIVATE )
 * public class BookController {
 *
 *     @WebRoute(method = Method.POST, consumes = MediaTypes.APPLICATION_JSON)
 *     void create(@Body Book book) {
 *         ...
 *     }
 *
 *     @WebRoute(path = "/{id}", method = Method.PUT, consumes = MediaTypes.APPLICATION_JSON)
 *     void update(@PathParam String id, @Body Book book);
 *
 *     @WebRoute(method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
 *     Flux<Book> list();
 *
 *     @WebRoute(path = "/{id}", method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
 *     Mono<Book> get(@PathParam String id);
 *
 *     @WebRoute(path = "/{id}", method = Method.DELETE)
 *     void delete(@PathParam String id);
 * }
 * }</pre>
 *
 * <p>
 * The Web compiler will generate corresponding route configurers at compile time.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@io.inverno.core.annotation.Module
module io.inverno.mod.web.server {
	requires io.inverno.core;
	requires static io.inverno.core.annotation; // for javadoc...
	
	requires transitive io.inverno.mod.base;
	requires io.inverno.mod.configuration;
	requires transitive io.inverno.mod.http.server;
	requires transitive io.inverno.mod.web.base;

	requires org.apache.commons.text;
	requires org.apache.logging.log4j;
	requires org.reactivestreams;
	requires reactor.core;
	
	exports io.inverno.mod.web.server;
	exports io.inverno.mod.web.server.annotation;
	exports io.inverno.mod.web.server.ws;
}
