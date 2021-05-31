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
import java.lang.reflect.Type;

/**
 * <p>
 * Generic {@link ArrayTypeBuilder} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ArrayTypeBuilder
 */
class GenericArrayTypeBuilder implements ArrayTypeBuilder {

	private Type componentType;
	
	/**
	 * <p>Creates a generic array type builder.</p>
	 */
	public GenericArrayTypeBuilder() {
	}

	@Override
	public TypeArgumentBuilder<ArrayTypeBuilder> componentType(Class<?> rawType) {
		return new GenericTypeArgumentBuilder<>(this, rawType, this::setComponentType);
	}

	@Override
	public ArrayTypeArgumentBuilder<ArrayTypeBuilder> componentArrayType() {
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
