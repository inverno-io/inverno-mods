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
 * Thrown to indicate a malformed service descriptor configuration when resolving a service from configuration.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see AbstractConfigurationDiscoveryService
 */
public class MalformedServiceDescriptorException extends ServiceDiscoveryException {

	private static final long serialVersionUID = 1L;

	/**
	 * <p>
	 * Creates a malformed service descriptor exception to report a null service ID.
	 * </p>
	 */
	public MalformedServiceDescriptorException() {
		super(null);
	}

	/**
	 * <p>
	 * Creates a malformed service descriptor exception with the specified service ID.
	 * </p>
	 *
	 * @param serviceId The service ID
	 */
	public MalformedServiceDescriptorException(ServiceID serviceId) {
		super(serviceId);
	}

	/**
	 * <p>
	 * Creates a malformed service descriptor exception with the specified service ID and message.
	 * </p>
	 *
	 * @param serviceId the service ID
	 * @param message a message
	 */
	public MalformedServiceDescriptorException(ServiceID serviceId, String message) {
		super(serviceId, message);
	}

	/**
	 * <p>
	 * Creates a malformed service descriptor exception with the specified service ID and cause.
	 * </p>
	 *
	 * @param serviceId the service ID
	 * @param cause the cause
	 */
	public MalformedServiceDescriptorException(ServiceID serviceId, Throwable cause) {
		super(serviceId, cause);
	}

	/**
	 * <p>
	 * Creates a malformed service descriptor exception with the specified service ID, message and cause.
	 * </p>
	 *
	 * @param serviceId the service ID
	 * @param message a message
	 * @param cause the cause
	 */
	public MalformedServiceDescriptorException(ServiceID serviceId, String message, Throwable cause) {
		super(serviceId, message, cause);
	}
}
