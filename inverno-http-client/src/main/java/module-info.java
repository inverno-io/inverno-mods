/*
 * Copyright 2022 Jeremy KUHN
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
import io.inverno.mod.http.base.Parameter;

/**
 * <p>
 * The Inverno framework HTTP client module provides a HTTP1.x and HTTP/2 client.
 * </p>
 * 
 * <p>
 * It defines the following sockets:
 * </p>
 * 
 * <dl>
 * <dt><b>httpClientConfiguration</b></dt>
 * <dd>the HTTP client module configuration</dd>
 * <dt><b>netService (required)</b></dt>
 * <dd>the Net service used to create the HTTP client</dd>
 * <dt><b>resourceService (required)</b></dt>
 * <dd>the resource service used to load resources required by the HTTP client (eg. key store...)</dd>
 * <dt><b>reactor (required)</b></dt>
 * <dd>the reactor used in the connection pool</dd>
 * <dt><b>headerCodecs</b></dt>
 * <dd>custom header codecs</dd>
 * <dt><b>parameterConverter</b></dt>
 * <dd>override the default parameter converter used in {@link Parameter} instances to convert their values</dd>
 * </dl>
 * 
 * <p>
 * It exposes the following beans:
 * </p>
 * 
 * <dl>
 * <dt><b>httpClientConfiguration</b></dt>
 * <dd>the HTTP client module configuration</dd>
 * <dt><b>httpClient</b></dt>
 * <dd>the HTTP client</dd>
 * </dl>
 * 
 * <p>
 * A simple HTTP client using the default configuration can be started as follows:
 * </p>
 * 
 * <pre>{@code
 * NetService netService = ...;
 * Reactor reactor = ...;
 * 
 * Client client = Application.with(new Client.Builder(netService, reactor)).run();
 * 
 * Endpoint endpoint = client.httpClient().endpoint("example.com". 80).build();
 * 
 * String response = endpoint.request(Method.GET, "/")
 *	.send()
 *	.flatMapMany(exchange -> exchange.response().body().string().stream())
 *	.reduceWith(() -> new StringBuilder(), (acc, chunk) -> acc.append(chunk))
 *	.map(StringBuilder::toString).block();
 * }</pre>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
@io.inverno.core.annotation.Module
module io.inverno.mod.http.client {
	requires io.inverno.core;
	requires static io.inverno.core.annotation; // for javadoc...
	
	requires transitive io.inverno.mod.base;
	requires io.inverno.mod.configuration;
	requires transitive io.inverno.mod.http.base;
	
	requires org.apache.logging.log4j;
	
	requires jdk.unsupported; // required by netty for low level API for accessing direct buffers
	requires transitive reactor.core;
	requires transitive org.reactivestreams;
	requires transitive io.netty.buffer;
	requires io.netty.common;
	requires io.netty.codec;
	requires io.netty.codec.http;
	requires io.netty.codec.http2;
	requires io.netty.handler;
	
	requires static com.aayushatharva.brotli4j;
	
	exports io.inverno.mod.http.client;
	exports io.inverno.mod.http.client.ws;
}
