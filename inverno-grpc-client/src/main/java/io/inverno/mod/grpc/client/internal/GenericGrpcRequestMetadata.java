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
import com.google.protobuf.MessageLite;
import io.inverno.mod.grpc.base.GrpcHeaders;
import io.inverno.mod.grpc.base.GrpcOutboundRequestMetadata;
import io.inverno.mod.http.base.OutboundRequestHeaders;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Generic {@link GrpcOutboundRequestMetadata} implementation.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public class GenericGrpcRequestMetadata extends AbstractGrpcMetadata<OutboundRequestHeaders> implements GrpcOutboundRequestMetadata {
	
	/**
	 * The gRPC timeout.
	 */
	private Duration timeout;
	
	/**
	 * <p>
	 * Creates generic gRPC request metadata.
	 * </p>
	 *
	 * @param headers           the HTTP request headers
	 * @param extensionRegistry the Protocol buffer extension registry
	 */
	public GenericGrpcRequestMetadata(OutboundRequestHeaders headers, ExtensionRegistry extensionRegistry) {
		super(headers, extensionRegistry);
	}

	@Override
	public boolean isWritten() {
		return this.headers.isWritten();
	}

	@Override
	public GenericGrpcRequestMetadata add(CharSequence name, CharSequence value) {
		this.headers.add(name, value);
		return this;
	}

	@Override
	public GenericGrpcRequestMetadata set(CharSequence name, CharSequence value) {
		this.headers.set(name, value);
		return this;
	}
	
	@Override
	public GenericGrpcRequestMetadata remove(CharSequence... names) {
		this.headers.remove(names);
		return this;
	}

	@Override
	public <T extends MessageLite> GenericGrpcRequestMetadata addBinary(CharSequence name, T value) {
		this.headers.add(name + "-bin", Base64.getEncoder().encodeToString(value.toByteArray()));
		return this;
	}

	@Override
	public <T extends MessageLite> GenericGrpcRequestMetadata setBinary(CharSequence name, T value) {
		this.headers.set(name + "-bin", Base64.getEncoder().encodeToString(value.toByteArray()));
		return this;
	}

	@Override
	public GenericGrpcRequestMetadata removeBinary(CharSequence... names) {
		for(CharSequence name : names) {
			this.headers.remove(name + "-bin");
		}
		return this;
	}

	@Override
	public List<String> getAcceptMessageEncoding() {
		return this.headers.get(GrpcHeaders.NAME_GRPC_ACCEPT_MESSAGE_ENCODING)
			.map(value -> Arrays.stream(value.split(",")).map(String::trim).collect(Collectors.toList()))
			.orElse(List.of());
	}
	
	@Override
	public GrpcOutboundRequestMetadata acceptMessageEncoding(List<String> messageEncodings) {
		this.headers.set(GrpcHeaders.NAME_GRPC_ACCEPT_MESSAGE_ENCODING, messageEncodings.stream().collect(Collectors.joining(",")));
		return this;
	}

	@Override
	public Optional<String> getMessageEncoding() {
		return this.headers.get(GrpcHeaders.NAME_GRPC_MESSAGE_ENCODING);
	}
	
	@Override
	public GrpcOutboundRequestMetadata messageEncoding(String messageEncoding) {
		this.headers.set(GrpcHeaders.NAME_GRPC_MESSAGE_ENCODING, messageEncoding);
		return this;
	}

	@Override
	public Optional<String> getMessageType() {
		return this.headers.get(GrpcHeaders.NAME_GRPC_MESSAGE_TYPE);
	}
	
	@Override
	public GrpcOutboundRequestMetadata messageType(String messageType) {
		this.headers.set(GrpcHeaders.NAME_GRPC_MESSAGE_TYPE, messageType);
		return this;
	}
	
	@Override
	public Optional<Duration> getTimeout() {
		return Optional.ofNullable(this.timeout);
	}
	
	@Override
	public GrpcOutboundRequestMetadata timeout(Duration timeout) throws IllegalArgumentException {
		if(timeout.compareTo(GrpcHeaders.VALUE_MAX_GRPC_TIMEOUT) > 0) {
			throw new IllegalArgumentException("Timeout is too big, MAX=" + GrpcHeaders.VALUE_MAX_GRPC_TIMEOUT);
		}
		String value;
		if(timeout.getNano() > 0) {
			long nanos = timeout.toNanos();
			if(nanos%1000000 == 0) {
				value = (nanos/1000000) + "m";
			}
			else if(nanos%1000 == 0) {
				value = (nanos/1000) + "u";
			}
			else {
				value = nanos + "n";
			}
		}
		else {
			long seconds = timeout.getSeconds();
			if(seconds%3600 == 0) {
				value = (seconds/3600) + "H";
			}
			else if(seconds%60 == 0) {
				value = (int)(seconds/60) + "M";
			}
			else {
				value = (int)seconds + "S";
			}
		}
		this.headers.set(GrpcHeaders.NAME_GRPC_TIMEOUT, value);
		this.timeout = timeout;
		return this;
	}
}
