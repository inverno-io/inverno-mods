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
import io.netty.util.concurrent.FastThreadLocal;

/**
 * <p>
 * A reactor scope is used to provide bean instances bound to the current reactor thread.
 * </p>
 * 
 * <p>
 * This implementation is backed by a {@link FastThreadLocal}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 * 
 * @param <T> the type of the scoped instances 
 */
public abstract class ReactorScope<T> extends Scope<T> {

	private final FastThreadLocal<T> instanceThreadLocal;
	
	/**
	 * <p>
	 * Creates a thread scope.
	 * </p>
	 */
	public ReactorScope() {
		this.instanceThreadLocal = new ReactorThreadLocal();
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
	 * Returns the instance bound to the current reactor thread.
	 * </p>
	 * 
	 * <p>
	 * If no instance is bound to the key, a new instance is created and kept in the scope.
	 * </p>
	 * 
	 * @return the instance
	 * 
	 * @throws IllegalStateException when the method is invoked outside the reactor
	 */
	public final T get() {
		Thread thread = Thread.currentThread();
		if(!(thread instanceof Reactor.Thread)) {
			throw new IllegalStateException("Current thread is not a reactor thread");
		}
		T instance = this.instanceThreadLocal.get();
		this.hookOnGet(instance);
		return instance;
	}
	
	/**
	 * <p>
	 * Removes the instance currently bound to the current thread.
	 * </p>
	 * 
	 * @throws IllegalStateException when the method is invoked outside the reactor
	 */
	public final void remove() {
		Thread thread = Thread.currentThread();
		if(!(thread instanceof Reactor.Thread)) {
			throw new IllegalStateException("Current thread is not a reactor thread");
		}
		this.instanceThreadLocal.remove();
	}
	
	/**
	 * <p>
	 * {@link FastThreadLocal} implementation that delegates the creation of its initial to the enclosing {@link ReactorScope}.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 */
	private class ReactorThreadLocal extends FastThreadLocal<T> {
		
		@Override
		protected T initialValue() throws Exception {
			return ReactorScope.this.create();
		}
	}
}
