/*
 * Copyright 2020 Jeremy KUHN
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
package io.winterframework.mod.web.internal;

import java.util.function.Supplier;

import io.winterframework.core.annotation.Bean;
import io.winterframework.mod.base.resource.ResourceService;

/**
 * <p>
 * The {@link ResourceService} socket.
 * </p>
 * 
 * <p>
 * A net service is required by the HTTP server module and to load Open API
 * specifications among other things.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see OpenApiWebRouterConfigurer
 * @see WebjarsWebRouterConfigurer
 */
@Bean(name = "resourceService")
public interface ResourceServiceSocket extends Supplier<ResourceService> {

}
