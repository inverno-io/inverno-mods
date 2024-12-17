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
import io.inverno.mod.grpc.base.GrpcInboundResponseMetadata;
import io.inverno.mod.grpc.base.GrpcInboundResponseTrailersMetadata;
import io.inverno.mod.grpc.base.GrpcStatus;
import io.inverno.mod.grpc.base.internal.GrpcMessageReader;
import io.inverno.mod.grpc.client.GrpcResponse;
import io.inverno.mod.http.client.ResetStreamException;
import io.inverno.mod.http.client.Response;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link GrpcResponse} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public class GenericGrpcResponse<A extends Message> implements GrpcResponse.Unary<A>, GrpcResponse.Streaming<A> {

	/**
	 * The underlying client HTTP response.
	 */
	private final Response response;
	/**
	 * The gRPC message reader.
	 */
	private final GrpcMessageReader<A> messageReader;
	/**
	 * The Protocol buffer extension registry.
	 */
	private final ExtensionRegistry extensionRegistry;
	
	/**
	 * The gRPC response metadata.
	 */
	private GenericGrpcResponseMetadata metadata;
	/**
	 * The gRPC response trailers metadata.
	 */
	private GenericGrpcResponseTrailersMetadata trailersMetadata;

	/**
	 * <p>
	 * Creates a generic gRPC response.
	 * </p>
	 *
	 * @param response          the client HTTP response
	 * @param messageReader     the gRPC message reader
	 * @param extensionRegistry the Protocol buffer extension registry
	 */
	public GenericGrpcResponse(Response response, GrpcMessageReader<A> messageReader, ExtensionRegistry extensionRegistry) {
		this.response = response;
		this.messageReader = messageReader;
		this.extensionRegistry = extensionRegistry;
	}
	
	@Override
	public GrpcInboundResponseMetadata metadata() {
		if(this.metadata == null) {
			this.metadata = new GenericGrpcResponseMetadata(this.response.headers(), this.extensionRegistry);
		}
		return this.metadata;
	}

	@Override
	public GrpcInboundResponseTrailersMetadata trailersMetadata() {
		if(this.trailersMetadata == null && this.response.trailers() != null) {
			this.trailersMetadata = new GenericGrpcResponseTrailersMetadata(this.response.trailers(), this.extensionRegistry);
		}
		return this.trailersMetadata;
	}

	@Override
	public Mono<A> value() {
		return Mono.from(this.stream());
	}
	
	@Override
	public Publisher<A> stream() {
		return Flux.from(this.messageReader.apply(this.response.body().raw().stream()))
			.onErrorMap(ResetStreamException.class, e -> {
				GrpcStatus status = GrpcStatus.fromHttp2Code(e.getErrorCode());
				if(status != null) {
					return new GrpcException(status, e);
				}
				return e;
			})
			.doOnError(t -> GenericGrpcExchange.LOGGER.error("gRPC response processing error", t));
	}
}
