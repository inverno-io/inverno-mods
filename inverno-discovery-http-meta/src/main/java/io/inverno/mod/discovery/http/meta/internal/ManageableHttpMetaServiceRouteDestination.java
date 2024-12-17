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
package io.inverno.mod.discovery.http.meta.internal;

import io.inverno.mod.discovery.ManageableService;
import io.inverno.mod.discovery.http.HttpServiceInstance;
import io.inverno.mod.discovery.http.HttpTrafficPolicy;
import io.inverno.mod.discovery.http.meta.HttpMetaServiceDescriptor;
import io.inverno.mod.http.client.UnboundExchange;
import java.util.function.UnaryOperator;

/**
 * <p>
 * A manageable HTTP meta service route destination holding a {@link ManageableService}
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class ManageableHttpMetaServiceRouteDestination extends HttpMetaServiceRouteDestination {

	/**
	 * <p>
	 * Creates a manageable HTTP meta service route destination.
	 * </p>
	 *
	 * @param descriptor            a destination descriptor
	 * @param trafficPolicyOverride a traffic policy override
	 * @param service               a manageable HTTP service
	 */
	public ManageableHttpMetaServiceRouteDestination(HttpMetaServiceDescriptor.DestinationDescriptor descriptor, UnaryOperator<HttpTrafficPolicy> trafficPolicyOverride, ManageableService<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> service) {
		super(descriptor, trafficPolicyOverride, service);
	}

	@Override
	public ManageableService<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> getService() {
		return (ManageableService<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy>)super.getService();
	}
}
