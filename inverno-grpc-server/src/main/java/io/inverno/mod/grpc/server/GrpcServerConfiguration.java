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
package io.inverno.mod.grpc.server;

import io.inverno.core.annotation.NestedBean;
import io.inverno.mod.configuration.Configuration;
import io.inverno.mod.grpc.base.GrpcBaseConfiguration;

/**
 * <p>
 * The gRPC server module configuration.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
@Configuration(name = "configuration")
public interface GrpcServerConfiguration {

	/**
	 * <p>
	 * The gRPC base module configuration.
	 * </p>
	 * 
	 * @return the gRPC base module configuration
	 */
	@NestedBean
	GrpcBaseConfiguration base();
}
