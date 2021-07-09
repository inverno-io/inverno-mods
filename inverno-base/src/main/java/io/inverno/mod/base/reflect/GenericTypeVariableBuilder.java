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
import java.lang.reflect.TypeVariable;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * <p>
 * Generic {@link TypeVariableBuilder} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 * 
 * @see TypeVariable
 */
public class GenericTypeVariableBuilder<A> implements TypeVariableBuilder<A> {

	private final A parentBuilder;

	private final TypeVariable<?> rawTypeVariable;
	
	private final String name;
	
	private final Consumer<Type> typeInjector;
	
	private List<Type> boundTypes;
	
	/**
	 * <p>
	 * Creates a generic type variable builder with the specified name and based on
	 * the specified underlying type variable.
	 * </p>
	 * 
	 * @param parentBuilder   the parent builder
	 * @param rawTypeVariable the underlying type variable
	 * @param name            the name of the type variable
	 * @param typeInjector    the resulting type injector to invoke when the builder
	 *                        is finalized.
	 */
	public GenericTypeVariableBuilder(A parentBuilder, TypeVariable<?> rawTypeVariable, String name, Consumer<Type> typeInjector) {
		this.parentBuilder = parentBuilder;
		this.rawTypeVariable = rawTypeVariable;
		this.name = name;
		this.typeInjector = typeInjector;
	}

	@Override
	public TypeArgumentBuilder<TypeVariableBuilder<A>> boundType(Class<?> rawType) {
		if(rawType.isPrimitive()) {
			rawType = Types.boxType(rawType);
		}
		return new GenericTypeArgumentBuilder<>(this, rawType, this::addBoundType);
	}

	/**
	 * <p>
	 * Adds a bound type.
	 * </p>
	 * 
	 * <p>
	 * This method is invoked by child builders when they are finalized.
	 * </p>
	 * 
	 * @param type the upper bound type to set
	 */
	private void addBoundType(Type type) {
		if(this.boundTypes == null) {
			this.boundTypes = new LinkedList<>();
		}
		this.boundTypes.add(type);
	}
	
	@Override
	public A and() {
		this.typeInjector.accept(new TypeVariableWrapper<>(this.rawTypeVariable, this.name, this.boundTypes != null ? this.boundTypes.toArray(new Type[this.boundTypes.size()]) : new Type[0]));
		return this.parentBuilder;
	}
}
