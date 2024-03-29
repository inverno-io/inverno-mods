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
package io.inverno.mod.ldap.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Overridable;
import io.inverno.core.annotation.Wrapper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * <p>
 * Worker pool used to execute Ldap blocking operations.
 * </p>
 * 
 * <p>
 * This Ldap client implementation relies on {@link java.naming} module which exposes blocking operations that must be executed in dedicated threads to protect I/O event loop.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Wrapper
@Overridable
@Bean( visibility = Bean.Visibility.PRIVATE )
public class WorkerPool implements Supplier<ExecutorService> {

	@Override
	public ExecutorService get() {
		return Executors.newCachedThreadPool();
	}
}
