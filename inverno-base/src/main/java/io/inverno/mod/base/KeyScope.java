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

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * A key scope is used to provide bean instances bound to an arbitrary key.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 * 
 * @param <T> the type of the scoped instances 
 */
public abstract class KeyScope<T> extends Scope<T> {

	private final Map<Object, T> instances;
	
	/**
	 * Creates a key scope.
	 */
	public KeyScope() {
		this.instances = new HashMap<>();
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
	 * Returns the instance bound to the specified key.
	 * </p>
	 * 
	 * <p>
	 * If no instance is bound to the key, a new instance is created and kept in the scope.
	 * </p>
	 * 
	 * @param key the key defining the scope of the instance
	 * 
	 * @return the instance
	 */
	public final T get(Object key) {
		T instance = this.instances.get(key);
		if(instance == null) {
			instance = this.create();
			this.instances.put(key, instance);
		}
		this.hookOnGet(instance);
		return instance;
	}
	
	/**
	 * <p>
	 * Removes the instance currently bound to the specified key.
	 * </p>
	 * 
	 * @param key the key defining the scope of the instance
	 */
	public final void remove(Object key) {
		this.instances.remove(key);
	}
}
