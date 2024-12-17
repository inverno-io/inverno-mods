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
 * The Inverno framework gRPC server module provides a gRPC server.
 * </p>
 * 
 * <p>
 * It defines the following sockets:
 * </p>
 * 
 * <dl>
 * <dt><b>netService (required)</b></dt>
 * <dd>the Net service providing the ByteBuf allocator</dd>
 * <dt><b>configuration</b></dt>
 * <dd>the gRPC server module configuration</dd>
 * <dt><b>extensionRegistry</b></dt>
 * <dd>the Protocol buffer extension registry</dd>
 * <dt><b>messageCompressors</b></dt>
 * <dd>custom gRPC message compressors</dd>
 * </dl>
 * 
 * <p>
 * It exposes the following beans:
 * </p>
 * 
 * <dl>
 * <dt><b>configuration</b></dt>
 * <dd>the gRPC server module configuration</dd>
 * <dt><b>grpcServer</b></dt>
 * <dd>the gRPC server</dd>
 * </dl>
 * 
 * <p>
 * A simple gRPC server can be started with an HTTP/2 server by transforming server HTTPexchange handler into a server gRPC exchange handler as follows:
 * </p>
 * 
 * <pre>{@code
 * GrpcServer grpcServer = ...
 * NetService netService = ...
 * ResourceService resourceService = ...
 * 		
 * Application.with(new Server.Builder(netService, resourceService)
 *     .setConfiguration(HttpServerConfigurationLoader.load(conf -> conf.server_port(8080).h2_enabled(true)))
 *     .setController(ServerController.from(
 *         grpcServer.unary(
 *             HelloRequest.getDefaultInstance(), 
 *             HelloReply.getDefaultInstance(), 
 *             (GrpcExchange.Unary<ExchangeContext, HelloRequest, HelloReply> grpcExchange) -> {
 *                 grpcExchange.response().value(
 *                     grpcExchange.request().value().map(request -> HelloReply.newBuilder().setMessage("Hello " + request.getName()).build())
 *                 );
 *             }
 *         )
 *     ))
 * ).run();
 * }</pre>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
@io.inverno.core.annotation.Module( excludes = {"io.inverno.mod.http.base", "io.inverno.mod.http.server"} )
module io.inverno.mod.grpc.server {
	requires io.inverno.core;
	requires static io.inverno.core.annotation; // for javadoc...
	
	requires transitive io.inverno.mod.base;
	requires transitive io.inverno.mod.configuration;
	requires transitive io.inverno.mod.grpc.base;
	requires transitive io.inverno.mod.http.base;
	requires transitive io.inverno.mod.http.server;
	
	requires transitive com.google.protobuf;
	requires io.netty.buffer;
	requires org.apache.logging.log4j;
	requires transitive org.reactivestreams;
	requires transitive reactor.core;
	
	exports io.inverno.mod.grpc.server;
}
