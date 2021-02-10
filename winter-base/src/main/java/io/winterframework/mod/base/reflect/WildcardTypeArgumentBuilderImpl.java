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
import java.util.function.Consumer;

import io.winterframework.mod.base.reflect.Types.ArrayTypeArgumentBuilder;
import io.winterframework.mod.base.reflect.Types.TypeArgumentBuilder;
import io.winterframework.mod.base.reflect.Types.WildcardTypeArgumentBuilder;

/**
 * @author jkuhn
 *
 */
class WildcardTypeArgumentBuilderImpl<A> implements WildcardTypeArgumentBuilder<A> {
	
	private A parentBuilder;
	
	private Consumer<Type> typeInjector;
	
	private List<Type> upperBoundTypes;
	
	private List<Type> lowerBoundTypes;
	
	public WildcardTypeArgumentBuilderImpl(A parentBuilder, Consumer<Type> typeInjector) {
		this.parentBuilder = parentBuilder;
		this.typeInjector = typeInjector;
	}

	@Override
	public TypeArgumentBuilder<WildcardTypeArgumentBuilder<A>> upperBoundType(Class<?> rawType) {
		if(rawType.isPrimitive()) {
			rawType = Types.boxType(rawType);
		}
		return new TypeArgumentBuilderImpl<>(this, rawType, this::returnUpperBoundType);
	}

	@Override
	public ArrayTypeArgumentBuilder<WildcardTypeArgumentBuilder<A>> upperBoundArrayType() {
		return new ArrayTypeArgumentBuilderImpl<>(this, this::returnUpperBoundType);
	}

	@Override
	public TypeArgumentBuilder<WildcardTypeArgumentBuilder<A>> lowerBoundType(Class<?> rawType) {
		if(rawType.isPrimitive()) {
			rawType = Types.boxType(rawType);
		}
		return new TypeArgumentBuilderImpl<>(this, rawType, this::returnLowerBoundType);
	}

	@Override
	public ArrayTypeArgumentBuilder<WildcardTypeArgumentBuilder<A>> lowerBoundArrayType() {
		return new ArrayTypeArgumentBuilderImpl<>(this, this::returnLowerBoundType);
	}

	private void returnUpperBoundType(Type type) {
		if(this.upperBoundTypes == null) {
			this.upperBoundTypes = new LinkedList<>();
		}
		this.upperBoundTypes.add(type);
	}
	
	private void returnLowerBoundType(Type type) {
		if(this.upperBoundTypes == null) {
			this.upperBoundTypes = new LinkedList<>();
		}
		this.upperBoundTypes.add(type);
	}
	
	@Override
	public A and() {
		this.typeInjector.accept(new WildcardTypeImpl(this.upperBoundTypes != null ? this.upperBoundTypes.toArray(new Type[this.upperBoundTypes.size()]) : new Type[0], this.lowerBoundTypes != null ? this.lowerBoundTypes.toArray(new Type[this.lowerBoundTypes.size()]) : new Type[0]));
		return this.parentBuilder;
	}

}
