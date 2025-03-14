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
import io.inverno.mod.grpc.base.GrpcHeaders;
import io.inverno.mod.grpc.base.GrpcInboundResponseMetadata;
import io.inverno.mod.http.base.InboundResponseHeaders;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Generic {@link GrpcInboundResponseMetadata} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public class GenericGrpcResponseMetadata extends AbstractGrpcMetadata<InboundResponseHeaders> implements GrpcInboundResponseMetadata {

	/**
	 * <p>
	 * Creates generic gRPC response metadata.
	 * </p>
	 * 
	 * @param headers           the HTTP response headers
	 * @param extensionRegistry the Protocol buffer extension registry
	 */
	public GenericGrpcResponseMetadata(InboundResponseHeaders headers, ExtensionRegistry extensionRegistry) {
		super(headers, extensionRegistry);
	}

	@Override
	public List<String> getAcceptMessageEncoding() {
		return this.headers.get(GrpcHeaders.NAME_GRPC_ACCEPT_MESSAGE_ENCODING)
			.map(value -> Arrays.stream(value.split(",")).map(String::trim).collect(Collectors.toList()))
			.orElse(List.of());
	}

	@Override
	public Optional<String> getMessageEncoding() {
		return this.headers.get(GrpcHeaders.NAME_GRPC_MESSAGE_ENCODING);
	}
}
