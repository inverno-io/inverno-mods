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

/**
 * <p>
 * Base gRPC request for representing client or server requests.
 * </p>
 * 
 * <p>
 * It exposes gRPC request content as defined by <a href="https://datatracker.ietf.org/doc/html/draft-kumar-rtgwg-grpc-protocol-00">gRPC protocol</a>.
 * </p>
 * 
 * <p>
 * Considering a client exchange, where the request is created and sent from the client to the server, implementation shall provide methods to set gRPC request content. Considering a server exchange,
 * where the request is received by the server from the client, implementation shall only provide methods to access gRPC request content.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public interface GrpcBaseRequest {

	/**
	 * <p>
	 * Returns the fully qualified name of the service targeted in the request.
	 * </p>
	 * 
	 * <p>
	 * Protocol buffer defines the following format for service names: {@code <package>.<service>}.
	 * </p>
	 * 
	 * @return the target service name
	 */
	GrpcServiceName getServiceName();

	/**
	 * <p>
	 * Returns the name of the method targeted in the request.
	 * </p>
	 * 
	 * @return the target method name
	 */
	String getMethodName();
	
	/**
	 * <p>
	 * Returns the fully qualified name of the method targeted in the request.
	 * </p>
	 * 
	 * <p>
	 * Protocol buffer defines the following format for method names: {@code <package>.<service>/<method>}.
	 * </p>
	 * 
	 * @return the target full method name
	 */
	String getFullMethodName();
	
	/**
	 * <p>
	 * Returns gRPC request metadata.
	 * </p>
	 * 
	 * @return the request metadata
	 */
	GrpcInboundRequestMetadata metadata();
}
