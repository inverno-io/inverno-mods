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

import io.winterframework.mod.base.reflect.Types.ArrayTypeArgumentBuilder;
import io.winterframework.mod.base.reflect.Types.ArrayTypeBuilder;
import io.winterframework.mod.base.reflect.Types.TypeArgumentBuilder;

/**
 * @author jkuhn
 *
 */
class ArrayTypeBuilderImpl implements ArrayTypeBuilder {

	private Type componentType;
	
	public ArrayTypeBuilderImpl() {
	}

	@Override
	public TypeArgumentBuilder<ArrayTypeBuilder> componentType(Class<?> rawType) {
		return new TypeArgumentBuilderImpl<>(this, rawType, this::setComponentType);
	}

	@Override
	public ArrayTypeArgumentBuilder<ArrayTypeBuilder> componentArrayType() {
		return new ArrayTypeArgumentBuilderImpl<>(this, this::setComponentType);
	}

	private void setComponentType(Type type) {
		this.componentType = type;
	}
	
	@Override
	public Type build() {
		if(this.componentType == null) {
			throw new IllegalStateException("Missing array component type");
		}
		if(this.componentType instanceof Class) {
			return Array.newInstance((Class<?>)this.componentType, 0).getClass();
		}
		else {
			return new GenericArrayTypeImpl(this.componentType);
		}
	}

}
