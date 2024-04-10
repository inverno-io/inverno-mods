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
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import io.inverno.mod.grpc.base.GrpcInboundMetadata;
import io.inverno.mod.http.base.InboundHeaders;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Base {@link GrpcInboundMetadata} implementation.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public abstract class AbstractGrpcMetadata<A extends InboundHeaders> implements GrpcInboundMetadata {
	
	/**
	 * The underlying HTTP headers.
	 */
	protected final A headers;
	
	/**
	 * The Protobuf extension registry.
	 */
	protected final ExtensionRegistry extensionRegistry;

	/**
	 * <p>
	 * Creates base gRPC metadata.
	 * </p>
	 * 
	 * @param headers           the HTTP headers
	 * @param extensionRegistry the Protobuf extension registry
	 */
	protected AbstractGrpcMetadata(A headers, ExtensionRegistry extensionRegistry) {
		this.headers = headers;
		this.extensionRegistry = extensionRegistry;
	}
	
	@Override
	public boolean contains(CharSequence name) {
		return this.headers.contains(name);
	}

	@Override
	public boolean contains(CharSequence name, CharSequence value) {
		return this.headers.contains(name, value);
	}
	
	@Override
	public Set<String> getNames() {
		return this.headers.getNames().stream().filter(name -> !name.endsWith("-bin")).collect(Collectors.toSet());
	}

	@Override
	public Optional<String> get(CharSequence name) {
		return this.headers.get(name);
	}

	@Override
	public List<String> getAll(CharSequence name) {
		return this.headers.getAll(name);
	}
	
	@Override
	public Set<String> getBinaryNames() {
		return this.headers.getNames().stream().filter(name -> name.endsWith("-bin")).map(s -> s.substring(0, s.length() - 4)).collect(Collectors.toSet());
	}
	
	@Override
	public boolean containsBinary(CharSequence name) {
		return this.headers.contains(name + "-bin");
	}

	@Override
	public <T extends MessageLite> boolean containsBinary(CharSequence name, T value) {
		if(!this.headers.contains(name + "-bin")) {
			return false;
		}
		return this.headers.contains(name + "-bin", Base64.getEncoder().encodeToString(value.toByteArray()));
	}

	@Override
	public <T extends MessageLite> Optional<T> getBinary(CharSequence name, T defaultMessageInstance) throws IllegalArgumentException {
		return this.headers.get(name + "-bin")
			.map(value -> {
				try {
					return (T)defaultMessageInstance.getParserForType().parseFrom(Base64.getDecoder().decode(value), this.extensionRegistry);
				} 
				catch(InvalidProtocolBufferException e) {
					throw new IllegalArgumentException(e);
				}
			});
	}

	@Override
	public <T extends MessageLite> List<T> getAllBinary(CharSequence name, T defaultMessageInstance) throws IllegalArgumentException {
		return this.headers.getAll(name + "-bin").stream()
			.map(value -> {
				try {
					return (T)defaultMessageInstance.getParserForType().parseFrom(Base64.getDecoder().decode(value), this.extensionRegistry);
				} 
				catch(InvalidProtocolBufferException e) {
					throw new IllegalArgumentException(e);
				}
			})
			.collect(Collectors.toList());
	}
}
