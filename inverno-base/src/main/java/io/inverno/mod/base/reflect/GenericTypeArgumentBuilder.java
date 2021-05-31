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

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * <p>
 * Generic {@link TypeArgumentBuilder} implementation
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see TypeArgumentBuilder
 * 
 * @param <A> the type of the parent builder
 */
class GenericTypeArgumentBuilder<A> implements TypeArgumentBuilder<A> {

	private A parentBuilder;
	
	private Consumer<Type> typeInjector;
	
	private Class<?> rawType;
	
	private List<Type> typeArguments;
	
	private Type ownerType;
	
	/**
	 * <p>
	 * Creates a generic type argument builder with the specified parent, raw type
	 * and resulting type injector.
	 * </p>
	 * 
	 * @param parentBuildert he parent builder
	 * @param rawType        the erased type
	 * @param typeInjector   the resulting type injector to invoke when the builder
	 *                       is finalized.
	 */
	public GenericTypeArgumentBuilder(A parentBuilder, Class<?> rawType, Consumer<Type> typeInjector) {
		this.parentBuilder = parentBuilder;
		this.rawType = rawType;
		this.typeInjector = typeInjector;
	}

	@Override
	public TypeArgumentBuilder<TypeArgumentBuilder<A>> type(Class<?> rawType) {
		if(rawType.isPrimitive()) {
			rawType = Types.boxType(rawType);
		}
		return new GenericTypeArgumentBuilder<>(this, rawType, this::addArgumentType);
	}

	@Override
	public WildcardTypeArgumentBuilder<TypeArgumentBuilder<A>> wildcardType() {
		return new GenericWildcardTypeArgumentBuilder<>(this, this::addArgumentType);
	}

	@Override
	public ArrayTypeArgumentBuilder<TypeArgumentBuilder<A>> arrayType() {
		return new GenericArrayTypeArgumentBuilder<>(this, this::addArgumentType);
	}
	
	@Override
	public TypeArgumentBuilder<TypeArgumentBuilder<A>> ownerType(Class<?> rawType) {
		return new GenericTypeArgumentBuilder<>(this, rawType, this::setOwnerType);
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
	public A and() {
		if(this.typeArguments == null) {
			// TODO Here we might need to include the owner type 
			this.typeInjector.accept(this.rawType);
		}
		else {
			this.typeInjector.accept(new ParameterizedTypeImpl(this.rawType, this.typeArguments.toArray(new Type[this.typeArguments.size()]), this.ownerType));
		}
		return this.parentBuilder;
	}
}
