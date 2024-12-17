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
package io.inverno.mod.discovery;

/**
 * <p>
 * Thrown to indicate an error during service discovery process.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class ServiceDiscoveryException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * The service ID for which an error was raised.
	 */
	private final ServiceID serviceId;

	/**
	 * <p>
	 * Creates a service discovery exception to report a null service ID.
	 * </p>
	 */
	public ServiceDiscoveryException() {
		this.serviceId = null;
	}

	/**
	 * <p>
	 * Creates a service discovery exception with the specified service ID.
	 * </p>
	 *
	 * @param serviceId The service ID
	 */
	public ServiceDiscoveryException(ServiceID serviceId) {
		this.serviceId = serviceId;
	}

	/**
	 * <p>
	 * Creates a service discovery exception with the specified service ID and message.
	 * </p>
	 *
	 * @param serviceId the service ID
	 * @param message a message
	 */
	public ServiceDiscoveryException(ServiceID serviceId, String message) {
		super(message);
		this.serviceId = serviceId;
	}

	/**
	 * <p>
	 * Creates a service discovery exception with the specified service ID and cause.
	 * </p>
	 *
	 * @param serviceId the service ID
	 * @param cause the cause
	 */
	public ServiceDiscoveryException(ServiceID serviceId, Throwable cause) {
		super(cause);
		this.serviceId = serviceId;
	}

	/**
	 * <p>
	 * Creates a service discovery exception with the specified service ID, message and cause.
	 * </p>
	 *
	 * @param serviceId the service ID
	 * @param message a message
	 * @param cause the cause
	 */
	public ServiceDiscoveryException(ServiceID serviceId, String message, Throwable cause) {
		super(message, cause);
		this.serviceId = serviceId;
	}

	/**
	 * <p>
	 * Returns the service ID.
	 * </p>
	 *
	 * @return the service ID or null
	 */
	public ServiceID getServiceId() {
		return serviceId;
	}
}
