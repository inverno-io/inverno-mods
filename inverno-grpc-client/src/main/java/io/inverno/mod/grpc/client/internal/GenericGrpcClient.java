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
package io.inverno.mod.grpc.client.internal;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.net.NetService;
import io.inverno.mod.grpc.base.GrpcException;
import io.inverno.mod.grpc.base.GrpcHeaders;
import io.inverno.mod.grpc.base.GrpcMessageCompressor;
import io.inverno.mod.grpc.base.GrpcMessageCompressorService;
import io.inverno.mod.grpc.base.GrpcServiceName;
import io.inverno.mod.grpc.base.GrpcStatus;
import io.inverno.mod.grpc.base.internal.GrpcMessageReader;
import io.inverno.mod.grpc.base.internal.GrpcMessageWriter;
import io.inverno.mod.grpc.base.internal.IdentityGrpcMessageCompressor;
import io.inverno.mod.grpc.client.GrpcClient;
import io.inverno.mod.grpc.client.GrpcExchange;
import io.inverno.mod.grpc.client.GrpcRequest;
import io.inverno.mod.grpc.client.GrpcResponse;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.client.Exchange;
import io.netty.handler.codec.http2.Http2Error;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link GrpcClient} implementation.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
@Bean( name = "grpcClient" )
public class GenericGrpcClient implements GrpcClient {
	
	/**
	 * The gRPC message compressor service.
	 */
	private final GrpcMessageCompressorService compressorService;
	/**
	 * The Protocol buffer extension registry
	 */
	private final ExtensionRegistry extensionRegistry;
	/**
	 * The net service.
	 */
	private final NetService netService;

	/**
	 * <p>
	 * Creates a generic gRPC client.
	 * </p>
	 * 
	 * @param compressorService the gRPC message compressor service
	 * @param extensionRegistry the Protocol buffer extension registry
	 * @param netService        the net service
	 */
	public GenericGrpcClient(GrpcMessageCompressorService compressorService, ExtensionRegistry extensionRegistry, NetService netService) {
		this.compressorService = compressorService;
		this.extensionRegistry = extensionRegistry;
		this.netService = netService;
	}

	@Override
	public <A extends ExchangeContext, B extends Exchange<A>, C extends Message, D extends Message, E extends GrpcExchange.Unary<A, C, D>> E unary(B exchange, GrpcServiceName serviceName, String methodName, C defaultRequestInstance, D defaultResponseInstance) {
		return (E)new GenericGrpcExchange.GenericUnary<>(exchange, () -> this.createRequest(exchange, defaultRequestInstance, serviceName, methodName), () -> this.createResponse(exchange, defaultResponseInstance), serviceName, methodName);
	}

	@Override
	public <A extends ExchangeContext, B extends Exchange<A>, C extends Message, D extends Message, E extends GrpcExchange.ClientStreaming<A, C, D>> E clientStreaming(B exchange, GrpcServiceName serviceName, String methodName, C defaultRequestInstance, D defaultResponseInstance) {
		return (E)new GenericGrpcExchange.GenericClientStreaming<>(exchange, () -> this.createRequest(exchange, defaultRequestInstance, serviceName, methodName), () -> this.createResponse(exchange, defaultResponseInstance), serviceName, methodName);
	}

	@Override
	public <A extends ExchangeContext, B extends Exchange<A>, C extends Message, D extends Message, E extends GrpcExchange.ServerStreaming<A, C, D>> E serverStreaming(B exchange, GrpcServiceName serviceName, String methodName, C defaultRequestInstance, D defaultResponseInstance) {
		return (E)new GenericGrpcExchange.GenericServerStreaming<>(exchange, () -> this.createRequest(exchange, defaultRequestInstance, serviceName, methodName), () -> this.createResponse(exchange, defaultResponseInstance), serviceName, methodName);
	}

	@Override
	public <A extends ExchangeContext, B extends Exchange<A>, C extends Message, D extends Message, E extends GrpcExchange.BidirectionalStreaming<A, C, D>> E bidirectionalStreaming(B exchange, GrpcServiceName serviceName, String methodName, C defaultRequestInstance, D defaultResponseInstance) {
		return (E)new GenericGrpcExchange.GenericBidirectionalStreaming<>(exchange, () -> this.createRequest(exchange, defaultRequestInstance, serviceName, methodName), () -> this.createResponse(exchange, defaultResponseInstance), serviceName, methodName);
	}
	
