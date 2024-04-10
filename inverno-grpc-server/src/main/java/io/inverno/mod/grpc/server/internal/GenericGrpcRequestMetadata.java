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
import io.inverno.mod.grpc.base.GrpcInboundRequestMetadata;
import io.inverno.mod.http.base.InboundRequestHeaders;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Generic {@link GrpcInboundRequestMetadata} implementation
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public class GenericGrpcRequestMetadata extends AbstractGrpcMetadata<InboundRequestHeaders> implements GrpcInboundRequestMetadata {
	
	/**
	 * <p>
	 * Creates generic gRPC request metadata.
	 * </p>
	 * 
	 * @param headers           the HTTP request headers
	 * @param extensionRegistry the Protocol buffer extension registry
	 */
	public GenericGrpcRequestMetadata(InboundRequestHeaders headers, ExtensionRegistry extensionRegistry) {
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

	@Override
	public Optional<String> getMessageType() {
		return this.headers.get(GrpcHeaders.NAME_GRPC_MESSAGE_TYPE);
	}

	@Override
	public Optional<Duration> getTimeout() {
		return this.headers.get(GrpcHeaders.NAME_GRPC_TIMEOUT)
			.map(value -> {
				try {
					int timeoutValue = Integer.parseInt(value.substring(0, value.length() - 1));
					String timeoutUnit = value.substring(value.length() - 1);
					switch(timeoutUnit) {
						case "H": return Duration.of(timeoutValue, ChronoUnit.HOURS);
						case "M": return Duration.of(timeoutValue, ChronoUnit.MINUTES);
						case "S": return Duration.of(timeoutValue, ChronoUnit.SECONDS);
						case "m": return Duration.of(timeoutValue, ChronoUnit.MILLIS);
						case "u": return Duration.of(timeoutValue, ChronoUnit.MICROS);
						case "n": return Duration.of(timeoutValue, ChronoUnit.NANOS);
						default: throw new IllegalArgumentException("Unsupported unit: " + timeoutUnit);
					}
				}
				catch(NumberFormatException e) {
					throw new IllegalArgumentException(e);
				}
			});
	}
}
