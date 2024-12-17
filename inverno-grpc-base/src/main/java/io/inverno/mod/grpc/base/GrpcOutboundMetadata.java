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
package io.inverno.mod.grpc.base;

import com.google.protobuf.MessageLite;

/**
 * <p>
 * Represents mutable outbound gRPC metadata.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 * 
 * @param <A> the gRPC outbound metadata type
 */
public interface GrpcOutboundMetadata<A extends GrpcOutboundMetadata<A>> extends GrpcInboundMetadata {

	/**
	 * <p>
	 * Determines whether the metadata have been sent to the recipient.
	 * </p>
	 * 
	 * @return true if the metadata have been sent, false otherwise
	 */
	boolean isWritten();
	
	/**
	 * <p>
	 * Adds a metadata with the specified name and value.
	 * </p>
	 * 
	 * @param name  the metadata name
	 * @param value the metadata value
	 * 
	 * @return the outbound metadata
	 */
	A add(CharSequence name, CharSequence value);
	
	/**
	 * <p>
	 * Sets the value of the metadata with the specified name.
	 * </p>
	 * 
	 * @param name  the metadata name
	 * @param value the metadata value
	 * 
	 * @return the outbound metadata
	 */
	A set(CharSequence name, CharSequence value);
	
	/**
	 * <p>
	 * Removes the metadata with the specified names.
	 * </p>
	 * 
	 * @param names the names of the metadata to remove
	 * 
	 * @return the outbound metadata
	 */
	A remove(CharSequence... names);
	
	/**
	 * <p>
	 * Adds a binary metadata with the specified name and value.
	 * </p>
	 * 
	 * <p>
	 * The the binary name suffix {@code -bin} will be automatically added. The metadata value will be serialized in binary and encoded in a base64 string.
	 * </p>
	 * 
	 * @param <T>   the binary metadata message type
	 * @param name  the binary metadata name without the binary name suffix {@code -bin}
	 * @param value the binary metadata value
	 * 
	 * @return the outbound metadata
	 */
	<T extends MessageLite> A addBinary(CharSequence name, T value);
	
	/**
	 * <p>
	 * Sets a binary metadata with the specified name and value.
	 * </p>
	 * 
	 * <p>
	 * The the binary name suffix {@code -bin} will be automatically added. The metadata value will be serialized in binary and encoded in a base64 string.
	 * </p>
	 * 
	 * @param <T>   the binary metadata message type
	 * @param name  the binary metadata name without the binary name suffix {@code -bin}
	 * @param value the binary metadata value
	 * 
	 * @return the outbound metadata
	 */
	<T extends MessageLite> A setBinary(CharSequence name, T value);
	
	/**
	 * <p>
	 * Removes the binary metadata with the specified names.
	 * </p>
	 * 
	 * @param names the names of the binary metadata to remove without the binary name suffix {@code -bin}
	 * 
	 * @return the outbound metadata
	 */
	A removeBinary(CharSequence... names);
}