	/**
	 * <p>
	 * Creates a gRPC client request.
	 * </p>
	 *
	 * @param <A>                    the exchange context type
	 * @param <B>                    the client exchange type
	 * @param <C>                    the gRPC request message type
	 * @param <D>                    the gRPC request type
	 * @param exchange               the client HTTP exchange
	 * @param defaultRequestInstance the default request message instance
	 * @param serviceName            the gRPC service name
	 * @param methodName             the gRPC service method name
	 *
	 * @return a new gRPC client request
	 */
	private <A extends ExchangeContext, B extends Exchange<A>, C extends Message, D extends GrpcRequest<C>> D createRequest(B exchange, C defaultRequestInstance, GrpcServiceName serviceName, String methodName) {
		return (D)new GenericGrpcRequest<>(exchange.request(), () -> this.createMessageWriter(exchange), this.extensionRegistry, serviceName, methodName, () -> exchange.reset(Http2Error.CANCEL.code()));
	}
	
	/**
	 * <p>
	 * Creates a gRPC client response.
	 * </p>
	 * 
	 * <p>
	 * It determines the compressor to use based on the {@link GrpcHeaders#NAME_GRPC_MESSAGE_ENCODING} header in the request, if none is specified it falls back to the
	 * {@link IdentityGrpcMessageCompressor}.
	 * </p>
	 *
	 * @param <A>                     the exchange context type
	 * @param <B>                     the client exchange type
	 * @param <C>                     the gRPC response message type
	 * @param <D>                     the gRPC response type
	 * @param exchange                the client HTTP exchange
	 * @param defaultResponseInstance the default response message instance
	 *
	 * @return a new gRPC client response
	 * 
	 * @throws GrpcException if the message encoding specified in the response is not supported
	 */
	private <A extends ExchangeContext, B extends Exchange<A>, C extends Message, D extends GrpcResponse<C>> Mono<D> createResponse(B exchange, C defaultResponseInstance) throws GrpcException {
		return exchange.response().map(response -> {
			GrpcMessageCompressor messageCompressor = response.headers().get(GrpcHeaders.NAME_GRPC_MESSAGE_ENCODING)
				.map(value -> this.compressorService.getMessageCompressor(value).orElseThrow(() -> new GrpcException(GrpcStatus.INTERNAL, "Unsupported message encoding: " + value)))
				.or(() -> this.compressorService.getMessageCompressor(GrpcHeaders.VALUE_IDENTITY))
				.orElse(IdentityGrpcMessageCompressor.INSTANCE);

			GrpcMessageReader<C> messageReader = new GrpcMessageReader<>(defaultResponseInstance, this.extensionRegistry, this.netService, messageCompressor);
		
			return (D)new GenericGrpcResponse<>(response, messageReader, extensionRegistry);
		});
	}
	
	/**
	 * <p>
	 * Creates a gRPC message writer.
	 * </p>
	 *
	 * <p>
	 * It determines the compressor to use based on the {@link GrpcHeaders#NAME_GRPC_MESSAGE_ENCODING} header in the request, if none is specified, it falls back to the
	 * {@link IdentityGrpcMessageCompressor}.
	 * </p>
	 *
	 * @param <A>      the exchange context type
	 * @param <B>      the client exchange type
	 * @param <C>      the request message type
	 * @param exchange the client HTTP exchange
	 *
	 * @return a gRPC message writer
	 *
	 * @throws GrpcException if the specified message encoding is not supported
	 */
	private <A extends ExchangeContext, B extends Exchange<A>, C extends Message> GrpcMessageWriter<C> createMessageWriter(B exchange) throws GrpcException {
		GrpcMessageCompressor messageCompressor = exchange.request().headers().get(GrpcHeaders.NAME_GRPC_MESSAGE_ENCODING)
			.map(value -> this.compressorService.getMessageCompressor(value).orElseThrow(() -> new GrpcException(GrpcStatus.UNIMPLEMENTED, "Unsupported message encoding: " + value)))
			.or(() -> this.compressorService.getMessageCompressor(GrpcHeaders.VALUE_IDENTITY))
			.orElse(IdentityGrpcMessageCompressor.INSTANCE);
		
		return new GrpcMessageWriter<>(this.netService, messageCompressor);
	}
}
