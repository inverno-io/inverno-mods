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

import java.util.List;

/**
 * <p>
 * Represents mutable outbound gRPC response metadata.
 * </p>
 * 
 * <p>
 * This extends the {@link GrpcOutboundMetadata} to expose response specific information like accepted message encodings or message encoding.
 * </p>
 * 
 * <p>
 * An outbound response is sent by a server in a server exchange.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public interface GrpcOutboundResponseMetadata extends GrpcInboundResponseMetadata, GrpcOutboundMetadata<GrpcOutboundResponseMetadata> {
	
	/**
	 * <p>
	 * Sets the list of message encodings accepted by a server.
	 * </p>
	 * 
	 * @param messageEncodings a list of message encodings
	 * 
	 * @return the response metadata
	 */
	GrpcOutboundResponseMetadata acceptMessageEncoding(List<String> messageEncodings);
	
	/**
	 * <p>
	 * Sets the encoding of messages sent by a server.
	 * </p>
	 * 
	 * <p>
	 * Note that no error is reported here if an unsupported encoding is specified, the exchange will fail eventually when the response message publisher is subscribed to actually sent messages to the
	 * client.
	 * </p>
	 *
	 * @param messageEncoding a message encoding
	 * 
	 * @return the response metadata
	 */
	GrpcOutboundResponseMetadata messageEncoding(String messageEncoding);
}
