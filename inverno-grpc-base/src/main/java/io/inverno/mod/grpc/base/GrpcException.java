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
 * Base exception class used to report gRPC errors.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public class GrpcException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * The gRPC status code.
	 */
	private int statusCode;
	
	/**
	 * The gRPC status.
	 */
	private GrpcStatus status;

	/**
	 * <p>
	 * Creates a gRPC exception with default status {@link GrpcStatus#INTERNAL}.
	 * </p>
	 */
	public GrpcException() {
		this(GrpcStatus.INTERNAL);
	}
	
	/**
	 * <p>
	 * Creates a gRPC exception with default status {@link GrpcStatus#INTERNAL} and specified message.
	 * </p>
	 * 
	 * @param message a message
	 */
	public GrpcException(String message) {
		this(GrpcStatus.INTERNAL, message);
	}
	
	/**
	 * <p>
	 * Creates a gRPC exception with default status {@link GrpcStatus#INTERNAL} and specified cause.
	 * </p>
	 * 
	 * @param cause a cause
	 */
	public GrpcException(Throwable cause) {
		this(GrpcStatus.INTERNAL, cause);
	}

	/**
	 * <p>
	 * Creates a gRPC exception with default status {@link GrpcStatus#INTERNAL}, specified message and cause.
	 * </p>
	 * 
	 * @param message a message
	 * @param cause   a cause
	 */
	public GrpcException(String message, Throwable cause) {
		this(GrpcStatus.INTERNAL, message, cause);
	}
	
	/**
	 * <p>
	 * Creates a gRPC exception with specified gRPC status code.
	 * </p>
	 * 
	 * @param statusCode a gRPC status code
	 * 
	 * @throws IllegalArgumentException if the specified status code is invalid
	 */
	public GrpcException(int statusCode) throws IllegalArgumentException {
		this.setStatusCode(statusCode);
	}
	
	/**
	 * <p>
	 * Creates a gRPC exception with specified gRPC status code and message.
	 * </p>
	 *
	 * @param statusCode a gRPC status code
	 * @param message    a message
	 *
	 * @throws IllegalArgumentException if the specified status code is invalid
	 */
	public GrpcException(int statusCode, String message) throws IllegalArgumentException {
		super(message);
		this.setStatusCode(statusCode);
	}
	
	/**
	 * <p>
	 * Creates a gRPC exception with specified gRPC status code and cause.
	 * </p>
	 *
	 * @param statusCode a gRPC status code
	 * @param cause      a cause
	 *
	 * @throws IllegalArgumentException if the specified status code is invalid
	 */
	public GrpcException(int statusCode, Throwable cause) throws IllegalArgumentException {
		super(cause);
		this.setStatusCode(statusCode);
	}

	/**
	 * <p>
	 * Creates a gRPC exception with specified gRPC status code, message and cause.
	 * </p>
	 *
	 * @param statusCode a gRPC status code
	 * @param message    a message
	 * @param cause      a cause
	 *
	 * @throws IllegalArgumentException if the specified status code is invalid
	 */
	public GrpcException(int statusCode, String message, Throwable cause) throws IllegalArgumentException {
		super(message, cause);
		this.setStatusCode(statusCode);
	}

	/**
	 * <p>
	 * Creates a gRPC exception with specified gRPC status.
	 * </p>
	 * 
	 * @param status a gRPC status
	 */
	public GrpcException(GrpcStatus status) {
		this.setStatus(status);
	}
	
	/**
	 * <p>
	 * Creates a gRPC exception with specified gRPC status and message.
	 * </p>
	 * 
	 * @param status  a gRPC status
	 * @param message a message
	 */
	public GrpcException(GrpcStatus status, String message) {
		super(message);
		this.setStatus(status);
	}
	
	/**
	 * <p>
	 * Creates a gRPC exception with specified gRPC status and cause.
	 * </p>
	 * 
	 * @param status a gRPC status
	 * @param cause  a cause
	 */
	public GrpcException(GrpcStatus status, Throwable cause) {
		super(cause);
		this.setStatus(status);
	}

	/**
	 * <p>
	 * Creates a gRPC exception with specified gRPC status, message and cause.
	 * </p>
	 * 
	 * @param status  a gRPC status
	 * @param message a message
	 * @param cause   a cause
	 */
	public GrpcException(GrpcStatus status, String message, Throwable cause) {
		super(message, cause);
		this.setStatus(status);
	}
	
	/**
	 * <p>
	 * Sets the gRPC status and code.
	 * </p>
	 * 
	 * @param statusCode a gRPC status code 
	 * 
	 * @throws IllegalArgumentException if the specified status code is invalid
	 */
	private void setStatusCode(int statusCode) throws IllegalArgumentException {
		if(statusCode < 0) {
			throw new IllegalArgumentException("Invalid status code: " + statusCode);
		}
		this.statusCode = statusCode;
		try {
			this.status = GrpcStatus.valueOf(statusCode);
		}
		catch(IllegalArgumentException e) {
			this.status = null;
		}
	}
	
	/**
	 * <p>
	 * Sets the gRPC status and code.
	 * </p>
	 * 
	 * @param status a gRPC status
	 */
	private void setStatus(GrpcStatus status) {
		this.status = status;
		this.statusCode = status.getCode();
	}

	/**
	 * <p>
	 * Returns the gRPC status code.
	 * </p>
	 * 
	 * @return a gRPC status code
	 */
	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * <p>
	 * Returns the gRPC status.
	 * </p>
	 * 
	 * @return a gRPC status or null if the code specified does correspond to any known status
	 */
	public GrpcStatus getStatus() {
		return status;
	}
	
	/**
	 * <p>
	 * Wraps the specified error into an GrpcException.
	 * </p>
	 * 
	 * <p>
	 * The specified error is returned untouched if it is already an {@code GrpcException} instance, otherwise it is wrapped in a {@code GrpcException} with {@link GrpcStatus#INTERNAL} code.
	 * </p>
	 * 
	 * @param error the error to wrap
	 * 
	 * @return the specified error if it is a {@code GrpcException}, a {@code GrpcException} wrapping the specified error otherwise
	 */
	public static GrpcException wrap(Throwable error) {
		if(error instanceof GrpcException) {
			return (GrpcException)error;
		}
		return new GrpcException(error);
	}
}
