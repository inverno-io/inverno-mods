/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.base;


/**
 * <p>
 * A scope can be used to define a bean which allows to create and retrieve
 * different bean instances depending on a particular scope.
 * </p>
 * 
 * <p>
 * It is then possible to create instances using DI for a particular context.
 * This context can be anything from the current thread or specified by an
 * arbitrary key.
 * </p>
 * 
 * <p>
 * A typical <code>Scope</code> implementation typically relies on prototype
 * beans injected lazily to benefit from dependency injection but they can also
 * be designed as a regular factories.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 * @param <T> the type of the scoped instances 
 */
public abstract class Scope<T> {

	/**
	 * <p>
	 * Creates a scoped instance.
	 * </p>
	 * 
	 * @return an instance
	 */
	protected abstract T create();
}
