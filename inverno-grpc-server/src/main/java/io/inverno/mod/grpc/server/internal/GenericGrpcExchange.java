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

import com.google.protobuf.Message;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.grpc.base.GrpcException;
import io.inverno.mod.grpc.base.GrpcHeaders;
import io.inverno.mod.grpc.base.GrpcStatus;
import io.inverno.mod.grpc.server.GrpcExchange;
import io.inverno.mod.grpc.server.GrpcRequest;
import io.inverno.mod.grpc.server.GrpcResponse;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ResetStreamException;
import io.netty.handler.codec.http2.Http2Error;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>
 * Generic {@link GrpcExchange} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 * 
 * @param <A> The exchange context type 
 * @param <B> The request message type
 * @param <C> The response message type
 * @param <D> the request type
 * @param <E> the response type
 */
public class GenericGrpcExchange<A extends ExchangeContext, B extends Message, C extends Message, D extends GrpcRequest<B>, E extends GrpcResponse<C>> implements GrpcExchange<A, B, C, D, E> {
	
	public static final Logger LOGGER = LogManager.getLogger(GenericGrpcExchange.class);

	/**
	 * The underlying HTTP server exchange.
	 */
	private final Exchange<A> exchange;
	/**
	 * The gRPC request supplier.
	 */
	private final Supplier<D> requestSupplier;
	/**
	 * The gRPC response supplier.
	 */
	private final Supplier<E> responseSupplier;
	
	/**
	 * The gRPC request.
	 */
	private D request;
	/**
	 * The gRPC response.
	 */
	private E response;
	
	/**
	 * The cancel cause.
	 */
	private Optional<GrpcException> cancelCause;
	
	/**
	 * <p>
	 * Creates a generic gRPC exchange.
	 * </p>
	 *
	 * @param exchange         the HTTP server exchange
	 * @param requestSupplier  the gRPC request supplier
	 * @param responseSupplier the gRPC response supplier
	 */
	public GenericGrpcExchange(Exchange<A> exchange, Supplier<D> requestSupplier, Supplier<E> responseSupplier) {
		this.exchange = exchange;
		this.requestSupplier = requestSupplier;
		this.responseSupplier = responseSupplier;
		
		this.exchange.response().headers(headers -> headers.status(Status.OK).contentType(MediaTypes.APPLICATION_GRPC_PROTO));
		this.exchange.response().trailers(trailers -> trailers.set(GrpcHeaders.NAME_GRPC_STATUS, Integer.toString(GrpcStatus.OK.getCode())));
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
	public E response() {
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
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
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
		 * @param exchange         the HTTP server exchange
		 * @param requestSupplier  the gRPC request supplier
		 * @param responseSupplier the gRPC response supplier
		 */
		public GenericUnary(Exchange<A> exchange, Supplier<GrpcRequest.Unary<B>> requestSupplier, Supplier<GrpcResponse.Unary<C>> responseSupplier) {
			super(exchange, requestSupplier, responseSupplier);
		}
	}
	
	/**
	 * <p>
	 * Generic {@link GrpcExchange.ClientStreaming} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
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
		 * @param exchange         the HTTP server exchange
		 * @param requestSupplier  the gRPC request supplier
		 * @param responseSupplier the gRPC response supplier
		 */
		public GenericClientStreaming(Exchange<A> exchange, Supplier<GrpcRequest.Streaming<B>> requestSupplier, Supplier<GrpcResponse.Unary<C>> responseSupplier) {
			super(exchange, requestSupplier, responseSupplier);
		}
	}
	
	/**
	 * <p>
	 * Generic {@link GrpcExchange.ServerStreaming} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
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
		 * @param exchange         the HTTP server exchange
		 * @param requestSupplier  the gRPC request supplier
		 * @param responseSupplier the gRPC response supplier
		 */
		public GenericServerStreaming(Exchange<A> exchange, Supplier<GrpcRequest.Unary<B>> requestSupplier, Supplier<GrpcResponse.Streaming<C>> responseSupplier) {
			super(exchange, requestSupplier, responseSupplier);
		}
	}
	
	/**
	 * <p>
	 * Generic {@link GrpcExchange.BidirectionalStreaming} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
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
		 * @param exchange         the HTTP server exchange
		 * @param requestSupplier  the gRPC request supplier
		 * @param responseSupplier the gRPC response supplier
		 */
		public GenericBidirectionalStreaming(Exchange<A> exchange, Supplier<GrpcRequest.Streaming<B>> requestSupplier, Supplier<GrpcResponse.Streaming<C>> responseSupplier) {
			super(exchange, requestSupplier, responseSupplier);
		}
	}
}
