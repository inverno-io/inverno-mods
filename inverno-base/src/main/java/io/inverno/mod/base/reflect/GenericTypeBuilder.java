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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Generic {@link TypeBuilder} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see Type
 * @see ParameterizedType
 */
class GenericTypeBuilder implements TypeBuilder {

	private final Class<?> rawType;
	private final TypeVariable<?>[] rawTypeParameters;
	
	private List<Type> typeArguments;
	
	private Type ownerType;
	
	/**
	 * <p>
	 * Creates a generic type builder with the specified raw type.
	 * </p>
	 * 
	 * @param rawType an erased type
	 */
	public GenericTypeBuilder(Class<?> rawType) {
		Objects.requireNonNull(rawType, "rawType");
		this.rawType = rawType;
		this.rawTypeParameters = this.rawType.getTypeParameters();
	}
	
	@Override
	public TypeArgumentBuilder<TypeBuilder> type(Class<?> rawType) {
		return new GenericTypeArgumentBuilder<>(this, rawType, this::addArgumentType);
	}
	
	@Override
	public TypeBuilder type(Type type) {
		this.addArgumentType(type);
		return this;
	}

	@Override
	public TypeVariableBuilder<TypeBuilder> variableType(String name) {
		int argIndex = this.typeArguments != null ? this.typeArguments.size() : 0;
		if(argIndex < this.rawTypeParameters.length) {
			return new GenericTypeVariableBuilder<>(this, this.rawTypeParameters[argIndex], name, this::addArgumentType);
		}
		else {
			throw new IllegalStateException("Too many arguments");
		}
	}
	
	@Override
	public WildcardTypeArgumentBuilder<TypeBuilder> wildcardType() {
		return new GenericWildcardTypeArgumentBuilder<>(this, this::addArgumentType);
	}

	@Override
	public ArrayTypeArgumentBuilder<TypeBuilder> arrayType() {
		return new GenericArrayTypeArgumentBuilder<>(this, this::addArgumentType);
	}
	
	@Override
	public TypeArgumentBuilder<TypeBuilder> ownerType(Class<?> rawType) {
		return new GenericTypeArgumentBuilder<>(this, rawType, this::setOwnerType);
	}
	
	@Override
	public TypeBuilder ownerType(Type type) {
		this.setOwnerType(type);
		return this;
	}

	/**
	 * <p>
	 * Adds an argument type.
	 * </p>
	 * 
	 * <p>
	 * This method is invoked by child builders when they are finalized.
	 * </p>
	 * 
	 * @param type the argument type to set
	 */
	private void addArgumentType(Type type) {
		if(this.typeArguments == null) {
			this.typeArguments = new LinkedList<>();
		}
		this.typeArguments.add(type);
	}
	
	/**
	 * <p>
	 * Sets an owner type.
	 * </p>
	 * 
	 * <p>
	 * This method is invoked by child builders when they are finalized.
	 * </p>
	 * 
	 * @param type the owner type to set
	 */
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
