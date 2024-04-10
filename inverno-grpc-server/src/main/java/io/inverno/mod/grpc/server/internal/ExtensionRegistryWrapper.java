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
package io.inverno.mod.grpc.server.internal;

import com.google.protobuf.ExtensionRegistry;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Overridable;
import io.inverno.core.annotation.Wrapper;
import java.util.function.Supplier;

/**
 * <p>
 * The Protocol buffer {@link ExtensionRegistry} wrapper bean.
 * </p>
 * 
 * <p>
 * An empty registry is provided by default.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
@Bean( name = "extensionRegistry", visibility = Bean.Visibility.PRIVATE )
@Wrapper
@Overridable
public class ExtensionRegistryWrapper implements Supplier<ExtensionRegistry> {

	@Override
	public ExtensionRegistry get() {
		return ExtensionRegistry.getEmptyRegistry();
	}
}