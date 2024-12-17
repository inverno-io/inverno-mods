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

/**
 * <p>
 * Represents mutable outbound gRPC request metadata.
 * </p>
 * 
 * <p>
 * This extends the {@link GrpcOutboundMetadata} to expose request specific information like accepted message encodings, message encoding, message type and timeout.
 * </p>
 * 
 * <p>
 * An outbound request is sent by a client in a client exchange.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public interface GrpcOutboundRequestMetadata extends GrpcInboundRequestMetadata, GrpcOutboundMetadata<GrpcOutboundRequestMetadata> {

	/**
	 * <p>
	 * Sets the list of message encodings accepted by a client.
	 * </p>
	 * 
	 * @param messageEncodings a list of message encodings
	 * 
	 * @return the request metadata
	 */
	GrpcOutboundRequestMetadata acceptMessageEncoding(List<String> messageEncodings);
	
	/**
	 * <p>
	 * Sets the encoding of messages sent by a client.
	 * </p>
	 * 
	 * <p>
	 * Note that no error is reported here if an unsupported encoding is specified, the exchange will fail eventually when the request message publisher is subscribed to actually sent messages to the
	 * server.
	 * </p>
	 * 
	 * @param messageEncoding a message encoding
	 * 
	 * @return the request metadata
	 */
	GrpcOutboundRequestMetadata messageEncoding(String messageEncoding);
	
	/**
	 * <p>
	 * Sets the type of messages sent by a client.
	 * </p>
	 * 
	 * @param messageType a message type
	 * 
	 * @return the request metadata
	 */
	GrpcOutboundRequestMetadata messageType(String messageType);
	
	/**
	 * <p>
	 * Sets the gRPC timeout.
	 * </p>
	 * 
	 * @param timeout a timeout
	 * 
	 * @return the request metadata
	 * 
	 * @throws IllegalArgumentException if the specified duration is invalid 
	 * 
	 * @see GrpcHeaders#VALUE_MAX_GRPC_TIMEOUT
	 */
	GrpcOutboundRequestMetadata timeout(Duration timeout) throws IllegalArgumentException;
}
