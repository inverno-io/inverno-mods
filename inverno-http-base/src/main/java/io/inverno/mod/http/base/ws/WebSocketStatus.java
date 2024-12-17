/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.http.base.ws;

/**
 * <p>
 * Represents WebSocket status codes as defined by <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-7.4">RC 6455 Section 7.4</a>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public enum WebSocketStatus {
	
	/**
	 * <p>
	 * A normal closure, meaning that the purpose for which the connection was established has been fulfilled.
	 * </p>
	 * 
	 * <p>
	 * <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-7.4.1">RFC 6455 Section 7.4.1</a>
	 * </p>
	 */
	NORMAL_CLOSURE((short)1000, "Normal Closure"),
	
	/**
	 * <p>
	 * An endpoint is "going away", such as a server going down or a browser having navigated away from a page.
	 * </p>
	 * 
	 * <p>
	 * <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-7.4.1">RFC 6455 Section 7.4.1</a>
	 * </p>
	 */
	ENDPOINT_UNAVAILABLE((short)1001, "Going Away"),
	
	/**
	 * <p>
	 * An endpoint is terminating the connection due to a protocol error.
	 * </p>
	 * 
	 * <p>
	 * <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-7.4.1">RFC 6455 Section 7.4.1</a>
	 * </p>
	 */
	PROTOCOL_ERROR((short)1002, "Protocol Error"),
	
	/**
	 * <p>
	 * An endpoint is terminating the connection because it has received a type of data it cannot accept (e.g., an endpoint that understands only text data MAY send this if it receives a binary
	 * message).
	 * </p>
	 * 
	 * <p>
	 * <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-7.4.1">RFC 6455 Section 7.4.1</a>
	 * </p>
	 */
	UNSUPPORTED_DATA((short)1003, "Unsupported Data"),
	
	/**
	 * <p>
	 * Designated for use in applications expecting a status code to indicate that no status code was actually present.
	 * </p>
	 * 
	 * <p>
	 * This is a reserved value that MUST NOT be set as a status code in a Close control frame by an endpoint.
	 * </p>
	 * 
	 * <p>
	 * <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-7.4.1">RFC 6455 Section 7.4.1</a>
	 * </p>
	 */
	NO_STATUS((short)1005, "No Status"),
	
	/**
	 * <p>
	 * Designated for use in applications expecting a status code to indicate that the connection was closed abnormally, e.g., without sending or receiving a Close control frame.
	 * </p>
	 * 
	 * <p>
	 * This is a reserved value that MUST NOT be set as a status code in a Close control frame by an endpoint.
	 * </p>
	 * 
	 * <p>
	 * <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-7.4.1">RFC 6455 Section 7.4.1</a>
	 * </p>
	 */
	CLOSED_ABNORMALLY((short)1006, "Closed Abnormally"),
	
	/**
	 * <p>
	 * An endpoint is terminating the connection because it has received data within a message that was not consistent with the type of the message (e.g., non-UTF-8 [RFC3629] data within a text
	 * message).
	 * </p>
	 * 
	 * <p>
	 * <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-7.4.1">RFC 6455 Section 7.4.1</a>
	 * </p>
	 */
	INVALID_PAYLOAD_DATA((short)1007, "Invalid Payload Data"),
	
	/**
	 * <p>
	 * An endpoint is terminating the connection because it has received a message that violates its policy.  
	 * </p>
	 * 
	 * <p>
	 * This is a generic status code that can be returned when there is no other more suitable status code (e.g., 1003 or 1009) or if there is a need to hide specific details about the policy.
	 * </p>
	 * 
	 * <p>
	 * <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-7.4.1">RFC 6455 Section 7.4.1</a>
	 * </p>
	 */
	POLICY_VIOLATION((short)1008, "Policy Violation"),
	
	/**
	 * <p>
	 * An endpoint is terminating the connection because it has received a message that is too big for it to process.
	 * </p>
	 * 
	 * <p>
	 * <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-7.4.1">RFC 6455 Section 7.4.1</a>
	 * </p>
	 */
	MESSAGE_TOO_BIG((short)1009, "Message Too Big"),
	
	/**
	 * <p>
	 * An endpoint (client) is terminating the connection because it has expected the server to negotiate one or more extension, but the server didn't return them in the response message of the
	 * WebSocket handshake.
	 * </p>
	 * 
	 * <p>
	 * The list of extensions that are needed SHOULD appear in the /reason/ part of the Close frame. Note that this status code is not used by the server, because it can fail the WebSocket handshake
	 * instead.
	 * </p>
	 * 
	 * <p>
	 * <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-7.4.1">RFC 6455 Section 7.4.1</a>
	 * </p>
	 */
	MANDATORY_EXTENSION((short)1010, "Mandatory Extension"),
	
	/**
	 * <p>
	 * A server is terminating the connection because it encountered an unexpected condition that prevented it from fulfilling the request.
	 * </p>
	 * 
	 * <p>
	 * <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-7.4.1">RFC 6455 Section 7.4.1</a>
	 * </p>
	 */
	INTERNAL_SERVER_ERROR((short)1011, "Internal Server Error"),
	
	/**
	 * <p>
	 * Indicates that the service is restarted.
	 * </p>
	 * 
	 * <p>
	 * A client may reconnect, and if it chooses to do, should reconnect using a randomized delay of 5 - 30s.
	 * </p>
	 * 
	 * <p>
	 * <a href="https://mailarchive.ietf.org/arch/msg/hybi/P_1vbD9uyHl63nbIIbFxKMfSwcM/">Additional WebSocket Close Error Codes</a>
	 * </p>
	 */
	SERVICE_RESTART((short)1012, "Service Restart"),

	/**
	 * <p>
	 * Indicates that the service is experiencing overload.
	 * </p>
	 * 
	 * <p>
	 * A client should only connect to a different IP (when there are multiple for the target) or reconnect to the same IP upon user action.
	 * </p>
	 * 
	 * <p>
	 * <a href="https://mailarchive.ietf.org/arch/msg/hybi/P_1vbD9uyHl63nbIIbFxKMfSwcM/">Additional WebSocket Close Error Codes</a>
	 * </p>
	 */
	TRY_AGAIN_LATER((short)1013, "Try Again Later"),

	/**
	 * <p>
	 * The server was acting as a gateway or proxy and received an invalid response from the upstream server.
	 * </p>
	 * 
	 * <p>
	 * <a href="https://mailarchive.ietf.org/arch/msg/hybi/VOLI2xp4tzFnIFYespe6oOtpFXA/">WebSocket Subprotocol Close Code: Bad Gateway</a>
	 * </p>
	 */
	BAD_GATEWAY((short)1014, "Bad Gateway"),
	
	/**
	 * <p>
	 * It is designated for use in applications expecting a status code to indicate that the connection was closed due to a failure to perform a TLS handshake (e.g., the server certificate can't be
	 * verified).
	 * </p>
	 * 
	 * <p>
	 * It is a reserved value that MUST NOT be set as a status code in a Close control frame by an endpoint.
	 * </p>
	 * 
	 * <p>
	 * <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-7.4.1">RFC 6455 Section 7.4.1</a>
	 * </p>
	 */
	TLS_HANDSHAKE_ERROR((short)1015, "TLS Handshake Error");
	
	private final short code;
	
	private final String reason;
	
	WebSocketStatus(short code, String reason) {
		this.code = code;
		this.reason = reason;
	}
	
	/**
	 * <p>
	 * Returns the actual WebSocket status code.
	 * </p>
	 * 
	 * @return the status code
	 */
	public short getCode() {
		return code;
	}
	
	/**
	 * <p>
	 * Returns the reason describing the status.
	 * </p>
	 * 
	 * @return a reason
	 */
	public String getReason() {
		return reason;
	}
	
	/**
	 * <p>
	 * Returns the status corresponding to the specified code.
	 * </p>
	 * 
	 * @param code a code
	 * 
	 * @return a WebSocket status
	 * 
	 * @throws IllegalArgumentException if the specified code doesn't correspond to a supported status
	 */
	public static WebSocketStatus valueOf(short code) throws IllegalArgumentException{
		switch(code) {
			case 1000: return NORMAL_CLOSURE;
			case 1001: return ENDPOINT_UNAVAILABLE;
			case 1002: return PROTOCOL_ERROR;
			case 1003: return UNSUPPORTED_DATA;
			case 1005: return NO_STATUS;
			case 1006: return CLOSED_ABNORMALLY;
			case 1007: return INVALID_PAYLOAD_DATA;
			case 1008: return POLICY_VIOLATION;
			case 1009: return MESSAGE_TOO_BIG;
			case 1010: return MANDATORY_EXTENSION;
			case 1011: return INTERNAL_SERVER_ERROR;
			case 1015: return TLS_HANDSHAKE_ERROR;
			default:
				throw new IllegalArgumentException("No enum constant for status code: " + code);
		}
		
	}
}
