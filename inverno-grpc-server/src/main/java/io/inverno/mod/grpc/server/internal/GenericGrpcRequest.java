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
import io.inverno.mod.grpc.base.GrpcException;
import io.inverno.mod.grpc.base.GrpcInboundRequestMetadata;
import io.inverno.mod.grpc.base.GrpcServiceName;
import io.inverno.mod.grpc.base.GrpcStatus;
import io.inverno.mod.grpc.base.internal.GrpcMessageReader;
import io.inverno.mod.grpc.server.GrpcRequest;
import io.inverno.mod.http.server.Request;
import io.inverno.mod.http.server.ResetStreamException;
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
public class GenericGrpcRequest<A extends Message> implements GrpcRequest.Unary<A>, GrpcRequest.Streaming<A> {

	/**
	 * The underlying server HTTP request.
	 */
	private final Request request;
	/**
	 * The gRPC message reader.
	 */
	private final GrpcMessageReader<A> messageReader;
	/**
	 * The Protocol buffer extension registry.
	 */
	private final ExtensionRegistry extensionRegistry;
	
	/**
	 * The gRPC service name.
	 */
	private GrpcServiceName serviceName;
	/**
	 * The gRPC service method name.
	 */
	private String methodName;
	/**
	 * The gRPC full method name.
	 */
	private String fullMethodName;
	/**
	 * The gRPC request metadata.
	 */
	private GrpcInboundRequestMetadata metadata;

	/**
	 * <p>
	 * Creates a generic gRPC server request.
	 * </p>
	 *
	 * @param request           the server HTTP request
	 * @param messageReader     the gRPC message reader
	 * @param extensionRegistry the Protocol buffer extension registry
	 */
	public GenericGrpcRequest(Request request, GrpcMessageReader<A> messageReader, ExtensionRegistry extensionRegistry) {
		this.request = request;
		this.messageReader = messageReader;
		this.extensionRegistry = extensionRegistry;
	}

	@Override
	public GrpcServiceName getServiceName() {
		if(this.serviceName == null) {
			String path = this.request.getPath();
			int methodIndex = path.lastIndexOf("/");
			return GrpcServiceName.of(path.substring(1, methodIndex));
		}
		return this.serviceName;
	}
	
	@Override
	public String getMethodName() {
		if(this.methodName == null) {
			String path = this.request.getPath();
			int methodIndex = path.lastIndexOf("/");
			return path.substring(methodIndex + 1);
		}
		return this.methodName;
	}

	@Override
	public String getFullMethodName() {
		if(this.fullMethodName == null) {
			this.fullMethodName = this.request.getPath().substring(1);
		}
		return this.fullMethodName;
	}

	@Override
	public GrpcInboundRequestMetadata metadata() {
		if(this.metadata == null) {
			this.metadata = new GenericGrpcRequestMetadata(this.request.headers(), this.extensionRegistry);
		}
		return this.metadata;
	}

	@Override
	public Mono<A> value() {
		return Mono.from(this.stream());
	}
	
	@Override
	public Publisher<A> stream() {
		// When disposing the exchange errors are actually propagated to the request body sink so we can do that
		return Flux.from(this.messageReader.apply(this.request.body().get().raw().stream()))
			.onErrorMap(ResetStreamException.class, e -> {
				GrpcStatus status = GrpcStatus.fromHttp2Code(((ResetStreamException)e).getErrorCode());
				if(status != null) {
					if(status == GrpcStatus.CANCELLED) {
						return new GrpcException(status, "Request was cancelled", e);
					}
					return new GrpcException(status, e);
				}
				return e;
			})
			.doOnError(t -> GenericGrpcExchange.LOGGER.error("gRPC request processing error", t));
	}
}
