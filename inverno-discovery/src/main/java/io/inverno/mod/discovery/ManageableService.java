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

import java.util.List;

/**
 * <p>
 * A manageable service allows an enclosing service to manage its set of service instances.
 * </p>
 *
 * <p>
 * This is typically used when there is a need to delegate service instance resolution (i.e. load balancing, routing...) to a higher service.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the type of service instance
 * @param <B> the type of service request
 * @param <C> the type of traffic policy
 */
public interface ManageableService<A extends ServiceInstance, B, C extends TrafficPolicy<A, B>> extends Service<A, B, C> {

	/**
	 * <p>
	 * Returns the service instances.
	 * </p>
	 *
	 * @return a list of service instances
	 */
	List<A> getInstances();
}
