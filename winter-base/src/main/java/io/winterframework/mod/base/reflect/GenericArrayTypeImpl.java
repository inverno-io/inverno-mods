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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * <p>{@link GenericArrayType} implementation</p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ArrayTypeBuilder
 * @see ArrayTypeArgumentBuilder
 */
class GenericArrayTypeImpl implements GenericArrayType {

	private final Type genericComponentType;

	/**
	 * <p>
	 * Creates a generic array type with the specified component type.
	 * </p>
	 * 
	 * @param genericComponentType the type of the array component
	 */
	public GenericArrayTypeImpl(Type genericComponentType) {
		this.genericComponentType = genericComponentType;
	}

	@Override
	public Type getGenericComponentType() {
		return this.genericComponentType;
	}

	@Override
	public String toString() {
        return getGenericComponentType().getTypeName() + "[]";
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof GenericArrayType) {
            GenericArrayType that = (GenericArrayType) o;

            return Objects.equals(this.genericComponentType, that.getGenericComponentType());
        } else
            return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.genericComponentType);
    }
}
