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
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * Represents immutable inbound gRPC metadata.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public interface GrpcInboundMetadata {
	
	/**
	 * <p>
	 * Returns the names of the metadata specified in the gRPC metadata.
	 * </p>
	 * 
	 * <p>
	 * Binary metadata are excluded from the resulting list.
	 * </p>
	 * 
	 * @return the plain metadata names
	 */
	Set<String> getNames();
	
	/**
	 * <p>
	 * Determines whether a metadata with the specified name is present.
	 * </p>
	 * 
	 * @param name a metadata name
	 * 
	 * @return true if a metadata is present, false otherwise
	 */
	boolean contains(CharSequence name);
	
	/**
	 * <p>
	 * Determines whether a metadata with the specified name and value is present.
	 * </p>
	 * 
	 * @param name  a metadata name
	 * @param value a metadata value
	 * 
	 * @return true if a metadata is present, false otherwise
	 */
	boolean contains(CharSequence name, CharSequence value);
	
	/**
	 * <p>
	 * Returns the value of the metadata with the specified name.
	 * </p>
	 *
	 * <p>
	 * If there are multiple metadata with the same name, the first one is returned.
	 * </p>
	 *
	 * @param name a metadata name
	 *
	 * @return an optional returning the value of the metadata or an empty optional if there's no metadata with the specified name
	 */
	Optional<String> get(CharSequence name);
	
	/**
	 * <p>
	 * Returns the values of all metadata with the specified name.
	 * </p>
	 *
	 * @param name a metadata name
	 *
	 * @return a list of metadata values or an empty list if there's no metadata with the specified name
	 */
	List<String> getAll(CharSequence name);
	
	/**
	 * <p>
	 * Returns the names of the binary metadata ({@code *-bin}) specified in the gRPC metadata.
	 * </p>
	 * 
	 * <p>
	 * Plain metadata are excluded from the resulting list which contains names without the binary name suffix {@code -bin}.
	 * </p>
	 * 
	 * @return the binary metadata names
	 */
	Set<String> getBinaryNames();
	
	/**
	 * <p>
	 * Determines whether a binary metadata with the specified name is present.
	 * </p>
	 * 
	 * @param name a binary metadata name without the binary name suffix {@code -bin}
	 * 
	 * @return true if a binary metadata is present, false otherwise
	 */
	boolean containsBinary(CharSequence name);
	
	/**
	 * <p>
	 * Determines whether a binary metadata with the specified name and value is present.
	 * </p>
	 * 
	 * @param <T>   the binary metadata message type
	 * @param name  a binary metadata name without the binary name suffix {@code -bin}
	 * @param value a metadata value
	 * 
	 * @return true if a binary metadata is present, false otherwise
	 */
	<T extends MessageLite> boolean containsBinary(CharSequence name, T value);
	
	/**
	 * <p>
	 * Returns the value of the binary metadata with the specified name.
	 * </p>
	 *
	 * <p>
	 * If there are multiple metadata with the same name, the first one is returned.
	 * </p>
	 *
	 * @param <T>                    the binary metadata message type
	 * @param name                   a binary metadata name without the binary name suffix {@code -bin}
	 * @param defaultMessageInstance the default message instance
	 *
	 * @return an optional returning the decoded value of the binary metadata or an empty optional if there's no metadata with the specified name
	 * 
	 * @throws IllegalArgumentException if there was an error parsing the value
	 */
	<T extends MessageLite> Optional<T> getBinary(CharSequence name, T defaultMessageInstance) throws IllegalArgumentException;
	
	/**
	 * <p>
	 * Returns the values of all binary metadata with the specified name.
	 * </p>
	 *
	 * @param <T>                    the binary metadata message type
	 * @param name                   a binary metadata name without the binary name suffix {@code -bin}
	 * @param defaultMessageInstance the default message instance
	 *
	 * @return a list of decoded binary metadata values or an empty list if there's no metadata with the specified name
	 * 
	 * @throws IllegalArgumentException if there was an error parsing the value
	 */
	<T extends MessageLite> List<T> getAllBinary(CharSequence name, T defaultMessageInstance) throws IllegalArgumentException;
}
