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
import io.inverno.mod.grpc.base.GrpcHeaders;
import io.inverno.mod.grpc.base.GrpcStatus;
import io.inverno.mod.http.base.OutboundHeaders;
import java.util.Optional;
import io.inverno.mod.grpc.base.GrpcOutboundResponseTrailersMetadata;

/**
 * <p>
 * Generic {@link GrpcOutboundResponseTrailersMetadata} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public class GenericGrpcResponseTrailersMetadata extends AbstractGrpcResponseMetadata<GrpcOutboundResponseTrailersMetadata> implements GrpcOutboundResponseTrailersMetadata {

	/**
	 * <p>
	 * Creates generic gRPC response trailers metadata.
	 * </p>
	 * 
	 * @param trailers          the HTTP response trailers
	 * @param extensionRegistry the Protobuf extension registry
	 */
	public GenericGrpcResponseTrailersMetadata(OutboundHeaders<?> trailers, ExtensionRegistry extensionRegistry) {
		super(trailers, extensionRegistry);
	}

	@Override
	public GrpcStatus getStatus() throws IllegalArgumentException {
		return this.headers.get(GrpcHeaders.NAME_GRPC_STATUS).map(value -> GrpcStatus.valueOf(Integer.parseInt(value))).orElse(GrpcStatus.OK);
	}

	@Override
	public GrpcOutboundResponseTrailersMetadata status(GrpcStatus status) {
		this.headers.set(GrpcHeaders.NAME_GRPC_STATUS, Integer.toString(status.getCode()));
		return this;
	}
	
	@Override
	public int getStatusCode() {
		return this.headers.get(GrpcHeaders.NAME_GRPC_STATUS).map(Integer::valueOf).orElse(GrpcStatus.OK.getCode());
	}

	@Override
	public GrpcOutboundResponseTrailersMetadata status(int statusCode) {
		this.headers.set(GrpcHeaders.NAME_GRPC_STATUS, Integer.toString(statusCode));
		return this;
	}
	
	@Override
	public Optional<String> getStatusMessage() {
		return this.headers.get(GrpcHeaders.NAME_GRPC_STATUS_MESSAGE);
	}
	
	@Override
	public GrpcOutboundResponseTrailersMetadata statusMessage(String message) {
		this.headers.set(GrpcHeaders.NAME_GRPC_STATUS_MESSAGE, message);
		return this;
	}

	
}
