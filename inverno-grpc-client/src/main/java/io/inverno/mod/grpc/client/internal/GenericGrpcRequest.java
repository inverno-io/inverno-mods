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
import io.inverno.mod.grpc.base.GrpcException;
import io.inverno.mod.grpc.base.GrpcInboundRequestMetadata;
import io.inverno.mod.grpc.base.GrpcOutboundRequestMetadata;
import io.inverno.mod.grpc.base.GrpcServiceName;
import io.inverno.mod.grpc.base.GrpcStatus;
import io.inverno.mod.grpc.base.internal.GrpcMessageWriter;
import io.inverno.mod.grpc.client.GrpcRequest;
import io.inverno.mod.http.client.Request;
import io.netty.buffer.Unpooled;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link GrpcRequest} implementation.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public class GenericGrpcRequest<A extends Message> implements GrpcRequest.Unary<A>, GrpcRequest.Streaming<A>  {

	/**
	 * The underlying client HTTP request.
	 */
	private final Request request;
	/**
	 * The gRPC message writer supplier.
	 */
	private final Supplier<GrpcMessageWriter<A>> messageWriterSupplier;
	/**
	 * THe Protocol buffer extension registry.
	 */
	private final ExtensionRegistry extensionRegistry;
	/**
	 * The gRPC service name.
	 */
	private final GrpcServiceName serviceName;
	/**
	 * The gRPC service method name.
	 */
	private final String methodName;
	/**
	 * The cancel exchange runnable.
	 */
	private final Runnable cancelExchange;
	
	/**
	 * The gRPC request metadata.
	 */
	private GenericGrpcRequestMetadata metadata;

	/**
	 * <p>
	 * Creates a generic gRPC client request.
	 * </p>
	 *
	 * @param request               the client HTTP request
	 * @param messageWriterSupplier the gRPC message writer supplier
	 * @param extensionRegistry     the Protocol buffer extension registry
	 * @param serviceName           the gRPC service name
	 * @param methodName            the gRPC service method name
	 * @param canceExchange         the cancel exchange function
	 */
	public GenericGrpcRequest(Request request, Supplier<GrpcMessageWriter<A>> messageWriterSupplier, ExtensionRegistry extensionRegistry, GrpcServiceName serviceName, String methodName, Runnable cancelExchange) {
		this.request = request;
		this.messageWriterSupplier = messageWriterSupplier;
		this.extensionRegistry = extensionRegistry;
		this.serviceName = serviceName;
		this.methodName = methodName;
		this.cancelExchange = cancelExchange;
	}
	
	@Override
	public GrpcServiceName getServiceName() {
		return this.serviceName;
	}

	@Override
	public String getMethodName() {
		return this.methodName;
	}

	@Override
	public String getFullMethodName() {
		return this.serviceName.getFullyQualifiedName() + "/" + this.methodName;
	}
	
	@Override
	public GenericGrpcRequest<A> metadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) throws IllegalStateException {
		if(metadataConfigurer != null) {
			metadataConfigurer.accept((GrpcOutboundRequestMetadata)this.metadata());
		}
		return this;
	}

	@Override
	public GrpcInboundRequestMetadata metadata() {
		if(this.metadata == null) {
			this.request.headers(headers -> {
				this.metadata = new GenericGrpcRequestMetadata(headers, this.extensionRegistry);
			});
		}
		return this.metadata;
	}
	
	@Override
	public void value(Mono<A> value) {
		this.stream(value);
	}

	@Override
	public <T extends A> void stream(Publisher<T> value) throws IllegalStateException {
		this.request.body().get().raw().stream(
			Flux.concat(
				Mono.just(Unpooled.EMPTY_BUFFER), // Make sure we have a flux here (client buffer first chunk to be able to provide a content-length in case of single publisher)
				Flux.from(value).transformDeferred(data -> this.messageWriterSupplier.get().apply((Publisher<A>)data))
			)
			.doOnError(Throwable.class, error -> {
				if(error instanceof GrpcException) {
					GrpcException grpcError = (GrpcException)error;
					if(grpcError.getStatusCode() == GrpcStatus.CANCELLED.getCode()) {
						this.cancelExchange.run();
					}
				}
			})
		);
	}
}
