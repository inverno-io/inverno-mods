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
package io.inverno.mod.base.reflect;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Consumer;


/**
 * <p>
 * Generic {@link ArrayTypeArgumentBuilder} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see GenericArrayType
 * @see ParameterizedType
 * 
 * @param <A> the type of the parent builder
 */
class GenericArrayTypeArgumentBuilder<A> implements ArrayTypeArgumentBuilder<A> {

	private A parentBuilder;

	private Consumer<Type> typeInjector;
	
	private Type componentType;
	
	/**
	 * <p>
	 * Creates a generic array type argument builder with the specified parent
	 * builder and resulting type injector.
	 * </p>
	 * 
	 * @param parentBuilder the parent builder
	 * @param typeInjector  the resulting type injector to invoke when the builder
	 *                      is finalized.
	 */
	public GenericArrayTypeArgumentBuilder(A parentBuilder, Consumer<Type> typeInjector) {
		this.parentBuilder = parentBuilder;
		this.typeInjector = typeInjector;
	}

	@Override
	public TypeArgumentBuilder<ArrayTypeArgumentBuilder<A>> componentType(Class<?> rawType) {
		return new GenericTypeArgumentBuilder<>(this, rawType, this::setComponentType);
	}
	
	@Override
	public ArrayTypeArgumentBuilder<A> componentType(Type type) {
		this.setComponentType(type);
		return this;
	}

	@Override
	public ArrayTypeArgumentBuilder<ArrayTypeArgumentBuilder<A>> componentArrayType() {
		return new GenericArrayTypeArgumentBuilder<>(this, this::setComponentType);
	}
	
	/**
	 * <p>
	 * Sets the array component type.
	 * </p>
	 * 
	 * <p>
	 * This method is invoked by child builders when they are finalized.
	 * </p>
	 * 
	 * @param type the type to set
	 */
	private void setComponentType(Type type) {
		this.componentType = type;
	}

	@Override
	public A and() {
		if(this.componentType == null) {
			throw new IllegalStateException("Missing array component type");
		}
		if(this.componentType instanceof Class) {
			this.typeInjector.accept(Array.newInstance((Class<?>)this.componentType, 0).getClass());
		}
		else {
			this.typeInjector.accept(new GenericArrayTypeImpl(this.componentType));
		}
		return this.parentBuilder;
	}
}
