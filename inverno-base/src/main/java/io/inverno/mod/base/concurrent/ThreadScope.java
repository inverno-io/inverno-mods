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
package io.inverno.mod.base.concurrent;

import io.inverno.mod.base.Scope;

/**
 * <p>
 * A thread scope is used to provide bean instances bound to the current thread.
 * </p>
 * 
 * <p>
 * This implementation is backed by a {@link ThreadLocal}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 * 
 * @param <T> the type of the scoped instances
 */
public abstract class ThreadScope<T> extends Scope<T> {

	private final ThreadLocal<T> instanceThreadLocal;
	
	/**
	 * <p>
	 * Creates a thread scope.
	 * </p>
	 */
	public ThreadScope() {
		this.instanceThreadLocal = ThreadLocal.withInitial(this::create);
	}
	
	/**
	 * <p>
	 * Optional hook executed when an instance is retrieved.
	 * </p>
	 * 
	 * @param instance the instance
	 */
	protected void hookOnGet(T instance) {
		
	}
	
	/**
	 * <p>
	 * Returns the instance bound to the current thread.
	 * </p>
	 * 
	 * <p>
	 * If no instance is bound to the key, a new instance is created and kept in the scope.
	 * </p>
	 * 
	 * @return the instance
	 */
	public final T get() {
		T instance = this.instanceThreadLocal.get();
		this.hookOnGet(instance);
		return instance;
	}
	
	/**
	 * <p>
	 * Removes the instance currently bound to the current thread.
	 * </p>
	 */
	public final void remove() {
		this.instanceThreadLocal.remove();
	}
}
