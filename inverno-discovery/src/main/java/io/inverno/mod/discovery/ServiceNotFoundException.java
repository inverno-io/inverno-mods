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
 * Thrown to indicate that no service could be resolved by a {@link DiscoveryService} for a specific {@link ServiceID}.
 * </p>
 *
 * <p>
 * Note that, as per documentation, {@link DiscoveryService#resolve(ServiceID, TrafficPolicy)} shall return an empty mono when no service could be resolved, this exception must then be thrown
 * explicitly whenever there's a need to notify this as an error.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class ServiceNotFoundException extends ServiceDiscoveryException {

	private static final long serialVersionUID = 1L;

	/**
	 * <p>
	 * Creates a service not found exception to report a null service ID.
	 * </p>
	 */
	public ServiceNotFoundException() {
		super(null);
	}

	/**
	 * <p>
	 * Creates a service not found exception with the specified service ID.
	 * </p>
	 *
	 * @param serviceId The service ID
	 */
	public ServiceNotFoundException(ServiceID serviceId) {
		super(serviceId);
	}

	/**
	 * <p>
	 * Creates a service not found exception with the specified service ID and message.
	 * </p>
	 *
	 * @param serviceId the service ID
	 * @param message a message
	 */
	public ServiceNotFoundException(ServiceID serviceId, String message) {
		super(serviceId, message);
	}

	/**
	 * <p>
	 * Creates a service not found exception with the specified service ID and cause.
	 * </p>
	 *
	 * @param serviceId the service ID
	 * @param cause the cause
	 */
	public ServiceNotFoundException(ServiceID serviceId, Throwable cause) {
		super(serviceId, cause);
	}

	/**
	 * <p>
	 * Creates a service not found exception with the specified service ID, message and cause.
	 * </p>
	 *
	 * @param serviceId the service ID
	 * @param message a message
	 * @param cause the cause
	 */
	public ServiceNotFoundException(ServiceID serviceId, String message, Throwable cause) {
		super(serviceId, message, cause);
	}
}
