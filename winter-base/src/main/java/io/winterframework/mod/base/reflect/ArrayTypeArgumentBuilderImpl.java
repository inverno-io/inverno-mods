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
package io.winterframework.mod.base.reflect;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.function.Consumer;

import io.winterframework.mod.base.reflect.Types.ArrayTypeArgumentBuilder;
import io.winterframework.mod.base.reflect.Types.TypeArgumentBuilder;


/**
 * @author jkuhn
 *
 */
class ArrayTypeArgumentBuilderImpl<A> implements ArrayTypeArgumentBuilder<A> {

	private A parentBuilder;

	private Consumer<Type> typeInjector;
	
	private Type componentType;
	
	/**
	 * 
	 */
	public ArrayTypeArgumentBuilderImpl(A parentBuilder, Consumer<Type> typeInjector) {
		this.parentBuilder = parentBuilder;
		this.typeInjector = typeInjector;
	}

	@Override
	public TypeArgumentBuilder<A> componentType(Class<?> rawType) {
		return new TypeArgumentBuilderImpl<>(this.parentBuilder, rawType, this::setComponentType);
	}

	@Override
	public ArrayTypeArgumentBuilder<A> componentArrayType() {
		return new ArrayTypeArgumentBuilderImpl<>(this.parentBuilder, this::setComponentType);
	}
	
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
