import java.util.List;

import io.netty.buffer.ByteBuf;
import io.winterframework.core.v1.Application;
import io.winterframework.mod.base.converter.MediaTypeConverter;
import io.winterframework.mod.base.resource.MediaTypes;
import io.winterframework.mod.http.base.Method;
import io.winterframework.mod.http.base.Parameter;

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
 * The Winter framework Web server module provides a Web enabled HTTP1.x and
 * HTTP/2 server
 * </p>
 * 
 * <p>
 * It defines a complete APIs for request routing and the creation of REST APIs
 * using a collection of annotations.
 * </p>
 * 
 * <p>
 * It defines the following sockets:
 * </p>
 * 
 * <dl>
 * <dt>webConfiguration</dt>
 * <dd>the web module configuration</dd>
 * <dt>netService (required)</dt>
 * <dd>the net service used to create the HTTP server</dd>
 * <dt>resourceService (required)</dt>
 * <dd>the resource service used to load resources.</dd>
 * <dt>mediaTypeConverters (required)</dt>
 * <dd>the media type converters used to encode and decode payloads based on the
 * content type of a request or a response</dd>
 * <dt>webRouterConfigurer</dt>
 * <dd>the configurer used to specify the resources exposed by the server</dd>
 * <dt>errorRouterConfigurer</dt>
 * <dd>the configurer used to specify how errors are processed and returned to
 * a client</dd>
 * <dt>parameterConverter</dt>
 * <dd>override the default parameter converter used in {@link Parameter}
 * instances to convert their values</dd>
 * </dl>
 * 
 * <p>
 * It exposes the following beans:
 * </p>
 * 
 * <dl>
 * <dt>webConfiguration</dt>
 * <dd>the Web server module configuration</dd>
 * <dt>webRouter</dt>
 * <dd>the router used to route a request to the right handler</dd>
 * <dt>errorHandler</dt>
 * <dd>the router used to route a failed request to the right handler</dd>
 * </dl>
 * 
 * <p>
 * A Web server can then be started as follows:
 * </p>
 * 
 * <blockquote>
 * 
 * <pre>
 * NetService netService = ...;
 * ResourceService resourceService = ...;
 * List{@literal <MediaTypeConverter<ByteBuf>>} mediaTypeConverters = ...;
 *
 * Application.with(new Web.Builder(netService, resourceService, mediaTypeConverters)
 *     .setWebConfiguration(WebConfigurationLoader.load(conf -> conf.web(http_conf -> http_conf.server_port(8080))))
 *     .setWebRouterConfigurer(router -> router
 *         .route()
 *             .path("/path/to/resource1")
 *             .method(Method.GET)
 *             .produces(MediaTypes.APPLICATION_JSON)
 *             .produces(MediaTypes.TEXT_PLAIN)
 *             .handler(exchange -> exchange
 *                 .response()
 *                 .body()
 *                 .encoder()
 *                 .value("Resource 1")
 *             )
 *         .route()
 *             .path("/path/to/resource2")
 *             .method(Method.GET)
 *             .produces(MediaTypes.APPLICATION_JSON)
 *             .produces(MediaTypes.TEXT_PLAIN)
 *             .handler(exchange -> exchange
 *                 .response()
 *                 .body()
 *                 .encoder()
 *                 .value("Resource 2")
 *             )
 *     )
 * ).run();
 * </pre>
 * 
 * </blockquote>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@io.winterframework.core.annotation.Module
module io.winterframework.mod.web {
	requires io.winterframework.core;
	requires static io.winterframework.core.annotation; // for javadoc...
	
	requires transitive io.winterframework.mod.base;
	requires io.winterframework.mod.configuration;
	requires transitive io.winterframework.mod.http.base;
	requires transitive io.winterframework.mod.http.server;
	
	requires org.apache.logging.log4j;
	requires reactor.core;
	requires org.reactivestreams;
	
	exports io.winterframework.mod.web;
	exports io.winterframework.mod.web.annotation;
}