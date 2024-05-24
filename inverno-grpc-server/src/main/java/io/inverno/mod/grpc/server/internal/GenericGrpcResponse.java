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
import io.inverno.mod.grpc.base.GrpcInboundResponseMetadata;
import io.inverno.mod.grpc.base.GrpcInboundResponseTrailersMetadata;
import io.inverno.mod.grpc.base.GrpcOutboundResponseMetadata;
import io.inverno.mod.grpc.base.internal.GrpcMessageWriter;
import io.inverno.mod.grpc.server.GrpcResponse;
import io.inverno.mod.http.server.Response;
import java.util.function.Consumer;
import org.reactivestreams.Publisher;
import io.inverno.mod.grpc.base.GrpcOutboundResponseTrailersMetadata;
import io.inverno.mod.grpc.base.GrpcStatus;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.function.Supplier;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

/**
 * <p>
 * Generic {@link GrpcResponse} implementation.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public class GenericGrpcResponse<A extends Message> implements GrpcResponse.Unary<A>, GrpcResponse.Streaming<A> {
	
	/**
	 * The underlying server HTTP response.
	 */
	private final Response response;
	/**
	 * The gRPC message writer supplier.
	 */
	private final Supplier<GrpcMessageWriter<A>> messageWriterSupplier;
	/**
	 * The Protocol buffer extension registry.
	 */
	private final ExtensionRegistry extensionRegistry;
	/**
	 * The cancel exchange runnable.
	 */
	private final Runnable cancelExchange;
	
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
	 * @param response              the server HTTP response
	 * @param messageWriterSupplier the gRPC message writer supplier
	 * @param extensionRegistry     the Protocol buffer extension registry
	 * @param canceExchange         the cancel exchange function
	 */
	public GenericGrpcResponse(Response response, Supplier<GrpcMessageWriter<A>> messageWriterSupplier, ExtensionRegistry extensionRegistry, Runnable cancelExchange) {
		this.response = response;
		this.messageWriterSupplier = messageWriterSupplier;
		this.extensionRegistry = extensionRegistry;
		this.cancelExchange = cancelExchange;
	}
	
	@Override
	public GenericGrpcResponse<A> metadata(Consumer<GrpcOutboundResponseMetadata> metadataConfigurer) throws IllegalStateException {
		if(metadataConfigurer != null) {
			if(this.metadata == null) {
				this.response.headers(headers -> {
					this.metadata = new GenericGrpcResponseMetadata(headers, this.extensionRegistry);
				});
			}
			metadataConfigurer.accept(this.metadata);
		}
		return this;
	}

	@Override
	public GenericGrpcResponse<A> trailersMetadata(Consumer<GrpcOutboundResponseTrailersMetadata> metadataConfigurer) throws IllegalStateException {
		if(metadataConfigurer != null) {
			if(this.trailersMetadata == null) {
				this.response.trailers(trailers -> {
					this.trailersMetadata = new GenericGrpcResponseTrailersMetadata(trailers, this.extensionRegistry);
				});
			}
			metadataConfigurer.accept(this.trailersMetadata);
		}
		return this;
	}

	@Override
	public GrpcInboundResponseMetadata metadata() {
		if(this.metadata == null) {
			this.response.headers(headers -> {
				this.metadata = new GenericGrpcResponseMetadata(headers, this.extensionRegistry);
			});
		}
		return this.metadata;
	}

	@Override
	public GrpcInboundResponseTrailersMetadata trailersMetadata() {
		if(this.trailersMetadata == null) {
			this.response.trailers(trailers -> {
				this.trailersMetadata = new GenericGrpcResponseTrailersMetadata(trailers, this.extensionRegistry);
			});
		}
		return this.trailersMetadata;
	}

	@Override
	public void value(Mono<A> value) {
		this.stream(value);
	}
	
	@Override
	public <T extends A> void stream(Publisher<T> value) throws IllegalStateException {
		this.response.body().raw().stream(Flux.concat(
			Mono.just(Unpooled.EMPTY_BUFFER),
			Flux.from(value).transformDeferred(data -> this.messageWriterSupplier.get().apply((Publisher<A>)data))
				.onErrorResume(t -> {
					GenericGrpcExchange.LOGGER.error("gRPC response processing error", t);
					GrpcStatus status;
					if(t instanceof GrpcException) {
						status = ((GrpcException)t).getStatus();
					}
					else {
						status = GrpcStatus.UNKNOWN;
					}
					
					this.trailersMetadata(trailers -> {
						trailers.status(status);
						if(t.getMessage() != null) {
							trailers.statusMessage(t.getMessage());
						}
					});
					
					if(status == GrpcStatus.CANCELLED) {
						return Mono.<ByteBuf>empty().doFinally(sig -> {
							if(sig == SignalType.ON_COMPLETE) {
								this.cancelExchange.run();
							}
						});
					}
					return Mono.empty();
				})
		));
	}
}
