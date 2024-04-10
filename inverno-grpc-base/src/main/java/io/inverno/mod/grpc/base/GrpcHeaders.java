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

/**
 * <p>
 * Defines gRPC headers.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public final class GrpcHeaders {
	
	/**
	 * <a href="https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md#requests">gRPC over HTTP/2</a>.
	 */
	public static final String NAME_GRPC_TIMEOUT = "grpc-timeout";
	
	/**
	 * <a href="https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md#requests">gRPC over HTTP/2</a>.
	 */
	public static final String NAME_GRPC_MESSAGE_ENCODING = "grpc-encoding";
	
	/**
	 * <a href="https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md#requests">gRPC over HTTP/2</a>.
	 */
	public static final String NAME_GRPC_ACCEPT_MESSAGE_ENCODING = "grpc-accept-encoding";
	
	/**
	 * <a href="https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md#requests">gRPC over HTTP/2</a>.
	 */
	public static final String NAME_GRPC_MESSAGE_TYPE = "grpc-message-type";
	
	/**
	 * <a href="https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md#responses">gRPC over HTTP/2</a>.
	 */
	public static final String NAME_GRPC_STATUS = "grpc-status";
	
	/**
	 * <a href="https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md#responses">gRPC over HTTP/2</a>.
	 */
	public static final String NAME_GRPC_STATUS_MESSAGE = "grpc-message";
	
	/**
	 * <p>
	 * The maximum gRPC timeout value.
	 * </p>
	 *
	 * <p>
	 * The gRPC timeout value is limited to 8 digits and the biggest unit being the hour: {@code 99999999H} as defined by <a href="https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md">gRPC
	 * over HTTP/2</a>.
	 * </p>
	 */
	public static final Duration VALUE_MAX_GRPC_TIMEOUT = Duration.ofHours(99999999);
	
	/**
	 * <a href="https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md">gRPC over HTTP/2</a>.
	 */
	public static final String VALUE_IDENTITY = "identity";
	
	/**
	 * <a href="https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md">gRPC over HTTP/2</a>.
	 */
	public static final String VALUE_GZIP = "gzip";
	
	/**
	 * <a href="https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md">gRPC over HTTP/2</a>.
	 */
	public static final String VALUE_DEFLATE = "deflate";
	
	/**
	 * <a href="https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md">gRPC over HTTP/2</a>.
	 */
	public static final String VALUE_SNAPPY = "snappy";
	
	private GrpcHeaders() {}
}
