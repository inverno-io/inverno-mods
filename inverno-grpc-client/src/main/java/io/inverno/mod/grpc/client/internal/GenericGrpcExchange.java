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

import com.google.protobuf.Message;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.grpc.base.GrpcException;
import io.inverno.mod.grpc.base.GrpcServiceName;
import io.inverno.mod.grpc.base.GrpcStatus;
import io.inverno.mod.grpc.client.GrpcExchange;
import io.inverno.mod.grpc.client.GrpcRequest;
import io.inverno.mod.grpc.client.GrpcResponse;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.ResetStreamException;
import io.netty.handler.codec.http2.Http2Error;
import java.util.Optional;
import java.util.function.Supplier;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link GrpcExchange} implementation.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public class GenericGrpcExchange<A extends ExchangeContext, B extends Message, C extends Message, D extends GrpcRequest<B>, E extends GrpcResponse<C>> implements GrpcExchange<A, B, C, D, E> {

	/**
	 * The underlying client HTTP exchange.
	 */
	private final Exchange<A> exchange;
	/**
	 * The request supplier.
	 */
	private final Supplier<D> requestSupplier;
	/**
	 * The response supplier.
	 */
	private final Supplier<Mono<? extends E>> responseSupplier;
	
	/**
	 * The gRPC request.
	 */
	private D request;
	/**
	 * The gRPC response mono.
	 */
	private Mono<? extends E> response;
	
	/**
	 * The cancel cause.
	 */
	private Optional<GrpcException> cancelCause;

	/**
	 * <p>
	 * Creates a generic gRPC exchange
	 * </p>
	 *
	 * @param exchange         the client HTTP exchange
	 * @param requestSupplier  the gRPC request supplier
	 * @param responseSupplier the gRPC response supplier
	 * @param serviceName      the gRPC service name
	 * @param methodName       the gRPC service method name
	 */
	public GenericGrpcExchange(Exchange<A> exchange, Supplier<D> requestSupplier, Supplier<Mono<? extends E>> responseSupplier, GrpcServiceName serviceName, String methodName) {
		this.exchange = exchange;
		this.requestSupplier = requestSupplier;
		this.responseSupplier = responseSupplier;
		
		this.exchange.request().method(Method.POST).path(serviceName.methodPath(methodName)).headers(headers -> headers.contentType(MediaTypes.APPLICATION_GRPC_PROTO));
	}
	
	@Override
	public A context() {
		return this.exchange.context();
	}

	@Override
	public D request() {
		if(this.request == null) {
			this.request = this.requestSupplier.get();
		}
		return this.request;
	}

	@Override
	public Mono<? extends E> response() {
		if(this.response == null) {
			this.response = this.responseSupplier.get();
		}
		return this.response;
	}

	@Override
	public void cancel() {
		this.exchange.reset(Http2Error.CANCEL.code());
	}

	@Override
	public Optional<GrpcException> getCancelCause() {
		if(this.cancelCause == null || this.cancelCause.isEmpty()) {
			this.cancelCause = this.exchange.getCancelCause().map(e -> {
				if(e instanceof GrpcException) {
					return (GrpcException)e;
				}
				else if(e instanceof ResetStreamException && ((ResetStreamException)e).getErrorCode() == Http2Error.CANCEL.code()) {
					return new GrpcException(GrpcStatus.CANCELLED, e);
				}
				return new GrpcException(GrpcStatus.UNKNOWN, e);
			});
		}
		return this.cancelCause;
	}

	/**
	 * <p>
	 * Generic {@link GrpcExchange.Unary} implementation.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.9
	 * 
	 * @param <A> the exchange context type
	 * @param <B> the request message type
	 * @param <C> the response message type
	 */
	public static class GenericUnary<A extends ExchangeContext, B extends Message, C extends Message> extends GenericGrpcExchange<A, B, C, GrpcRequest.Unary<B>, GrpcResponse.Unary<C>> implements GrpcExchange.Unary<A,B,C> {

		/**
		 * <p>
		 * Creates a generic unary gRPC exchange.
		 * </p>
		 *
		 * @param exchange         the client HTTP exchange
		 * @param requestSupplier  the gRPC request supplier
		 * @param responseSupplier the gRPC response supplier
		 * @param serviceName      the gRPC service name
		 * @param methodName       the gRPC service method name
		 */
		public GenericUnary(Exchange<A> exchange, Supplier<GrpcRequest.Unary<B>> requestSupplier, Supplier<Mono<? extends GrpcResponse.Unary<C>>> responseSupplier, GrpcServiceName serviceName, String methodName) {
			super(exchange, requestSupplier, responseSupplier, serviceName, methodName);
		}
	}
	
	/**
	 * <p>
	 * Generic {@link GrpcExchange.ClientStreaming} implementation.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.9
	 * 
	 * @param <A> the exchange context type
	 * @param <B> the request message type
	 * @param <C> the response message type
	 */
	public static class GenericClientStreaming<A extends ExchangeContext, B extends Message, C extends Message> extends GenericGrpcExchange<A, B, C, GrpcRequest.Streaming<B>, GrpcResponse.Unary<C>> implements GrpcExchange.ClientStreaming<A, B, C> {

		/**
		 * <p>
		 * Creates a generic client streaming gRPC exchange.
		 * </p>
		 *
		 * @param exchange         the client HTTP exchange
		 * @param requestSupplier  the gRPC request supplier
		 * @param responseSupplier the gRPC response supplier
		 * @param serviceName      the gRPC service name
		 * @param methodName       the gRPC service method name
		 */
		public GenericClientStreaming(Exchange<A> exchange, Supplier<GrpcRequest.Streaming<B>> requestSupplier, Supplier<Mono<? extends GrpcResponse.Unary<C>>> responseSupplier, GrpcServiceName serviceName, String methodName) {
			super(exchange, requestSupplier, responseSupplier, serviceName, methodName);
		}
	}
	
	/**
	 * <p>
	 * Generic {@link GrpcExchange.ServerStreaming} implementation.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.9
	 * 
	 * @param <A> the exchange context type
	 * @param <B> the request message type
	 * @param <C> the response message type
	 */
	public static class GenericServerStreaming<A extends ExchangeContext, B extends Message, C extends Message> extends GenericGrpcExchange<A, B, C, GrpcRequest.Unary<B>, GrpcResponse.Streaming<C>> implements GrpcExchange.ServerStreaming<A, B, C> {

		/**
		 * <p>
		 * Creates a generic server streaming gRPC exchange.
		 * </p>
		 *
		 * @param exchange         the client HTTP exchange
		 * @param requestSupplier  the gRPC request supplier
		 * @param responseSupplier the gRPC response supplier
		 * @param serviceName      the gRPC service name
		 * @param methodName       the gRPC service method name
		 */
		public GenericServerStreaming(Exchange<A> exchange, Supplier<GrpcRequest.Unary<B>> requestSupplier, Supplier<Mono<? extends GrpcResponse.Streaming<C>>> responseSupplier, GrpcServiceName serviceName, String methodName) {
			super(exchange, requestSupplier, responseSupplier, serviceName, methodName);
		}
	}
	
	/**
	 * <p>
	 * Generic {@link GrpcExchange.BidirectionalStreaming} implementation.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.9
	 * 
	 * @param <A> the exchange context type
	 * @param <B> the request message type
	 * @param <C> the response message type
	 */
	public static class GenericBidirectionalStreaming<A extends ExchangeContext, B extends Message, C extends Message> extends GenericGrpcExchange<A, B, C, GrpcRequest.Streaming<B>, GrpcResponse.Streaming<C>> implements GrpcExchange.BidirectionalStreaming<A, B, C> {

		/**
		 * <p>
		 * Creates a generic bidirectional streaming gRPC exchange.
		 * </p>
		 *
		 * @param exchange         the client HTTP exchange
		 * @param requestSupplier  the gRPC request supplier
		 * @param responseSupplier the gRPC response supplier
		 * @param serviceName      the gRPC service name
		 * @param methodName       the gRPC service method name
		 */
		public GenericBidirectionalStreaming(Exchange<A> exchange, Supplier<GrpcRequest.Streaming<B>> requestSupplier, Supplier<Mono<? extends GrpcResponse.Streaming<C>>> responseSupplier, GrpcServiceName serviceName, String methodName) {
			super(exchange, requestSupplier, responseSupplier, serviceName, methodName);
		}
	}
}
