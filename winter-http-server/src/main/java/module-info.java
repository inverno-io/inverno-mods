import io.netty.buffer.Unpooled;
import io.winterframework.core.v1.Application;
import io.winterframework.mod.base.Charsets;
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
 * The Winter framework HTTP server module provides a HTTP1.x and HTTP/2 server.
 * </p>
 * 
 * <p>
 * It defines the following sockets:
 * </p>
 * 
 * <dl>
 * <dt>httpServerConfiguration</dt>
 * <dd>the HTTP server module configuration</dd>
 * <dt>netService (required)</dt>
 * <dd>the Net service used to create the HTTP server</dd>
 * <dt>resourceService (required)</dt>
 * <dd>the resource service used to load resources required by the HTTP server
 * (eg. key store...)</dd>
 * <dt>rootHandler</dt>
 * <dd>override the default HTTP server root handler used to process server
 * exchanges</dd>
 * <dt>errorHandler</dt>
 * <dd>override the default HTTP server error handler used to process error
 * exchanges</dd>
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
 * <dt>httpServerConfiguration</dt>
 * <dd>the HTTP server module configuration</dd>
 * <dt>rootHandler</dt>
 * <dd>the HTTP server root exchange handler</dd>
 * <dt>errorHandler</dt>
 * <dd>the HTTP server error exchange handler</dd>
 * </dl>
 * 
 * <p>
 * A simple HTTP server using the default configuration can be started as
 * follows:
 * </p>
 * 
 * <blockquote><pre>
 * NetService netService = ...;
 * ResourceService resourceService = ...;
 *
 * Application.with(new Server.Builder(netService, resourceService)
 *     .setHttpServerConfiguration(HttpServerConfigurationLoader.load(conf -> conf.server_port(8080)))
 *     .setRootHandler(
 *         exchange -> exchange
 *             .response()
 *             .body()
 *             .raw()
 *             .value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hello, world!", Charsets.DEFAULT)))
 *      )
 * ).run();
 * </pre></blockquote>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@io.winterframework.core.annotation.Module
module io.winterframework.mod.http.server {
	requires io.winterframework.core;
	requires static io.winterframework.core.annotation; // for javadoc...
	
	requires transitive io.winterframework.mod.base;
	requires io.winterframework.mod.configuration;
	requires transitive io.winterframework.mod.http.base;
	
	requires org.apache.logging.log4j;
	requires com.fasterxml.jackson.databind;
	
	requires jdk.unsupported; // required by netty for low level API for accessing direct buffers
	requires transitive reactor.core;
	requires transitive org.reactivestreams;
	requires transitive io.netty.buffer;
	requires io.netty.common;
	requires io.netty.codec;
	requires io.netty.codec.http;
	requires io.netty.codec.http2;
	requires io.netty.handler;
	
	exports io.winterframework.mod.http.server;
}