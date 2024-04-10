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
package io.inverno.mod.http.client;

/**
 * <p>
 * Thrown to indicate that an HTTP/2 steam has been reset by peer.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public class ResetStreamException extends HttpClientException {

	private static final long serialVersionUID = 1L;

	/**
	 * The reset stream frame error code
	 */
	private final long errorCode;

	/**
	 * <p>
	 * Creates a reset stream exception.
	 * </p>
	 * 
	 * @param errorCode the reset stream frame error code
	 */
	public ResetStreamException(long errorCode) {
		this.errorCode = errorCode;
	}

	/**
	 * <p>
	 * Creates a reset stream exception.
	 * </p>
	 * 
	 * @param errorCode the reset stream frame error code
	 * @param message   the message
	 */
	public ResetStreamException(long errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	/**
	 * <p>
	 * Returns the reset stream frame error code.
	 * </p>
	 * 
	 * @return the error code
	 */
	public long getErrorCode() {
		return errorCode;
	}
}
