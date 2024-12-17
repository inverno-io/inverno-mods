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
import com.google.protobuf.MessageLite;
import io.inverno.mod.grpc.base.GrpcOutboundMetadata;
import io.inverno.mod.http.base.OutboundHeaders;
import java.util.Base64;

/**
 * <p>
 * Base {@link GrpcOutboundMetadata} metadata.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public abstract class AbstractGrpcResponseMetadata<A extends GrpcOutboundMetadata<A>> extends AbstractGrpcMetadata<OutboundHeaders<?>> implements GrpcOutboundMetadata<A> {

	/**
	 * <p>
	 * Creates base gRPC response metadata.
	 * </p>
	 * 
	 * @param headers           the HTTP headers
	 * @param extensionRegistry the Protobuf extension registry
	 */
	protected AbstractGrpcResponseMetadata(OutboundHeaders<?> headers, ExtensionRegistry extensionRegistry) {
		super(headers, extensionRegistry);
	}
	
	@Override
	public boolean isWritten() {
		return this.headers.isWritten();
	}

	@Override
	@SuppressWarnings("unchecked")
	public A add(CharSequence name, CharSequence value) {
		this.headers.add(name, value);
		return (A)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public A set(CharSequence name, CharSequence value) {
		this.headers.set(name, value);
		return (A)this;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public A remove(CharSequence... names) {
		this.headers.remove(names);
		return (A)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends MessageLite> A addBinary(CharSequence name, T value) {
		this.headers.add(name + "-bin", Base64.getEncoder().encodeToString(value.toByteArray()));
		return (A)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends MessageLite> A setBinary(CharSequence name, T value) {
		this.headers.set(name + "-bin", Base64.getEncoder().encodeToString(value.toByteArray()));
		return (A)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public A removeBinary(CharSequence... names) {
		for(CharSequence name : names) {
			this.headers.remove(name + "-bin");
		}
		return (A)this;
	}

}
