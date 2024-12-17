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
package io.inverno.mod.grpc.server.internal;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.net.NetService;
import io.inverno.mod.grpc.base.GrpcException;
import io.inverno.mod.grpc.base.GrpcHeaders;
import io.inverno.mod.grpc.base.GrpcMessageCompressor;
import io.inverno.mod.grpc.base.GrpcMessageCompressorService;
import io.inverno.mod.grpc.base.GrpcStatus;
import io.inverno.mod.grpc.base.internal.GrpcMessageReader;
import io.inverno.mod.grpc.base.internal.GrpcMessageWriter;
import io.inverno.mod.grpc.base.internal.IdentityGrpcMessageCompressor;
import io.inverno.mod.grpc.server.GrpcExchange;
import io.inverno.mod.grpc.server.GrpcExchangeHandler;
import io.inverno.mod.grpc.server.GrpcRequest;
import io.inverno.mod.grpc.server.GrpcResponse;
import io.inverno.mod.grpc.server.GrpcServer;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeHandler;
import io.netty.handler.codec.http2.Http2Error;
import java.util.function.Function;

/**
 * <p>
 * Generic {@link GrpcServer} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
@Bean( name = "grpcServer" )
public class GenericGrpcServer implements GrpcServer {
	
	/**
	 * The gRPC message compressor service.
	 */
	private final GrpcMessageCompressorService compressorService;
	/**
	 * The Protocol buffer extension registry.
	 */
	private final ExtensionRegistry extensionRegistry;
	/**
	 * The net service.
	 */
	private final NetService netService;
	
	/**
	 * <p>
	 * Creates a generic gRPC server.
	 * </p>
	 *
	 * @param compressorService the gRPC message compressor service
	 * @param extensionRegistry the Protocol buffer extension registry
	 * @param netService        the net service
	 */
	public GenericGrpcServer(GrpcMessageCompressorService compressorService, ExtensionRegistry extensionRegistry, NetService netService) {
		this.compressorService = compressorService;
		this.extensionRegistry = extensionRegistry;
		this.netService = netService;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <A extends ExchangeContext, B extends Exchange<A>, C extends Message, D extends Message, E extends GrpcExchange.Unary<A, C, D>> ExchangeHandler<A, B> unary(C defaultRequestInstance, D defaultResponseInstance, GrpcExchangeHandler<A, C, D, GrpcRequest.Unary<C>, GrpcResponse.Unary<D>, E> grpcExchangeHandler) {
		Function<B, E> exchangeFactory = exchange -> (E)new GenericGrpcExchange.GenericUnary<>(exchange, () -> this.createRequest(exchange, defaultRequestInstance), () -> this.createResponse(exchange, defaultRequestInstance));
		return new GrpcExchangeHandlerAdapter<>(grpcExchangeHandler, exchangeFactory, this::handleError);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <A extends ExchangeContext, B extends Exchange<A>, C extends Message, D extends Message, E extends GrpcExchange.ClientStreaming<A, C, D>> ExchangeHandler<A, B> clientStreaming(C defaultRequestInstance, D defaultResponseInstance, GrpcExchangeHandler<A, C, D, GrpcRequest.Streaming<C>, GrpcResponse.Unary<D>, E> grpcExchangeHandler) {
		Function<B, E> exchangeFactory = exchange -> (E)new GenericGrpcExchange.GenericClientStreaming<>(exchange, () -> this.createRequest(exchange, defaultRequestInstance), () -> this.createResponse(exchange, defaultRequestInstance));
		return new GrpcExchangeHandlerAdapter<>(grpcExchangeHandler, exchangeFactory, this::handleError);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <A extends ExchangeContext, B extends Exchange<A>, C extends Message, D extends Message, E extends GrpcExchange.ServerStreaming<A, C, D>> ExchangeHandler<A, B> serverStreaming(C defaultRequestInstance, D defaultResponseInstance, GrpcExchangeHandler<A, C, D, GrpcRequest.Unary<C>, GrpcResponse.Streaming<D>, E> grpcExchangeHandler) {
		Function<B, E> exchangeFactory = exchange -> (E)new GenericGrpcExchange.GenericServerStreaming<>(exchange, () -> this.createRequest(exchange, defaultRequestInstance), () -> this.createResponse(exchange, defaultRequestInstance));
		return new GrpcExchangeHandlerAdapter<>(grpcExchangeHandler, exchangeFactory, this::handleError);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <A extends ExchangeContext, B extends Exchange<A>, C extends Message, D extends Message, E extends GrpcExchange.BidirectionalStreaming<A, C, D>> ExchangeHandler<A, B> bidirectionalStreaming(C defaultRequestInstance, D defaultResponseInstance, GrpcExchangeHandler<A, C, D, GrpcRequest.Streaming<C>, GrpcResponse.Streaming<D>, E> grpcExchangeHandler) {
		Function<B, E> exchangeFactory = exchange -> (E)new GenericGrpcExchange.GenericBidirectionalStreaming<>(exchange, () -> this.createRequest(exchange, defaultRequestInstance), () -> this.createResponse(exchange, defaultRequestInstance));
		return new GrpcExchangeHandlerAdapter<>(grpcExchangeHandler, exchangeFactory, this::handleError);
	}
	
	@Override
	public <A extends ExchangeContext, B extends ErrorExchange<A>> ExchangeHandler<A, B> errorHandler() {
		return new GenericGrpcErrorExchangeHandler<>(this);
	}
	
	/**
	 * <p>
	 * Creates a gRPC server request.
	 * </p>
	 * 
	 * <p>
	 * It determines the compressor to use based on the {@link GrpcHeaders#NAME_GRPC_MESSAGE_ENCODING} header in the request, if none is specified, it falls back to the
	 * {@link IdentityGrpcMessageCompressor}.
	 * </p>
	 * 
	 * <p>
	 * In case the client sends a request with an unsupported encoding, the {@link GrpcHeaders#NAME_GRPC_ACCEPT_MESSAGE_ENCODING} header is set in the response with the list of supported message 
	 * encodings.
	 * </p>
	 * 
	 * @param <A>                    the exchange context type
	 * @param <B>                    the server exchange type
	 * @param <C>                    the gRPC request message type
	 * @param <D>                    the gRPC request type
	 * @param exchange               the server HTTP exchange
	 * @param defaultRequestInstance the default request message instance
	 * 
	 * @return a new gRPC server request
	 * 
	 * @throws GrpcException if the message encoding specified in the request is not supported
	 */
	@SuppressWarnings("unchecked")
	private <A extends ExchangeContext, B extends Exchange<A>, C extends Message, D extends GrpcRequest<C>> D createRequest(B exchange, C defaultRequestInstance) throws GrpcException {
		GrpcMessageCompressor messageCompressor = exchange.request().headers().get(GrpcHeaders.NAME_GRPC_MESSAGE_ENCODING)
			.map(value -> this.compressorService.getMessageCompressor(value).orElseThrow(() -> {
				exchange.response().headers(headers -> headers.set(GrpcHeaders.NAME_GRPC_ACCEPT_MESSAGE_ENCODING, String.join(",", this.compressorService.getMessageEncodings())));
				return new GrpcException(GrpcStatus.UNIMPLEMENTED, "Unsupported message encoding: " + value);
			}))
			.or(() -> this.compressorService.getMessageCompressor(GrpcHeaders.VALUE_IDENTITY))
			.orElse(IdentityGrpcMessageCompressor.INSTANCE);

		GrpcMessageReader<C> messageReader = new GrpcMessageReader<>(defaultRequestInstance, this.extensionRegistry, this.netService, messageCompressor);

		return (D)new GenericGrpcRequest<>(exchange.request(), messageReader, this.extensionRegistry);
	}
	
	/**
	 * <p>
	 * Creates a gRPC server response.
	 * </p>
	 * 
	 * @param <A>                     the exchange context type
	 * @param <B>                     the server exchange type
	 * @param <C>                     the gRPC response message type
	 * @param <D>                     the gRPC response type
	 * @param exchange                the server HTTP exchange
	 * @param defaultResponseInstance the default response message instance
	 * 
	 * @return a new gRPC server response
	 */
	@SuppressWarnings("unchecked")
	private <A extends ExchangeContext, B extends Exchange<A>, C extends Message, D extends GrpcResponse<C>> D createResponse(B exchange, C defaultResponseInstance) {
		return (D)new GenericGrpcResponse<>(exchange.response(), () -> this.createMessageWriter(exchange), this.extensionRegistry, () -> exchange.reset(Http2Error.CANCEL.code()));
	}
	
	/**
	 * <p>
	 * Creates a gRPC message writer.
	 * </p>
	 *
	 * <p>
	 * It determines the compressor to use based on the {@link GrpcHeaders#NAME_GRPC_MESSAGE_ENCODING} header in the request, if none is specified it selects the first supported message encoding
	 * listed in the {@link GrpcHeaders#NAME_GRPC_ACCEPT_MESSAGE_ENCODING} header. If no gRPC message compressor could be resolved it falls back to the
	 * {@link IdentityGrpcMessageCompressor}.
	 * </p>
	 * 
	 * @param <A>      the exchange context type
	 * @param <B>      the server exchange type
	 * @param <C>      the response message type
	 * @param exchange the server HTTP exchange
	 * 
	 * @return a gRPC message writer
	 * 
	 * throws GrpcException if the message encoding specified by the client is not supported
	 */
	private <A extends ExchangeContext, B extends Exchange<A>, C extends Message> GrpcMessageWriter<C> createMessageWriter(B exchange) throws GrpcException {
		// if message encoding has been specified in the response headers we must throw an error if this is not supported
		GrpcMessageCompressor messageCompressor = exchange.response().headers().get(GrpcHeaders.NAME_GRPC_MESSAGE_ENCODING)
			.map(value -> this.compressorService.getMessageCompressor(value).orElseThrow(() -> {
				exchange.response().headers(headers -> headers.set(GrpcHeaders.NAME_GRPC_ACCEPT_MESSAGE_ENCODING, String.join(",", this.compressorService.getMessageEncodings())));
				return new GrpcException(GrpcStatus.UNIMPLEMENTED, "Unsupported message encoding: " + value);
			}))
			.orElseGet(() -> {
				GrpcMessageCompressor c = exchange.request().headers().get(GrpcHeaders.NAME_GRPC_ACCEPT_MESSAGE_ENCODING)
					.flatMap(value -> this.compressorService.getMessageCompressor(value.split(",")))
					.or(() -> this.compressorService.getMessageCompressor(GrpcHeaders.VALUE_IDENTITY))
					.orElse(IdentityGrpcMessageCompressor.INSTANCE);
				
				exchange.response().headers(headers -> headers.set(GrpcHeaders.NAME_GRPC_MESSAGE_ENCODING, c.getMessageEncoding()));
				
				return c;
			});
		
		return new GrpcMessageWriter<>(this.netService, messageCompressor);
	}
	
	/**
	 * <p>
	 * Handles the specified error.
	 * </p>
	 *
	 * <p>
	 * This basically map the error to a gRPC status and message and set them in the HTTP response trailers and then terminates the exchange.
	 * </p>
	 * 
	 * <p>
	 * This is invoked by the {@link GrpcServer#errorHandler()} and by the {@link GrpcExchangeHandlerAdapter}.
	 * </p>
	 *
	 * @param <A>      the exchange context type
	 * @param <B>      the server exchange type
	 * @param exchange the server HTTP exchange
	 * @param error    the error
	 */
	public <A extends ExchangeContext, B extends Exchange<A>> void handleError(B exchange, Throwable error) {
		GenericGrpcExchange.LOGGER.error("gRPC exchange processing error", error);
		exchange.response()
			.headers(headers -> headers.status(Status.OK)) // just make sure we have 200... https://github.com/grpc/grpc/blob/master/doc/statuscodes.md
			.trailers(trailers -> {
				GrpcStatus grpcStatus;
				if(error instanceof GrpcException) {
					GrpcException grpcError = (GrpcException)error;
					grpcStatus = grpcError.getStatus();
				}
				else if(error instanceof HttpException) {
					// This is not supposed to happen, but we never know
					// https://github.com/grpc/grpc/blob/master/doc/http-grpc-status-mapping.md
					HttpException httpError = (HttpException)error;
					switch(httpError.getStatus()) {
						case BAD_REQUEST: grpcStatus = GrpcStatus.INTERNAL;
							break;
						case UNAUTHORIZED: grpcStatus = GrpcStatus.UNAUTHENTICATED;
							break;
						case FORBIDDEN: grpcStatus = GrpcStatus.PERMISSION_DENIED;
							break;
						case NOT_FOUND: grpcStatus = GrpcStatus.UNIMPLEMENTED;
							break;
						case TOO_MANY_REQUESTS:
						case BAD_GATEWAY:
						case SERVICE_UNAVAILABLE:
						case GATEWAY_TIMEOUT: grpcStatus = GrpcStatus.UNAVAILABLE;
							break;
						default: grpcStatus = GrpcStatus.UNKNOWN;
					}
				}
				else if(error instanceof IllegalArgumentException) {
					grpcStatus = GrpcStatus.INVALID_ARGUMENT;
				}
				else {
					grpcStatus = GrpcStatus.UNKNOWN;
				}
				trailers.set(GrpcHeaders.NAME_GRPC_STATUS, Integer.toString(grpcStatus.getCode()));
				if(error.getMessage() != null) {
					trailers.set(GrpcHeaders.NAME_GRPC_STATUS_MESSAGE, error.getMessage());
				}
			})
			.body().empty();
	}
}
