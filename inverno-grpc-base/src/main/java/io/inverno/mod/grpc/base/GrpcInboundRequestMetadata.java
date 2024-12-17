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

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * <p>
 * Represents immutable inbound gRPC request metadata.
 * </p>
 * 
 * <p>
 * This extends the {@link GrpcInboundMetadata} to expose request specific information like accepted message encodings, message encoding, message type and timeout.
 * </p>
 * 
 * <p>
 * An inbound request is received by a server in a server exchange.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public interface GrpcInboundRequestMetadata extends GrpcInboundMetadata {

	/**
	 * <p>
	 * Returns the list of message encodings accepted by a client.
	 * </p>
	 * 
	 * @return a list of message encodings or an empty list
	 */
	List<String> getAcceptMessageEncoding();
	
	/**
	 * <p>
	 * Returns the encoding of messages sent by a client.
	 * </p>
	 * 
	 * @return an optional returning the message encoding or an empty optional if none was specified
	 */
	Optional<String> getMessageEncoding();
	
	/**
	 * <p>
	 * Returns the type of messages sent by a client.
	 * </p>
	 * 
	 * @return an optional returning the message type or an empty optional if none was specified
	 */
	Optional<String> getMessageType();
	
	/**
	 * <p>
	 * Returns the gRPC timeout.
	 * </p>
	 * 
	 * @return an optional returning the gRPC timeout or an empty optional if none was specified
	 */
	Optional<Duration> getTimeout();
}
