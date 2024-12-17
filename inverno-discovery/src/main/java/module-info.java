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

/**
 * <p>
 * The Inverno framework discovery module defines the API used for service discovery.
 * </p>
 *
 * <p>
 * A service can be defined as a function that can process specific requests. In a distributed architecture, it is usually running on one or more instances. When an application needs to invoke a
 * service it must first resolve the instances of the service and then send a request to a particular instance. The service discovery API defines the
 * {@link io.inverno.mod.discovery.DiscoveryService Discovery Service} used to resolve {@link io.inverno.mod.discovery.Service Services} and select the right
 * {@link io.inverno.mod.discovery.ServiceInstance Service Instance} to process specific Service Requests.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
module io.inverno.mod.discovery {
	requires io.inverno.mod.base;
	requires static io.inverno.mod.configuration;

	requires org.apache.commons.lang3;
	requires org.apache.logging.log4j;
	requires org.reactivestreams;
	requires reactor.core;

	exports io.inverno.mod.discovery;
}