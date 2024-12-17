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
 * The Inverno framework gRPC base module defines the base APIs and services for gRPC client and server implementations.
 * </p>
 * 
 * <p>It defines the following sockets:</p>
 * 
 * <dl>
 * <dt><b>netService (required)</b></dt>
 * <dd>the Net service providing the ByteBuf allocator to message compressors</dd>
 * <dt><b>configuration</b></dt>
 * <dd>the gRPC base module configuration</dd>
 * <dt><b>messageCompressors</b></dt>
 * <dd>custom message compressors to be added to the built-in list of message compressors ({@code gzip}, {@code deflate}, {@code snappy})</dd>
 * </dl>
 * 
 * <p>
 * It exposes the following beans:
 * </p>
 * 
 * <dl>
 * <dt><b>configuration</b></dt>
 * <dd>the gRPC base module configuration</dd>
 * <dt><b>messageCompressorService</b></dt>
 * <dd>the message compressor service which allows to resolve a message compressor from a list of message encodings</dd>
 * </dl>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
@io.inverno.core.annotation.Module(excludes = "io.inverno.mod.http.base")
module io.inverno.mod.grpc.base {
	requires io.inverno.core;
	requires static io.inverno.core.annotation; // for javadoc...
	
	requires transitive io.inverno.mod.base;
	requires io.inverno.mod.configuration;
	requires transitive io.inverno.mod.http.base;
	
	requires transitive com.google.protobuf;
	requires transitive io.netty.buffer;
	requires io.netty.codec;
	requires io.netty.transport;
	requires org.reactivestreams;
	requires reactor.core;
	
	exports io.inverno.mod.grpc.base;
	exports io.inverno.mod.grpc.base.internal to io.inverno.mod.grpc.client, io.inverno.mod.grpc.server;
}
