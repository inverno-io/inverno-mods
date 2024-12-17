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
 * Enumeration of gRPC statuses as defined by <a href="https://grpc.io/docs/guides/status-codes/">gRPC Status Codes</a>.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public enum GrpcStatus {

	/**
	 * <p>
	 * Not an error; returned on success.
	 * </p>
	 */
    OK(0),
	/**
	 * <p>
	 * The operation was cancelled, typically by the caller.
	 * </p>
	 */
    CANCELLED(1),
	/**
	 * <p>
	 * Unknown error.
	 * </p>
	 * 
	 * <p>
	 * For example, this error may be returned when a status value received from another address space belongs to an error space that is not known in this address space. Also errors raised by
	 * APIs that do not return enough error information may be converted to this error.
	 * </p>
	 */
    UNKNOWN(2),
	/**
	 * <p>
	 * The client specified an invalid argument.
	 * </p>
	 * 
	 * <p>
	 * Note that this differs from {@link #FAILED_PRECONDITION}. {@code INVALID_ARGUMENT} indicates arguments that are problematic regardless of the state of the system (e.g., a malformed file name).
	 * </p>
	 */
    INVALID_ARGUMENT(3),
	/**
	 * <p>
	 * The deadline expired before the operation could complete.
	 * </p>
	 * 
	 * <p>
	 * For operations that change the state of the system, this error may be returned even if the operation has completed successfully. For example, a successful response from a server could have been
	 * delayed long.
	 * </p>
	 */
    DEADLINE_EXCEEDED(4),
	/**
	 * <p>
	 * Some requested entity (e.g., file or directory) was not found.
	 * </p>
	 * 
	 * <p>
	 * Note to server developers: if a request is denied for an entire class of users, such as gradual feature rollout or undocumented allowlist, {@code NOT_FOUND} may be used. If a request is denied
	 * for some users within a class of users, such as user-based access control, {@link #PERMISSION_DENIED} must be used.
	 * </p>
	 */
    NOT_FOUND(5),
	/**
	 * <p>
	 * The entity that a client attempted to create (e.g., file or directory) already exists.
	 * </p>
	 */
    ALREADY_EXISTS(6),
	/**
	 * <p>
	 * The caller does not have permission to execute the specified operation. 
	 * </p>
	 * 
	 * <p>
	 * {@code PERMISSION_DENIED} must not be used for rejections caused by exhausting some resource (use {@link #RESOURCE_EXHAUSTED} instead for those errors). {@code PERMISSION_DENIED} must not be
	 * used if the caller can not be identified (use {@link #UNAUTHENTICATED} instead for those errors). This error code does not imply the request is valid or the requested entity exists or satisfies
	 * other pre-conditions.
	 * </p>
	 */
    PERMISSION_DENIED(7),
	/**
	 * <p>
	 * Some resource has been exhausted, perhaps a per-user quota, or perhaps the entire file system is out of space.
	 * </p>
	 */
    RESOURCE_EXHAUSTED(8),
	/**
	 * <p>
	 * The operation was rejected because the system is not in a state required for the operation’s execution.
	 * </p>
	 * 
	 * <p>
	 * For example, the directory to be deleted is non-empty, an rmdir operation is applied to a non-directory, etc. Service implementors can use the following guidelines to decide between
	 * {@code FAILED_PRECONDITION}, {@link #ABORTED}, and {@link #UNAVAILABLE}:
	 * </p>
	 * 
	 * <ul>
	 * <li>Use {@link #UNAVAILABLE} if the client can retry just the failing call.</li>
	 * <li>Use ABORTED if the client should retry at a higher level (e.g., when a client-specified test-and-set fails, indicating the client should restart a read-modify-write sequence).</li>
	 * <li>Use {@code FAILED_PRECONDITION} if the client should not retry until the system state has been explicitly fixed. E.g., if an {@code rmdir} fails because the directory is non-empty,
	 * {@code FAILED_PRECONDITION} should be returned since the client should not retry unless the files are deleted from the directory.</li>
	 * </ul>
	 */
    FAILED_PRECONDITION(9),
	/**
	 * <p>
	 * The operation was aborted, typically due to a concurrency issue such as a sequencer check failure or transaction abort.
	 * </p>
	 * 
	 * <p>
	 * See the guidelines above for deciding between {@link #FAILED_PRECONDITION}, {@code ABORTED}, and {@link #UNAVAILABLE}.
	 * </p>
	 */
    ABORTED(10),
	/**
	 * <p>
	 * The operation was attempted past the valid range.
	 * </p>
	 * 
	 * <p>
	 * E.g., seeking or reading past end-of-file. Unlike {@link #INVALID_ARGUMENT}, this error indicates a problem that may be fixed if the system state changes. For example, a 32-bit file system will
	 * generate {@link #INVALID_ARGUMENT} if asked to read at an offset that is not in the range {@code [0,2^32-1]}, but it will generate {@code OUT_OF_RANGE} if asked to read from an offset past the
	 * current file size. There is a fair bit of overlap between {@link #FAILED_PRECONDITION} and {@code OUT_OF_RANGE}. We recommend using {@code OUT_OF_RANGE} (the more specific error) when it
	 * applies so that callers who are iterating through a space can easily look for an {@code OUT_OF_RANGE} error to detect when they are done.
	 * </p>
	 */
    OUT_OF_RANGE(11),
	/**
	 * <p>
	 * The operation is not implemented or is not supported/enabled in this service.
	 * </p>
	 */
    UNIMPLEMENTED(12),
	/**
	 * <p>
	 * Internal errors.
	 * </p>
	 * 
	 * <p>
	 * This means that some invariants expected by the underlying system have been broken. This error code is reserved for serious errors.
	 * </p>
	 */
    INTERNAL(13),
	/**
	 * <p>
	 * The service is currently unavailable.
	 * </p>
	 * 
	 * <p>
	 * This is most likely a transient condition, which can be corrected by retrying with a backoff. Note that it is not always safe to retry non-idempotent operations.
	 * </p>
	 */
    UNAVAILABLE(14),
	/**
	 * <p>
	 * Unrecoverable data loss or corruption.
	 * </p>
	 */
    DATA_LOSS(15),
	/**
	 * <p>
	 * The request does not have valid authentication credentials for the operation.
	 * </p>
	 */
    UNAUTHENTICATED(16);
	
	/**
	 * The gRPC status code.
	 */
	private final int code;

	/**
	 * <p>
	 * Creates a gRPC status with the specified code.
	 * </p>
	 * 
	 * @param code a gRPC status code
	 */
    GrpcStatus(int code) {
      this.code = code;
    }

	/**
	 * <p>
	 * Returns the gRPC status code.
	 * </p>
	 * 
	 * @return the gRPC status code
	 */
	public int getCode() {
		return code;
	}
	
	/**
	 * <p>
	 * Returns the gRPC status corresponding to the specified code.
	 * </p>
	 * 
	 * @param code a gRPC status code
	 * 
	 * @return a gRPC status
	 * 
	 * @throws IllegalArgumentException if the specified status doesn't correspond to a known gRPC status
	 */
	public static GrpcStatus valueOf(int code) throws IllegalArgumentException {
		for(GrpcStatus status : values()) {
			if(status.getCode() == code) {
				return status;
			}
		}
		throw new IllegalArgumentException("No enum constant for status code: " + code);
	}
	
	/**
	 * <p>
	 * Maps an HTTP/2 Error code to a gRPC status as defined by the <a href="https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md">gRPC protocol</a>.
	 * </p>
	 * 
	 * @param code an HTTP/2 error code
	 * 
	 * @return a gRPC status or null if no mapping exists
	 */
	public static GrpcStatus fromHttp2Code(long code) {
		GrpcStatus status;
		switch((int)code) {
			case 0x00: // Http2Error.NO_ERROR
			case 0x01: // Http2Error.PROTOCOL_ERROR
			case 0x02: // Http2Error.INTERNAL_ERROR
			case 0x03: // Http2Error.FLOW_CONTROL_ERROR
			case 0x04: // Http2Error.SETTINGS_TIMEOUT
			case 0x06: // Http2Error.FRAME_SIZE_ERROR
			case 0x09: // Http2Error.COMPRESSION_ERROR
			case 0x0A: // Http2Error.CONNECT_ERROR
				status = GrpcStatus.INTERNAL;
				break;
			case 0x07: // Http2Error.REFUSED_STREAM
				status = GrpcStatus.UNAVAILABLE;
				break;
			case 0x08: // Http2Error.CANCEL
				status = GrpcStatus.CANCELLED;
				break;
			case 0x0B: // Http2Error.ENHANCE_YOUR_CALM
				status = GrpcStatus.RESOURCE_EXHAUSTED;
				break;
			case 0x0C: // Http2Error.INADEQUATE_SECURITY
				status = GrpcStatus.PERMISSION_DENIED;
				break;
			case 0x05: // Http2Error.STREAM_CLOSED
			case 0x0D: // Http2Error.HTTP_1_1_REQUIRED
			default: 
				// Let it propagate as is
				return null;
		}
		return status;
	}
}
