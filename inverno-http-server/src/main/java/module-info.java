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
 * The Inverno framework HTTP server module provides an HTTP1.x and HTTP/2 server.
 * </p>
 * 
 * <p>
 * It defines the following sockets:
 * </p>
 * 
 * <dl>
 * <dt><b>netService (required)</b></dt>
 * <dd>the Net service used to create the HTTP server</dd>
 * <dt><b>reactor (required)</b></dt>
 * <dd>the Reactor</dd>
 * <dt><b>resourceService (required)</b></dt>
 * <dd>the resource service used to load resources required by the HTTP server (eg. key store...)</dd>
 * <dt><b>configuration</b></dt>
 * <dd>the HTTP server module configuration</dd>
 * <dt><b>controller</b></dt>
 * <dd>override the default HTTP server controller used to process server exchanges</dd>
 * <dt><b>headerCodecs</b></dt>
 * <dd>custom header codecs</dd>
 * <dt><b>parameterConverter</b></dt>
 * <dd>override the default parameter converter used in {@link io.inverno.mod.http.base.Parameter} instances to convert their values</dd>
 * </dl>
 *
 * <p>
 * It exposes the following beans:
 * </p>
 * 
 * <dl>
 * <dt><b>configuration</b></dt>
 * <dd>the HTTP server module configuration</dd>
 * <dt><b>controller</b></dt>
 * <dd>the HTTP server controller</dd>
 * </dl>
 * 
 * <p>
 * A simple HTTP server using the default configuration can be started as follows:
 * </p>
 *
 * <pre>{@code
 * NetService netService = ...
 * Reactor reactor = ...
 * ResourceService resourceService = ...
 *
 * Application.with(new Server.Builder(netService, reactor, resourceService)
 *     .setConfiguration(HttpServerConfigurationLoader.load(conf -> conf.server_port(8080)))
 *     .setController(ServerController.from(
 *         exchange -> exchange
 *             .response()
 *             .body()
 *             .raw()
 *             .value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hello, world!", Charsets.DEFAULT)))
 *      ))
 * ).run();
 * }</pre>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@io.inverno.core.annotation.Module
module io.inverno.mod.http.server {
	requires io.inverno.core;
	requires static io.inverno.core.annotation; // for javadoc...
	
	requires transitive io.inverno.mod.base;
	requires io.inverno.mod.configuration;
	requires transitive io.inverno.mod.http.base;

	requires static com.aayushatharva.brotli4j;
	requires transitive io.netty.buffer;
	requires io.netty.common;
	requires io.netty.codec;
	requires io.netty.codec.http;
	requires io.netty.codec.http2;
	requires io.netty.handler;
	requires jdk.unsupported; // required by netty for low level API for accessing direct buffers
	requires org.apache.commons.text;
	requires org.apache.logging.log4j;
	requires transitive org.reactivestreams;
	requires transitive reactor.core;

	exports io.inverno.mod.http.server;
	exports io.inverno.mod.http.server.ws;
}
