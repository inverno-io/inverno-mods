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
package io.inverno.mod.http.server.ws;

import io.inverno.mod.http.base.BadRequestException;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * A HTTP exception that indicates that the WebSocket subprotocols provided in the upgrade request are not supported.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 *
 * @see HttpException
 */
public class UnsupportedProtocolException extends BadRequestException {

	private static final long serialVersionUID = 1L;
	
	/**
	 * The list of supported subprotocols.
	 */
	private final Optional<Set<String>> supportedProtocols;

	/**
	 * <p>
	 * Creates an unsupported protocol exception.
	 * </p>
	 */
	public UnsupportedProtocolException() {
		super();
		this.supportedProtocols = Optional.empty();
	}

	/**
	 * <p>
	 * Creates an unsupported protocol exception with the specified message.
	 * </p>
	 * 
	 * @param message a message
	 */
	public UnsupportedProtocolException(String message) {
		super(message);
		this.supportedProtocols = Optional.empty();
	}

	/**
	 * <p>
	 * Creates an unsupported protocol exception with the specified cause.
	 * </p>
	 * 
	 * @param cause a cause
	 */
	public UnsupportedProtocolException(Throwable cause) {
		super(cause);
		this.supportedProtocols = Optional.empty();
	}

	/**
	 * <p>
	 * Creates an unsupported protocol exception with the specified message and cause.
	 * </p>
	 * 
	 * @param message a message
	 * @param cause   a cause
	 */
	public UnsupportedProtocolException(String message, Throwable cause) {
		super(message, cause);
		this.supportedProtocols = Optional.empty();
	}
	
	/**
	 * <p>
	 * Creates an unsupported protocol exception with the specified list of media types
	 * accepted by the requested resource.
	 * </p>
	 * 
	 * @param supportedProtocols   a list of supported subprotocols
	 */
	public UnsupportedProtocolException(Set<String> supportedProtocols) {
		super();
		this.supportedProtocols = Optional.ofNullable(supportedProtocols);
	}

	/**
	 * <p>
	 * Creates an unsupported protocol exception with the specified list of media types
	 * accepted by the requested resource and message.
	 * </p>
	 * 
	 * @param supportedProtocols   a list of supported subprotocols
	 * @param message              a message
	 */
	public UnsupportedProtocolException(Set<String> supportedProtocols, String message) {
		super(message);
		this.supportedProtocols = Optional.ofNullable(supportedProtocols);
	}

	/**
	 * <p>
	 * Creates an unsupported protocol exception with the specified list of media types
	 * accepted by the requested resource and cause.
	 * </p>
	 * 
	 * @param supportedProtocols   a list of supported subprotocols
	 * @param cause                a cause
	 */
	public UnsupportedProtocolException(Set<String> supportedProtocols, Throwable cause) {
		super(cause);
		this.supportedProtocols = Optional.ofNullable(supportedProtocols);
	}

	/**
	 * <p>
	 * Creates an unsupported protocol exception with the specified list of media types
	 * accepted by the requested resource, message and cause.
	 * </p>
	 * 
	 * @param supportedProtocols   a list of supported subprotocols
	 * @param message              a message
	 * @param cause                a cause
	 */
	public UnsupportedProtocolException(Set<String> supportedProtocols, String message, Throwable cause) {
		super(message, cause);
		this.supportedProtocols = Optional.ofNullable(supportedProtocols);
	}
	
	/**
	 * <p>
	 * Returns the list of supported WebSocket subprotocols.
	 * </p>
	 * 
	 * @return the list of supported WebSocket subprotocols
	 */
	public Optional<Set<String>> getSupportedProtocol() {
		return supportedProtocols;
	}
}
