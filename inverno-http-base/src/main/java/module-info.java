import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.HeaderService;

/*
 * Copyright 2021 Jeremy KUHN
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
 * The Inverno framework HTTP base module defines the base APIs and services for
 * HTTP client and server implementations.
 * </p>
 * 
 * <p>It defines the following sockets:</p>
 * 
 * <dl>
 * <dt><b>headerCodecs</b></dt>
 * <dd>extend the extends header service capabilities with a list of custom header codecs</dd>
 * <dt><b>parameterConverter</b></dt>
 * <dd>override the default parameter converter used in {@link Parameter} instances to convert their values</dd>
 * </dl>
 * 
 * <p>
 * It exposes the following beans:
 * </p>
 * 
 * <dl>
 * <dt><b>headerService</b></dt>
 * <dd>A {@link HeaderService} used to decode and encode HTTP header fields.</dd>
 * </dl>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@io.inverno.core.annotation.Module
module io.inverno.mod.http.base {
	requires io.inverno.core;
	requires static io.inverno.core.annotation; // for javadoc...
	
	requires transitive io.inverno.mod.base;
	
	requires transitive io.netty.buffer;
	requires io.netty.common;
	requires io.netty.codec;
	requires io.netty.codec.http;
	requires io.netty.codec.http2;

	exports io.inverno.mod.http.base;
	exports io.inverno.mod.http.base.header;
	exports io.inverno.mod.http.base.ws;

	exports io.inverno.mod.http.base.internal to io.inverno.mod.http.client, io.inverno.mod.http.server;
	exports io.inverno.mod.http.base.internal.header to io.inverno.mod.http.client, io.inverno.mod.http.server, io.inverno.mod.web;
	exports io.inverno.mod.http.base.internal.netty to io.inverno.mod.http.client, io.inverno.mod.http.server;
	exports io.inverno.mod.http.base.internal.ws to io.inverno.mod.http.client, io.inverno.mod.http.server;
}
