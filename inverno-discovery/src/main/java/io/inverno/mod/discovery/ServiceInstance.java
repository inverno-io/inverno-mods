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

import reactor.core.publisher.Mono;

/**
 * <p>
 * A service instance is used to process a service request.
 * </p>
 *
 * <p>
 * It is obtained from {@link Service#getInstance(Object)}, it shall expose services or methods to actually process specific service requests.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public interface ServiceInstance {

	/**
	 * <p>
	 * Shutdowns the service instance.
	 * </p>
	 *
	 * <p>
	 * A service instance shall never be shutdown directly, it should always be shutdown by its enclosing service (see {@link Service#shutdown()}).
	 * </p>
	 *
	 * @return a {@code Mono} which completes once the service instance is shutdown
	 */
	Mono<Void> shutdown();

	/**
	 * <p>
	 * Gracefully shutdowns the service instance.
	 * </p>
	 *
	 * <p>
	 * A service instance shall never be shutdown directly, it should always be shutdown by its enclosing service (see {@link Service#shutdownGracefully()}).
	 * </p>
	 *
	 * @return a {@code Mono} which completes once the service instance is shutdown
	 */
	Mono<Void> shutdownGracefully();
}
