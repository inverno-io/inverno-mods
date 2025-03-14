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
import java.lang.reflect.WildcardType;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * <p>
 * Generic {@link WildcardTypeArgumentBuilder} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see WildcardType
 * @see ParameterizedType
 * 
 * @param <A> the type of the parent builder
 */
class GenericWildcardTypeArgumentBuilder<A> implements WildcardTypeArgumentBuilder<A> {
	
	private final A parentBuilder;
	
	private final Consumer<Type> typeInjector;
	
	private List<Type> upperBoundTypes;
	
	private List<Type> lowerBoundTypes;
	
	/**
	 * <p>
	 * Creates a generic wildcard type argument builder with the specified parent builder and resulting type injector.
	 * </p>
	 * 
	 * @param parentBuilder the parent builder
	 * @param typeInjector  the resulting type injector to invoke when the builder is finalized.
	 */
	public GenericWildcardTypeArgumentBuilder(A parentBuilder, Consumer<Type> typeInjector) {
		this.parentBuilder = parentBuilder;
		this.typeInjector = typeInjector;
	}

	@Override
	public TypeArgumentBuilder<WildcardTypeArgumentBuilder<A>> upperBoundType(Class<?> rawType) {
		if(rawType.isPrimitive()) {
			rawType = Types.boxType(rawType);
		}
		return new GenericTypeArgumentBuilder<>(this, rawType, this::addUpperBoundType);
	}

	@Override
	public WildcardTypeArgumentBuilder<A> upperBoundType(Type type) {
		this.addUpperBoundType(type);
		return this;
	}

	@Override
	public ArrayTypeArgumentBuilder<WildcardTypeArgumentBuilder<A>> upperBoundArrayType() {
		return new GenericArrayTypeArgumentBuilder<>(this, this::addUpperBoundType);
	}

	@Override
	public TypeArgumentBuilder<WildcardTypeArgumentBuilder<A>> lowerBoundType(Class<?> rawType) {
		if(rawType.isPrimitive()) {
			rawType = Types.boxType(rawType);
		}
		return new GenericTypeArgumentBuilder<>(this, rawType, this::addLowerBoundType);
	}
	
	@Override
	public WildcardTypeArgumentBuilder<A> lowerBoundType(Type type) {
		this.addLowerBoundType(type);
		return this;
	}

	@Override
	public ArrayTypeArgumentBuilder<WildcardTypeArgumentBuilder<A>> lowerBoundArrayType() {
		return new GenericArrayTypeArgumentBuilder<>(this, this::addLowerBoundType);
	}

	/**
	 * <p>
	 * Adds a upper bound type.
	 * </p>
	 * 
	 * <p>
	 * This method is invoked by child builders when they are finalized.
	 * </p>
	 * 
	 * @param type the upper bound type to add
	 */
	private void addUpperBoundType(Type type) {
		if(this.upperBoundTypes == null) {
			this.upperBoundTypes = new LinkedList<>();
		}
		this.upperBoundTypes.add(type);
	}
	
	/**
	 * <p>
	 * Adds a lower bound type.
	 * </p>
	 * 
	 * <p>
	 * This method is invoked by child builders when they are finalized.
	 * </p>
	 * 
	 * @param type the lower bound type to add
	 */
	private void addLowerBoundType(Type type) {
		if(this.lowerBoundTypes == null) {
			this.lowerBoundTypes = new LinkedList<>();
		}
		this.lowerBoundTypes.add(type);
	}
	
	@Override
	public A and() {
		this.typeInjector.accept(new WildcardTypeImpl(this.upperBoundTypes != null ? this.upperBoundTypes.toArray(Type[]::new) : new Type[0], this.lowerBoundTypes != null ? this.lowerBoundTypes.toArray(Type[]::new) : new Type[0]));
		return this.parentBuilder;
	}

}
