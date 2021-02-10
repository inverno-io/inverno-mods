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

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import io.winterframework.mod.base.reflect.Types.ArrayTypeArgumentBuilder;
import io.winterframework.mod.base.reflect.Types.TypeArgumentBuilder;
import io.winterframework.mod.base.reflect.Types.TypeBuilder;
import io.winterframework.mod.base.reflect.Types.WildcardTypeArgumentBuilder;

/**
 * @author jkuhn
 *
 */
class TypeBuilderImpl implements TypeBuilder {

	private Class<?> rawType;
	
	private List<Type> typeArguments;
	
	private Type ownerType;
	
	/**
	 * 
	 */
	public TypeBuilderImpl(Class<?> rawType) {
		Objects.requireNonNull(rawType, "rawType");
		this.rawType = rawType;
	}
	
	@Override
	public TypeArgumentBuilder<TypeBuilder> type(Class<?> rawType) {
		return new TypeArgumentBuilderImpl<>(this, rawType, this::addArgumentType);
	}

	@Override
	public WildcardTypeArgumentBuilder<TypeBuilder> wildcardType() {
		return new WildcardTypeArgumentBuilderImpl<>(this, this::addArgumentType);
	}

	@Override
	public ArrayTypeArgumentBuilder<TypeBuilder> arrayType() {
		return new ArrayTypeArgumentBuilderImpl<>(this, this::addArgumentType);
	}
	
	@Override
	public TypeArgumentBuilder<TypeBuilder> ownerType(Class<?> rawType) {
		return new TypeArgumentBuilderImpl<>(this, rawType, this::setOwnerType);
	}

	private void addArgumentType(Type type) {
		if(this.typeArguments == null) {
			this.typeArguments = new LinkedList<>();
		}
		this.typeArguments.add(type);
	}
	
	private void setOwnerType(Type type) {
		this.ownerType = type;
	}
	
	@Override
	public Type build() {
		if(this.typeArguments == null) {
			// TODO Here we might need to include the owner type 
			return this.rawType;
		}
		else {
			return new ParameterizedTypeImpl(this.rawType, this.typeArguments.toArray(new Type[this.typeArguments.size()]), this.ownerType);
		}
	}

}
