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
 * The Inverno framework gRPC client module provides a gRPC client.
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
 * <dd>the gRPC client module configuration</dd>
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
 * <dd>the gRPC client module configuration</dd>
 * <dt><b>grpcClient</b></dt>
 * <dd>the gRPC client</dd>
 * </dl>
 * 
 * <p>
 * A remote gRPC service method can be called by using an HTTP/2 client and transforming the client HTTP exchange into a client gRPC exchange as follows:
 * </p>
 * 
 * <pre>{@code
 * HttpClient httpClient = ... 
 * h2Endpoint = httpClient.endpoint("localhost", 8080)
 *     .configuration(HttpClientConfigurationLoader.load(conf -> conf.http_protocol_versions(Set.of(HttpVersion.HTTP_2_0))))
 *     .build();
 * 
 * HelloReply reply = h2Endpoint.exchange()
 *     .map(exchange -> (GrpcExchange.Unary<ExchangeContext, HelloRequest, HelloReply>)grpcClient.unary(
 *         exchange,
 *         GrpcServiceName.of("helloworld", "Greeter"), "SayHello",
 *         HelloRequest.getDefaultInstance(), HelloReply.getDefaultInstance()
 *     ))
 *     .flatMap(grpcExchange -> {
 *         grpcExchange.request().value(
 *             HelloRequest.newBuilder()
 *                 .setName("Bob")
 *                 .build()
 *         );
 *         return grpcExchange.response();
 *     })
 *     .flatMap(GrpcResponse.Unary::value)
 *     .block(); 
 * }</pre>
 * 
 * <p>
 * gRPC is only supported on top of the HTTP/2 protocol, the behaviour of an application using an HTTP/1.x client is undetermined.
 * </p>
 * 
 * <p>
 * It is recommended to rely on a <a href="https://protobuf.dev/reference/cpp/api-docs/google.protobuf.compiler.plugin/">protoc plugin</a> for generating a proper gRPC client stub from Protocol buffer
 * definitions.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
@io.inverno.core.annotation.Module( excludes = {"io.inverno.mod.http.base", "io.inverno.mod.http.client"} )
module io.inverno.mod.grpc.client {
	requires io.inverno.core;
	requires static io.inverno.core.annotation; // for javadoc...
	
	requires transitive io.inverno.mod.base;
	requires io.inverno.mod.configuration;
	requires transitive io.inverno.mod.grpc.base;
	requires transitive io.inverno.mod.http.base;
	requires transitive io.inverno.mod.http.client;
	
	requires transitive com.google.protobuf;
	requires io.netty.codec.http2;
	requires org.apache.logging.log4j;
	requires transitive org.reactivestreams;
	requires transitive reactor.core;
	
	exports io.inverno.mod.grpc.client;
}
