/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.security.jose.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.resource.ResourceService;
import java.util.function.Supplier;

/**
 * <p>
 * The {@link ResourceService} socket.
 * </p>
 * 
 * <p>
 * A resource service is required to resolve X.509 certificates or JWK Set URL when building or reading JSON Web Keys or JOSE objects.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Bean( name = "resourceService" )
public interface ResourceServiceSocket extends Supplier<ResourceService> {

}
